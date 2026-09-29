package com.nadoumi.hr.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.nadoumi.common.text.Texts;
import com.nadoumi.common.web.PageResponse;
import com.nadoumi.common.web.PageSupport;
import com.nadoumi.hr.domain.Task;
import com.nadoumi.hr.domain.TaskPriority;
import com.nadoumi.hr.domain.TaskStatus;
import com.nadoumi.hr.mapper.TaskEventMapper;
import com.nadoumi.hr.mapper.TaskMapper;
import com.nadoumi.hr.web.request.TaskRequest;
import com.nadoumi.hr.web.response.TaskResponse;
import com.nadoumi.common.exception.NadBadRequestException;
import com.nadoumi.common.exception.NadForbiddenException;
import com.nadoumi.common.exception.NadNotFoundException;
import com.ruoyi.common.utils.AuditActor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private static final String EV_CREATED = "CREATED";
    private static final String EV_STATUS = "STATUS_CHANGED";
    private static final String EV_ASSIGNED = "ASSIGNED";
    private static final String EV_PRIORITY = "PRIORITY_CHANGED";

    private final TaskMapper taskMapper;
    private final TaskEventMapper eventMapper;
    private final TaskEventRecorder recorder;

    public TaskService(TaskMapper taskMapper, TaskEventMapper eventMapper, TaskEventRecorder recorder) {
        this.taskMapper = taskMapper;
        this.eventMapper = eventMapper;
        this.recorder = recorder;
    }

    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> list(String q, String status, String priority, Long assigneeUserId,
            Long createdByUserId, Long ownedByUserId, int page, int size) {
        page = PageSupport.clampPage(page);
        size = PageSupport.clampSize(size);
        PageHelper.startPage(page + 1, size);
        List<Task> rows = taskMapper.search(Texts.blankToNull(q), Texts.blankToNull(status), Texts.blankToNull(priority), assigneeUserId, createdByUserId,
                ownedByUserId);
        long total = new PageInfo<>(rows).getTotal();
        return PageResponse.of(rows.stream().map(TaskResponse::row).toList(), page, size, total);
    }

    @Transactional(readOnly = true)
    public TaskResponse get(long id) {
        return TaskResponse.detail(require(id), eventMapper.findByTask(id));
    }

    /**
     * Same lookup, scoped like {@link #list}: a non-approver may only view a
     * task they created or are assigned to.
     */
    @Transactional(readOnly = true)
    public TaskResponse get(long id, long actorUserId, boolean isApprover) {
        Task t = require(id);
        assertCanView(t, actorUserId, isApprover);
        return TaskResponse.detail(t, eventMapper.findByTask(id));
    }

    private void assertCanView(Task t, long actorUserId, boolean isApprover) {
        if (!isApprover && !isParticipant(t, actorUserId)) {
            throw new NadForbiddenException("you can only view your own tasks");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public TaskResponse create(TaskRequest req, long actorUserId) {
        Task t = new Task();
        t.setTitle(req.title().trim());
        t.setDescription(Texts.blankToNull(req.description()));
        t.setPriority(parsePriority(req.priority()));
        t.setStatus(TaskStatus.PENDING.name());
        t.setAssigneeUserId(req.assigneeUserId());
        t.setCreatedByUserId(actorUserId);
        t.setDueDate(req.dueDate());
        t.setRelatedType(Texts.blankToNull(req.relatedType()));
        t.setRelatedId(req.relatedId());
        t.setCreateBy(AuditActor.username());
        taskMapper.insert(t);

        recorder.record(t.getId(), EV_CREATED, null, TaskStatus.PENDING.name(), actorUserId, null);
        if (req.assigneeUserId() != null) {
            recorder.record(t.getId(), EV_ASSIGNED, null, null, actorUserId, "assigned on creation");
        }
        recorder.notifyParticipants(t, null, actorUserId);
        return get(t.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public TaskResponse update(long id, TaskRequest req, long actorUserId) {
        Task t = require(id);
        if (TaskStatus.valueOf(t.getStatus()).isTerminal()) {
            throw new NadBadRequestException("a " + t.getStatus() + " task can no longer be edited");
        }
        String newPriority = parsePriority(req.priority());
        boolean priorityChanged = !newPriority.equals(t.getPriority());
        boolean assigneeChanged = !Objects.equals(req.assigneeUserId(), t.getAssigneeUserId());

        t.setTitle(req.title().trim());
        t.setDescription(Texts.blankToNull(req.description()));
        t.setPriority(newPriority);
        t.setAssigneeUserId(req.assigneeUserId());
        t.setDueDate(req.dueDate());
        t.setRelatedType(Texts.blankToNull(req.relatedType()));
        t.setRelatedId(req.relatedId());
        t.setUpdateBy(AuditActor.username());
        taskMapper.update(t);

        if (priorityChanged) {
            recorder.record(id, EV_PRIORITY, null, null, actorUserId, "priority -> " + newPriority);
        }
        if (assigneeChanged) {
            recorder.record(id, EV_ASSIGNED, null, null, actorUserId, "reassigned");
        }
        if (priorityChanged || assigneeChanged) {
            recorder.notifyParticipants(t, t.getStatus(), actorUserId);
        }
        return get(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public TaskResponse changeStatus(long id, String targetRaw, String note, long actorUserId, boolean isAdmin) {
        Task t = require(id);
        // A rank-and-file employee may only move a task that is assigned to them
        // (or that they created). Approvers / admins may move any task.
        if (!isAdmin && !isParticipant(t, actorUserId)) {
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
        if (target == TaskStatus.CLOSED && !isAdmin) {
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
        if (target == TaskStatus.CLOSED) {
            t.setApprovedByUserId(actorUserId);
            t.setApprovedAt(now);
        }
        t.setUpdateBy(AuditActor.username());
        taskMapper.update(t);

        recorder.record(id, EV_STATUS, from.name(), target.name(), actorUserId, Texts.blankToNull(note));
        recorder.notifyParticipants(t, from.name(), actorUserId);
        return get(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(long id, long actorUserId) {
        Task t = require(id);
        if (!Objects.equals(actorUserId, t.getCreatedByUserId())) {
            throw new NadForbiddenException("Only the creator can delete a task");
        }
        if (taskMapper.deleteById(id) == 0) {
            throw new NadNotFoundException("task not found");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public TaskResponse addNote(long id, String note, long actorUserId, boolean isApprover) {
        Task t = require(id);
        assertCanView(t, actorUserId, isApprover);
        recorder.record(id, "NOTE", null, null, actorUserId, Texts.blankToNull(note));
        recorder.notifyParticipants(t, t.getStatus(), actorUserId);
        return get(id);
    }

    // ---- internals ----

    private static boolean isParticipant(Task t, long actorUserId) {
        return Objects.equals(actorUserId, t.getAssigneeUserId())
                || Objects.equals(actorUserId, t.getCreatedByUserId());
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
            return TaskPriority.valueOf(raw.trim().toUpperCase(Locale.ROOT)).name();
        } catch (RuntimeException e) {
            throw new NadBadRequestException("unknown priority: " + raw);
        }
    }

    private static TaskStatus parseStatus(String raw) {
        try {
            return TaskStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException e) {
            throw new NadBadRequestException("unknown task status: " + raw);
        }
    }
}
