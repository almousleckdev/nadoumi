package com.nadoumi.university.mapper;

import com.nadoumi.university.domain.Department;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** MyBatis mapper for {@code nad_department}. */
public interface DepartmentMapper {

    List<Department> findByUniversity(@Param("universityId") long universityId);

    Department findById(@Param("id") long id);

    Long findIdByUniversityAndName(@Param("universityId") long universityId, @Param("name") String name);

    /** Majors that currently point at this department — blocks deletion when > 0. */
    int countMajorsUsing(@Param("id") long id);

    void insert(Department department);

    int update(Department department);

    int deleteById(@Param("id") long id);
}
