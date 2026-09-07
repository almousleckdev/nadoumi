package com.nadoumi.hr.web;

import com.nadoumi.common.web.PageResponse;
import com.nadoumi.hr.service.TaskService;
import com.nadoumi.hr.web.request.TaskRequest;
import com.nadoumi.hr.web.request.TaskStatusRequest;
import com.nadoumi.hr.web.response.TaskResponse;
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

/** Task admin: create / assign / edit, and drive the lifecycle through {@code /{id}/status}. */
@RestController
@RequestMapping("/api/staff/tasks")
public class StaffTaskController {

    private static final String APPROVE_PERM = "nad:task:approve";

    private final TaskService service;

    public StaffTaskController(TaskService service) {
        this.service = service;
    }

    private long actor() {
        return SecurityUtils.getUserId();
    }

    private boolean isApprover() {
        Set<String> perms = SecurityUtils.getLoginUser().getPermissions();
        return perms.contains("*:*:*") || perms.contains(APPROVE_PERM);
    }

    @GetMapping
    @PreAuthorize("@ss.hasPermi('nad:task:list')")
    public PageResponse<TaskResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) Long assigneeUserId,
            @RequestParam(required = false) Boolean mine,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long createdBy = Boolean.TRUE.equals(mine) ? actor() : null;
        // Managers / approvers see every task; everyone else is scoped to tasks
        // they were assigned or created.
        Long ownedBy = isApprover() ? null : actor();
        return service.list(q, status, priority, assigneeUserId, createdBy, ownedBy, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('nad:task:query')")
    public TaskResponse get(@PathVariable long id) {
        return service.get(id);
    }

    @PostMapping
    @PreAuthorize("@ss.hasPermi('nad:task:add')")
    @Log(title = "Task", businessType = BusinessType.INSERT)
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse create(@Valid @RequestBody TaskRequest req) {
        return service.create(req, actor());
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('nad:task:edit')")
    @Log(title = "Task", businessType = BusinessType.UPDATE)
    public TaskResponse update(@PathVariable long id, @Valid @RequestBody TaskRequest req) {
        return service.update(id, req, actor());
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("@ss.hasAnyPermi('nad:task:progress,nad:task:edit')")
    @Log(title = "Task status", businessType = BusinessType.UPDATE)
    public TaskResponse changeStatus(@PathVariable long id, @Valid @RequestBody TaskStatusRequest req) {
        return service.changeStatus(id, req.status(), req.note(), actor(), isApprover());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('nad:task:remove')")
    @Log(title = "Task", businessType = BusinessType.DELETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        service.delete(id);
    }
}
