package com.nadoumi.hr.web.response;

import com.nadoumi.hr.domain.Task;
import com.nadoumi.hr.domain.TaskEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Staff view of a task; {@code events} is populated only on the detail endpoint. */
public record TaskResponse(
        long id,
        String title,
        String description,
        String priority,
        String status,
        Long assigneeUserId,
        String assigneeName,
        Long createdByUserId,
        String createdByName,
        LocalDate dueDate,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        String approvedByName,
        LocalDateTime approvedAt,
        String relatedType,
        Long relatedId,
        LocalDateTime createTime,
        List<Event> events) {

    public record Event(long id, String eventType, String fromStatus, String toStatus,
            String actorName, String note, LocalDateTime createdAt) {
        static Event from(TaskEvent e) {
            return new Event(e.getId(), e.getEventType(), e.getFromStatus(), e.getToStatus(),
                    e.getActorName(), e.getNote(), e.getCreatedAt());
        }
    }

    public static TaskResponse row(Task t) {
        return build(t, List.of());
    }

    public static TaskResponse detail(Task t, List<TaskEvent> events) {
        return build(t, events.stream().map(Event::from).toList());
    }

    private static TaskResponse build(Task t, List<Event> events) {
        return new TaskResponse(t.getId(), t.getTitle(), t.getDescription(), t.getPriority(), t.getStatus(),
                t.getAssigneeUserId(), t.getAssigneeName(), t.getCreatedByUserId(), t.getCreatedByName(),
                t.getDueDate(), t.getStartedAt(), t.getCompletedAt(), t.getApprovedByName(), t.getApprovedAt(),
                t.getRelatedType(), t.getRelatedId(), t.getCreateTime(), events);
    }
}
