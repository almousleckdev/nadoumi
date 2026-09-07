package com.nadoumi.hr.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.nadoumi.common.web.PageResponse;
import com.nadoumi.hr.domain.Employee;
import com.nadoumi.hr.mapper.EmployeeMapper;
import com.nadoumi.hr.web.request.EmployeeRequest;
import com.nadoumi.hr.web.response.EmployeeResponse;
import com.nadoumi.identity.exception.NadBadRequestException;
import com.nadoumi.identity.exception.NadNotFoundException;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.service.ISysUserService;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Employee (HR) management. One employee = one {@code sys_user} staff account
 * ({@code user_type='00'}) + one {@code nad_employee} record, created and edited
 * together. Salary is only returned to callers who hold
 * {@code nad:employee:compensation:view} (enforced at the controller and applied
 * here).
 */
@Service
public class EmployeeService {

    private static final String EMPLOYEE_NO_PREFIX = "NAD-EMP-";
    private static final String STAFF_USER_TYPE = "00";

    private final EmployeeMapper mapper;
    private final ISysUserService userService;

    public EmployeeService(EmployeeMapper mapper, ISysUserService userService) {
        this.mapper = mapper;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public PageResponse<EmployeeResponse> list(String q, Long deptId, String status, int page, int size,
            boolean canViewComp) {
        PageHelper.startPage(page + 1, size);
        List<Employee> rows = mapper.search(blankToNull(q), deptId, blankToNull(status));
        long total = new PageInfo<>(rows).getTotal();
        return PageResponse.of(rows.stream().map(e -> EmployeeResponse.from(e, canViewComp)).toList(),
                page, size, total);
    }

    @Transactional(readOnly = true)
    public EmployeeResponse get(long id, boolean canViewComp) {
        Employee e = mapper.findById(id);
        if (e == null) {
            throw new NadNotFoundException("employee not found");
        }
        return EmployeeResponse.from(e, canViewComp, roleIdsOf(e.getUserId()));
    }

    /** Role ids currently granted to the employee's staff account (for the edit form). */
    private List<Long> roleIdsOf(Long userId) {
        if (userId == null) {
            return List.of();
        }
        SysUser u = userService.selectUserById(userId);
        if (u == null || u.getRoles() == null) {
            return List.of();
        }
        return u.getRoles().stream().map(r -> r.getRoleId()).toList();
    }

    @Transactional
    public EmployeeResponse create(EmployeeRequest req, boolean canViewComp) {
        long userId;
        if (req.userId() != null) {
            // link an existing staff account
            if (mapper.findByUserId(req.userId()) != null) {
                throw new NadBadRequestException("that user already has an employee record");
            }
            SysUser existing = userService.selectUserById(req.userId());
            if (existing == null) {
                throw new NadBadRequestException("user " + req.userId() + " does not exist");
            }
            SysUser u = new SysUser();
            u.setUserId(req.userId());
            u.setNickName(req.nickName().trim());
            u.setEmail(trimToNull(req.email()));
            u.setPhonenumber(trimToNull(req.phone()));
            u.setDeptId(req.deptId());
            u.setStatus(StringUtils.hasText(req.userStatus()) ? req.userStatus() : existing.getStatus());
            if (req.roleIds() != null) {
                u.setRoleIds(req.roleIds().toArray(new Long[0]));
            }
            userService.updateUser(u);
            userId = req.userId();
        } else {
            if (!StringUtils.hasText(req.userName()) || !StringUtils.hasText(req.password())) {
                throw new NadBadRequestException("provide an existing userId, or a username + initial password");
            }
            SysUser u = new SysUser();
            u.setUserName(req.userName().trim());
            u.setNickName(req.nickName().trim());
            u.setEmail(trimToNull(req.email()));
            u.setPhonenumber(trimToNull(req.phone()));
            u.setUserType(STAFF_USER_TYPE);
            u.setStatus(StringUtils.hasText(req.userStatus()) ? req.userStatus() : "0");
            u.setDeptId(req.deptId());
            u.setPassword(SecurityUtils.encryptPassword(req.password()));
            if (req.roleIds() != null && !req.roleIds().isEmpty()) {
                u.setRoleIds(req.roleIds().toArray(new Long[0]));
            }
            userService.insertUser(u);
            userId = u.getUserId();
        }

        Employee e = new Employee();
        e.setUserId(userId);
        e.setEmployeeNo(nextEmployeeNo());
        applyEmployment(e, req);
        e.setCreateBy(currentUser());
        mapper.insert(e);
        return get(e.getId(), canViewComp);
    }

    @Transactional
    public EmployeeResponse update(long id, EmployeeRequest req, boolean canViewComp) {
        Employee existing = mapper.findById(id);
        if (existing == null) {
            throw new NadNotFoundException("employee not found");
        }
        SysUser u = new SysUser();
        u.setUserId(existing.getUserId());
        u.setNickName(req.nickName().trim());
        u.setEmail(trimToNull(req.email()));
        u.setPhonenumber(trimToNull(req.phone()));
        u.setDeptId(req.deptId());
        u.setStatus(StringUtils.hasText(req.userStatus()) ? req.userStatus() : existing.getUserStatus());
        if (req.roleIds() != null) {
            u.setRoleIds(req.roleIds().toArray(new Long[0]));
        }
        userService.updateUser(u);

        Employee e = new Employee();
        e.setId(id);
        applyEmployment(e, req);
        e.setUpdateBy(currentUser());
        mapper.update(e);
        return get(id, canViewComp);
    }

    @Transactional
    public void delete(long id) {
        if (mapper.deleteById(id) == 0) {
            throw new NadNotFoundException("employee not found");
        }
    }

    private static void applyEmployment(Employee e, EmployeeRequest req) {
        e.setPositionId(req.positionId());
        e.setPositionTitle(trimToNull(req.positionTitle()));
        e.setDeptId(req.deptId());
        e.setManagerUserId(req.managerUserId());
        e.setEmploymentType(req.employmentType());
        e.setEmploymentStatus(req.employmentStatus());
        e.setStartDate(req.startDate());
        e.setProbationEndDate(req.probationEndDate());
        e.setEndDate(req.endDate());
        e.setWorkLocation(trimToNull(req.workLocation()));
        e.setSalaryAmount(req.salaryAmount());
        e.setSalaryCurrency(req.salaryCurrency() == null ? null : req.salaryCurrency().toUpperCase(Locale.ROOT));
        e.setPayFrequency(StringUtils.hasText(req.payFrequency()) ? req.payFrequency() : "MONTHLY");
        e.setEmergencyContact(trimToNull(req.emergencyContact()));
        e.setEmergencyContactRelationship(trimToNull(req.emergencyContactRelationship()));
        e.setEmergencyContactPhone(trimToNull(req.emergencyContactPhone()));
        e.setEmergencyContactEmail(trimToNull(req.emergencyContactEmail()));
        e.setNotes(trimToNull(req.notes()));
    }

    private String nextEmployeeNo() {
        Integer max = mapper.maxEmployeeSeq(EMPLOYEE_NO_PREFIX);
        return EMPLOYEE_NO_PREFIX + String.format(Locale.ROOT, "%04d", (max == null ? 0 : max) + 1);
    }

    private static String currentUser() {
        try {
            return SecurityUtils.getUsername();
        } catch (RuntimeException e) {
            return "system";
        }
    }

    private static String blankToNull(String s) {
        return StringUtils.hasText(s) ? s.trim() : null;
    }

    private static String trimToNull(String s) {
        return StringUtils.hasText(s) ? s.trim() : null;
    }
}
