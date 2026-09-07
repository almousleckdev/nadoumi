package com.nadoumi.hr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nadoumi.common.outbox.OutboxEventTypes;
import com.nadoumi.common.outbox.OutboxWriter;
import com.nadoumi.hr.domain.Task;
import com.nadoumi.hr.domain.TaskEvent;
import com.nadoumi.hr.mapper.HrAudienceMapper;
import com.nadoumi.hr.mapper.TaskEventMapper;
import com.nadoumi.hr.mapper.TaskMapper;
import com.nadoumi.hr.web.request.TaskRequest;
import com.nadoumi.hr.web.response.TaskResponse;
import com.nadoumi.identity.exception.NadBadRequestException;
import com.nadoumi.identity.exception.NadForbiddenException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class TaskServiceTest {

    private final TaskMapper taskMapper = mock(TaskMapper.class);
    private final TaskEventMapper eventMapper = mock(TaskEventMapper.class);
    private final HrAudienceMapper audienceMapper = mock(HrAudienceMapper.class);
    private final OutboxWriter outbox = mock(OutboxWriter.class);
    private final TaskService service = new TaskService(taskMapper, eventMapper, audienceMapper, outbox);

    TaskServiceTest() {
        doAnswer(inv -> { inv.getArgument(0, Task.class).setId(50L); return null; })
                .when(taskMapper).insert(any(Task.class));
        when(audienceMapper.findStaffUserIdsWithPermission(anyString())).thenReturn(List.of(9L));
        when(eventMapper.findByTask(anyLong())).thenReturn(List.of());
    }

    private Task stored(long id, String status) {
        Task t = new Task();
        t.setId(id);
        t.setTitle("Prepare JW202 batch");
        t.setPriority("HIGH");
        t.setStatus(status);
        t.setAssigneeUserId(7L);
        t.setCreatedByUserId(3L);
        return t;
    }

    @Test
    void create_insertsPendingTask_logsCreatedEvent_andEmits() {
        when(taskMapper.findById(50L)).thenReturn(stored(50L, "PENDING"));

        TaskResponse created = service.create(
                new TaskRequest("Prepare JW202 batch", null, "high", 7L, null, null, null), 3L);

        assertThat(created.id()).isEqualTo(50L);
        ArgumentCaptor<Task> task = ArgumentCaptor.forClass(Task.class);
        verify(taskMapper).insert(task.capture());
        assertThat(task.getValue().getStatus()).isEqualTo("PENDING");
        assertThat(task.getValue().getPriority()).isEqualTo("HIGH");

        ArgumentCaptor<TaskEvent> ev = ArgumentCaptor.forClass(TaskEvent.class);
        verify(eventMapper, org.mockito.Mockito.atLeastOnce()).insert(ev.capture());
        assertThat(ev.getAllValues()).anyMatch(e -> "CREATED".equals(e.getEventType()));

        verify(outbox).write(eq("task"), eq(50L), eq(OutboxEventTypes.TASK_PROGRESS_CHANGED), anyString());
    }

    @Test
    void changeStatus_pendingToInProgress_stampsStartedAt_logsAndEmits() {
        when(taskMapper.findById(50L)).thenReturn(stored(50L, "PENDING"));

        service.changeStatus(50L, "in_progress", "starting", 7L, false);

        ArgumentCaptor<Task> task = ArgumentCaptor.forClass(Task.class);
        verify(taskMapper).update(task.capture());
        assertThat(task.getValue().getStatus()).isEqualTo("IN_PROGRESS");
        assertThat(task.getValue().getStartedAt()).isNotNull();
        verify(eventMapper).insert(org.mockito.ArgumentMatchers.argThat(
                e -> "STATUS_CHANGED".equals(e.getEventType()) && "IN_PROGRESS".equals(e.getToStatus())));
        verify(outbox).write(eq("task"), eq(50L), eq(OutboxEventTypes.TASK_PROGRESS_CHANGED), anyString());
    }

    @Test
    void changeStatus_rejectsANonOwnerNonAdmin() {
        when(taskMapper.findById(50L)).thenReturn(stored(50L, "PENDING"));

        // actor 99 is neither the assignee (7) nor the creator (3), and not an approver
        assertThatThrownBy(() -> service.changeStatus(50L, "IN_PROGRESS", null, 99L, false))
                .isInstanceOf(NadForbiddenException.class);
        verify(taskMapper, never()).update(any());
    }

    @Test
    void changeStatus_allowsANonOwnerApprover() {
        when(taskMapper.findById(50L)).thenReturn(stored(50L, "PENDING"));

        service.changeStatus(50L, "IN_PROGRESS", null, 99L, true);

        verify(taskMapper).update(any());
    }

    @Test
    void changeStatus_rejectsAnIllegalTransition() {
        when(taskMapper.findById(50L)).thenReturn(stored(50L, "PENDING"));

        assertThatThrownBy(() -> service.changeStatus(50L, "APPROVED", null, 7L, true))
                .isInstanceOf(NadBadRequestException.class);
        verify(taskMapper, never()).update(any());
    }

    @Test
    void changeStatus_approveRequiresApproverRole() {
        when(taskMapper.findById(50L)).thenReturn(stored(50L, "COMPLETED"));

        assertThatThrownBy(() -> service.changeStatus(50L, "APPROVED", null, 7L, false))
                .isInstanceOf(NadBadRequestException.class)
                .hasMessageContaining("approve");

        service.changeStatus(50L, "APPROVED", "looks good", 3L, true);
        ArgumentCaptor<Task> task = ArgumentCaptor.forClass(Task.class);
        verify(taskMapper).update(task.capture());
        assertThat(task.getValue().getStatus()).isEqualTo("APPROVED");
        assertThat(task.getValue().getApprovedByUserId()).isEqualTo(3L);
        assertThat(task.getValue().getApprovedAt()).isNotNull();
    }

    @Test
    void emit_excludesTheActorFromRecipients() {
        when(taskMapper.findById(50L)).thenReturn(stored(50L, "PENDING"));
        // actor 7 is the assignee; creator is 3, approver audience is 9
        service.changeStatus(50L, "IN_PROGRESS", null, 7L, false);

        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(outbox).write(anyString(), anyLong(), anyString(), payload.capture());
        assertThat(payload.getValue()).contains("\"recipientUserIds\"");
        assertThat(payload.getValue()).doesNotContain(":7,").doesNotContain("[7]").doesNotContain("[7,");
        assertThat(payload.getValue()).contains("3").contains("9");
    }
}
