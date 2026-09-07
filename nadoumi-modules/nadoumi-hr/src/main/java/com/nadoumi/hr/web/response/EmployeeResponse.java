package com.nadoumi.hr.web.response;

import com.nadoumi.hr.domain.Employee;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Staff-facing view of an employee. {@code salaryAmount} / {@code salaryCurrency}
 * are {@code null} unless the caller holds {@code nad:employee:compensation:view}
 * ({@code compensationVisible} reports which case applies).
 */
public record EmployeeResponse(
        long id,
        long userId,
        String employeeNo,
        String userName,
        String nickName,
        String email,
        String phone,
        String userStatus,
        Long positionId,
        String positionTitle,
        Long deptId,
        String deptName,
        Long managerUserId,
        String managerName,
        String employmentType,
        String employmentStatus,
        LocalDate startDate,
        LocalDate probationEndDate,
        LocalDate endDate,
        String workLocation,
        BigDecimal salaryAmount,
        String salaryCurrency,
        String payFrequency,
        boolean compensationVisible,
        String emergencyContact,
        String emergencyContactRelationship,
        String emergencyContactPhone,
        String emergencyContactEmail,
        String notes,
        LocalDateTime createTime,
        List<Long> roleIds) {

    public static EmployeeResponse from(Employee e, boolean canViewComp) {
        return from(e, canViewComp, List.of());
    }

    public static EmployeeResponse from(Employee e, boolean canViewComp, List<Long> roleIds) {
        return new EmployeeResponse(
                e.getId(), e.getUserId(), e.getEmployeeNo(), e.getUserName(), e.getNickName(),
                e.getEmail(), e.getPhonenumber(), e.getUserStatus(),
                e.getPositionId(), e.getPositionTitle(), e.getDeptId(), e.getDeptName(),
                e.getManagerUserId(), e.getManagerName(),
                e.getEmploymentType(), e.getEmploymentStatus(),
                e.getStartDate(), e.getProbationEndDate(), e.getEndDate(), e.getWorkLocation(),
                canViewComp ? e.getSalaryAmount() : null,
                canViewComp ? e.getSalaryCurrency() : null,
                e.getPayFrequency(), canViewComp,
                e.getEmergencyContact(), e.getEmergencyContactRelationship(),
                e.getEmergencyContactPhone(), e.getEmergencyContactEmail(),
                e.getNotes(), e.getCreateTime(),
                roleIds == null ? List.of() : roleIds);
    }
}
