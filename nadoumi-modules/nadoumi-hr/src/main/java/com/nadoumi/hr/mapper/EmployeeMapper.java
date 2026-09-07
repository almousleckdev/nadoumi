package com.nadoumi.hr.mapper;

import com.nadoumi.hr.domain.Employee;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** MyBatis mapper for {@code nad_employee}. Scanned by {@code com.nadoumi.**.mapper}. */
public interface EmployeeMapper {

    void insert(Employee employee);

    int update(Employee employee);

    Employee findById(@Param("id") long id);

    Employee findByUserId(@Param("userId") long userId);

    int deleteById(@Param("id") long id);

    /** Paged staff/HR list; {@code q} matches name / username / email / employee_no. */
    List<Employee> search(@Param("q") String q, @Param("deptId") Long deptId,
            @Param("employmentStatus") String employmentStatus);

    /** Highest numeric suffix already used for {@code employee_no} beginning with {@code prefix}. */
    Integer maxEmployeeSeq(@Param("prefix") String prefix);

    /** Employees with a recorded salary who are not terminated (payroll view). */
    List<Employee> payrollRows();
}
