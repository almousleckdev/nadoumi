package com.nadoumi.hr.service;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.nadoumi.common.outbox.OutboxEventTypes;
import com.nadoumi.common.outbox.OutboxWriter;
import com.nadoumi.hr.domain.Task;
import com.nadoumi.hr.domain.TaskEvent;
import com.nadoumi.hr.mapper.TaskEventMapper;
import com.ruoyi.common.utils.AuditActor;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
class TaskEventRecorder {

    private static final String AGGREGATE = "task";
    private static final String NEW_TASK_MARKER = "new";

    private final TaskEventMapper eventMapper;
    private final OutboxWriter outboxWriter;

    TaskEventRecorder(TaskEventMapper eventMapper, OutboxWriter outboxWriter) {
        this.eventMapper = eventMapper;
        this.outboxWriter = outboxWriter;
    }

    void record(long taskId, String type, String fromStatus, String toStatus, long actorUserId, String note) {
        TaskEvent ev = new TaskEvent();
        ev.setTaskId(taskId);
        ev.setEventType(type);
        ev.setFromStatus(fromStatus);
        ev.setToStatus(toStatus);
        ev.setActorUserId(actorUserId);
        ev.setNote(note);
        eventMapper.insert(ev);
    }

    void notifyParticipants(Task t, String fromStatus, long actorUserId) {
        Set<Long> recipients = new LinkedHashSet<>();
        recipients.add(t.getCreatedByUserId());
        if (t.getAssigneeUserId() != null) {
            recipients.add(t.getAssigneeUserId());
        }
        recipients.remove(actorUserId);

        JSONObject payload = new JSONObject();
        payload.put("taskId", t.getId());
        payload.put("taskTitle", t.getTitle());
        payload.put("taskStatus", t.getStatus());
        payload.put("fromStatus", fromStatus == null ? NEW_TASK_MARKER : fromStatus);
        payload.put("actor", AuditActor.username());
        payload.put("recipientUserIds", new JSONArray(recipients.toArray()));
        outboxWriter.write(AGGREGATE, t.getId(), OutboxEventTypes.TASK_PROGRESS_CHANGED, payload.toJSONString());
    }
}
