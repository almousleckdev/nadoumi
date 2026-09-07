package com.nadoumi.hr.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Create/update payload for an employee. On create, an account fields block
 * ({@code userName}, {@code password}) provisions the {@code sys_user}; on update
 * the account is edited in place ({@code nickName} / {@code email} / {@code phone}).
 */
public record EmployeeRequest(
        // account — on create, pass either userId (link an existing staff account)
        // OR userName + password (provision a new one)
        Long userId,
        String userName,
        String password,
        @NotBlank String nickName,
        String email,
        String phone,
        String userStatus,
        java.util.List<Long> roleIds,
        // employment
        Long positionId,
        @Size(max = 120) String positionTitle,
        Long deptId,
        Long managerUserId,
        @NotBlank String employmentType,
        @NotBlank String employmentStatus,
        @NotNull LocalDate startDate,
        LocalDate probationEndDate,
        LocalDate endDate,
        @Size(max = 120) String workLocation,
        BigDecimal salaryAmount,
        String salaryCurrency,
        String payFrequency,
        @Size(max = 200) String emergencyContact,
        @Size(max = 60) String emergencyContactRelationship,
        @Size(max = 32) String emergencyContactPhone,
        @Size(max = 120) String emergencyContactEmail,
        @Size(max = 1000) String notes) {
}
