package com.nadoumi.hr.mapper;

import com.nadoumi.hr.domain.Task;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** MyBatis mapper for {@code nad_task}. */
public interface TaskMapper {

    void insert(Task task);

    int update(Task task);

    Task findById(@Param("id") long id);

    int deleteById(@Param("id") long id);

    List<Task> search(@Param("q") String q, @Param("status") String status,
            @Param("priority") String priority, @Param("assigneeUserId") Long assigneeUserId,
            @Param("createdByUserId") Long createdByUserId,
            @Param("ownedByUserId") Long ownedByUserId);
}
