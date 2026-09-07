package com.nadoumi.hr.service;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.nadoumi.common.outbox.OutboxEventTypes;
import com.nadoumi.common.outbox.OutboxWriter;
import com.nadoumi.common.web.PageResponse;
import com.nadoumi.hr.domain.Task;
import com.nadoumi.hr.domain.TaskEvent;
import com.nadoumi.hr.domain.TaskStatus;
import com.nadoumi.hr.mapper.HrAudienceMapper;
import com.nadoumi.hr.mapper.TaskEventMapper;
import com.nadoumi.hr.mapper.TaskMapper;
import com.nadoumi.hr.web.request.TaskRequest;
import com.nadoumi.hr.web.response.TaskResponse;
import com.nadoumi.identity.exception.NadBadRequestException;
import com.nadoumi.identity.exception.NadForbiddenException;
import com.nadoumi.identity.exception.NadNotFoundException;
import com.ruoyi.common.utils.SecurityUtils;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Task assignment + progress tracking. Every create, reassignment, priority change
 * and status transition writes an append-only {@code nad_task_event} and emits a
 * {@code TaskProgressChanged} outbox event whose payload names the recipients
 * (creator + assignee + task admins) so the notification pipeline tells them.
 *
 * <p>Status transitions are validated by {@link TaskStatus#canMoveTo}; moving to
 * {@code APPROVED} additionally requires {@code isAdmin}.</p>
 */
@Service
public class TaskService {

    private static final String AGGREGATE = "task";
    private static final String APPROVE_PERMISSION = "nad:task:approve";
    private static final String EV_CREATED = "CREATED";
    private static final String EV_STATUS = "STATUS_CHANGED";
    private static final String EV_ASSIGNED = "ASSIGNED";
    private static final String EV_PRIORITY = "PRIORITY_CHANGED";

    private final TaskMapper taskMapper;
    private final TaskEventMapper eventMapper;
    private final HrAudienceMapper audienceMapper;
    private final OutboxWriter outboxWriter;

    public TaskService(TaskMapper taskMapper, TaskEventMapper eventMapper, HrAudienceMapper audienceMapper,
            OutboxWriter outboxWriter) {
        this.taskMapper = taskMapper;
        this.eventMapper = eventMapper;
        this.audienceMapper = audienceMapper;
        this.outboxWriter = outboxWriter;
    }

    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> list(String q, String status, String priority, Long assigneeUserId,
            Long createdByUserId, Long ownedByUserId, int page, int size) {
        PageHelper.startPage(page + 1, size);
        List<Task> rows = taskMapper.search(nz(q), nz(status), nz(priority), assigneeUserId, createdByUserId,
                ownedByUserId);
        long total = new PageInfo<>(rows).getTotal();
        return PageResponse.of(rows.stream().map(TaskResponse::row).toList(), page, size, total);
    }

    @Transactional(readOnly = true)
    public TaskResponse get(long id) {
        Task t = require(id);
        return TaskResponse.detail(t, eventMapper.findByTask(id));
    }

    @Transactional
    public TaskResponse create(TaskRequest req, long actorUserId) {
        Task t = new Task();
        t.setTitle(req.title().trim());
        t.setDescription(nz(req.description()));
        t.setPriority(parsePriority(req.priority()));
        t.setStatus(TaskStatus.PENDING.name());
        t.setAssigneeUserId(req.assigneeUserId());
        t.setCreatedByUserId(actorUserId);
        t.setDueDate(req.dueDate());
        t.setRelatedType(nz(req.relatedType()));
        t.setRelatedId(req.relatedId());
        t.setCreateBy(currentUser());
        taskMapper.insert(t);

        writeEvent(t.getId(), EV_CREATED, null, TaskStatus.PENDING.name(), actorUserId, null);
        if (req.assigneeUserId() != null) {
            writeEvent(t.getId(), EV_ASSIGNED, null, null, actorUserId, "assigned on creation");
        }
        emit(t, null, actorUserId);
        return get(t.getId());
    }

    @Transactional
    public TaskResponse update(long id, TaskRequest req, long actorUserId) {
        Task t = require(id);
        if (TaskStatus.valueOf(t.getStatus()).isTerminal()) {
            throw new NadBadRequestException("a " + t.getStatus() + " task can no longer be edited");
        }
        String newPriority = parsePriority(req.priority());
        boolean priorityChanged = !newPriority.equals(t.getPriority());
        boolean assigneeChanged = !java.util.Objects.equals(req.assigneeUserId(), t.getAssigneeUserId());

        t.setTitle(req.title().trim());
        t.setDescription(nz(req.description()));
        t.setPriority(newPriority);
        t.setAssigneeUserId(req.assigneeUserId());
        t.setDueDate(req.dueDate());
        t.setRelatedType(nz(req.relatedType()));
        t.setRelatedId(req.relatedId());
        t.setUpdateBy(currentUser());
        taskMapper.update(t);

        if (priorityChanged) {
            writeEvent(id, EV_PRIORITY, null, null, actorUserId, "priority -> " + newPriority);
        }
        if (assigneeChanged) {
            writeEvent(id, EV_ASSIGNED, null, null, actorUserId, "reassigned");
        }
        if (priorityChanged || assigneeChanged) {
            emit(t, t.getStatus(), actorUserId);
        }
        return get(id);
    }

    @Transactional
    public TaskResponse changeStatus(long id, String targetRaw, String note, long actorUserId, boolean isAdmin) {
        Task t = require(id);
        // A rank-and-file employee may only move a task that is assigned to them
        // (or that they created). Approvers / admins may move any task.
        if (!isAdmin
                && !java.util.Objects.equals(actorUserId, t.getAssigneeUserId())
                && !java.util.Objects.equals(actorUserId, t.getCreatedByUserId())) {
            throw new NadForbiddenException("you can only change the status of your own tasks");
        }
        TaskStatus from = TaskStatus.valueOf(t.getStatus());
        TaskStatus target = parseStatus(targetRaw);
        if (from == target) {
            return get(id);
        }
        if (!from.canMoveTo(target)) {
            throw new NadBadRequestException("cannot move a task from " + from + " to " + target);
        }
        if (target == TaskStatus.APPROVED && !isAdmin) {
            throw new NadBadRequestException("only an approver (nad:task:approve) can approve a completed task");
        }

        LocalDateTime now = LocalDateTime.now();
        t.setStatus(target.name());
        if (target == TaskStatus.IN_PROGRESS && t.getStartedAt() == null) {
            t.setStartedAt(now);
        }
        if (target == TaskStatus.COMPLETED) {
            t.setCompletedAt(now);
        }
        if (target == TaskStatus.APPROVED) {
            t.setApprovedByUserId(actorUserId);
            t.setApprovedAt(now);
        }
        t.setUpdateBy(currentUser());
        taskMapper.update(t);

        writeEvent(id, EV_STATUS, from.name(), target.name(), actorUserId, nz(note));
        emit(t, from.name(), actorUserId);
        return get(id);
    }

    @Transactional
    public void delete(long id) {
        if (taskMapper.deleteById(id) == 0) {
            throw new NadNotFoundException("task not found");
        }
    }

    // ---- internals ----

    private void writeEvent(long taskId, String type, String fromStatus, String toStatus, long actorUserId,
            String note) {
        TaskEvent ev = new TaskEvent();
        ev.setTaskId(taskId);
        ev.setEventType(type);
        ev.setFromStatus(fromStatus);
        ev.setToStatus(toStatus);
        ev.setActorUserId(actorUserId);
        ev.setNote(note);
        eventMapper.insert(ev);
    }

    private void emit(Task t, String fromStatus, long actorUserId) {
        Set<Long> recipients = new LinkedHashSet<>();
        recipients.add(t.getCreatedByUserId());
        if (t.getAssigneeUserId() != null) {
            recipients.add(t.getAssigneeUserId());
        }
        recipients.addAll(audienceMapper.findStaffUserIdsWithPermission(APPROVE_PERMISSION));
        recipients.remove(actorUserId); // don't notify the person who made the change

        JSONObject payload = new JSONObject();
        payload.put("taskId", t.getId());
        payload.put("taskTitle", t.getTitle());
        payload.put("taskStatus", t.getStatus());
        payload.put("fromStatus", fromStatus == null ? "new" : fromStatus);
        payload.put("actor", currentUser());
        payload.put("recipientUserIds", new JSONArray(recipients.toArray()));
        outboxWriter.write(AGGREGATE, t.getId(), OutboxEventTypes.TASK_PROGRESS_CHANGED, payload.toJSONString());
    }

    private Task require(long id) {
        Task t = taskMapper.findById(id);
        if (t == null) {
            throw new NadNotFoundException("task not found");
        }
        return t;
    }

    private static String parsePriority(String raw) {
        try {
            return com.nadoumi.hr.domain.TaskPriority.valueOf(raw.trim().toUpperCase(java.util.Locale.ROOT)).name();
        } catch (RuntimeException e) {
            throw new NadBadRequestException("unknown priority: " + raw);
        }
    }

    private static TaskStatus parseStatus(String raw) {
        try {
            return TaskStatus.valueOf(raw.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (RuntimeException e) {
            throw new NadBadRequestException("unknown task status: " + raw);
        }
    }

    private static String currentUser() {
        try {
            return SecurityUtils.getUsername();
        } catch (RuntimeException e) {
            return "system";
        }
    }

    private static String nz(String s) {
        return StringUtils.hasText(s) ? s.trim() : null;
    }
}
