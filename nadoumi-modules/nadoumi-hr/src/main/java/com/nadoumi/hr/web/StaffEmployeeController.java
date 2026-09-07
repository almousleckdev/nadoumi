package com.nadoumi.hr.web;

import com.nadoumi.common.web.PageResponse;
import com.nadoumi.hr.service.EmployeeService;
import com.nadoumi.hr.web.response.EmployeeResponse;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.SecurityUtils;
import jakarta.validation.Valid;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.nadoumi.hr.web.request.EmployeeRequest;

/**
 * Employee (HR) admin. Salary fields are returned only to callers holding
 * {@code nad:employee:compensation:view}.
 */
@RestController
@RequestMapping("/api/staff/employees")
public class StaffEmployeeController {

    private static final String COMPENSATION_PERM = "nad:employee:compensation:view";

    private final EmployeeService service;

    public StaffEmployeeController(EmployeeService service) {
        this.service = service;
    }

    private boolean canViewCompensation() {
        Set<String> perms = SecurityUtils.getLoginUser().getPermissions();
        return perms.contains("*:*:*") || perms.contains(COMPENSATION_PERM);
    }

    @GetMapping
    @PreAuthorize("@ss.hasPermi('nad:employee:list')")
    public PageResponse<EmployeeResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) String employmentStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.list(q, deptId, employmentStatus, page, size, canViewCompensation());
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('nad:employee:query')")
    public EmployeeResponse get(@PathVariable long id) {
        return service.get(id, canViewCompensation());
    }

    @PostMapping
    @PreAuthorize("@ss.hasPermi('nad:employee:add')")
    @Log(title = "Employee", businessType = BusinessType.INSERT)
    @ResponseStatus(HttpStatus.CREATED)
    public EmployeeResponse create(@Valid @RequestBody EmployeeRequest req) {
        return service.create(req, canViewCompensation());
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('nad:employee:edit')")
    @Log(title = "Employee", businessType = BusinessType.UPDATE)
    public EmployeeResponse update(@PathVariable long id, @Valid @RequestBody EmployeeRequest req) {
        return service.update(id, req, canViewCompensation());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('nad:employee:remove')")
    @Log(title = "Employee", businessType = BusinessType.DELETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        service.delete(id);
    }
}
