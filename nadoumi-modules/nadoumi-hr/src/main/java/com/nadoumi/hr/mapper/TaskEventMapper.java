package com.nadoumi.hr.mapper;

import com.nadoumi.hr.domain.TaskEvent;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** MyBatis mapper for {@code nad_task_event} (append-only). */
public interface TaskEventMapper {

    void insert(TaskEvent event);

    List<TaskEvent> findByTask(@Param("taskId") long taskId);
}
