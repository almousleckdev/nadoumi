package com.nadoumi.application.web;

import com.nadoumi.application.domain.WfDefinition;
import com.nadoumi.application.domain.WfStage;
import com.nadoumi.application.mapper.ApplicationSearch;
import com.nadoumi.application.service.ApplicationAdminService;
import com.nadoumi.application.web.request.AssignRequest;
import com.nadoumi.application.web.request.DecisionRequest;
import com.nadoumi.application.web.request.SkipTaskRequest;
import com.nadoumi.application.web.request.StartApplicationRequest;
import com.nadoumi.application.web.request.TransitionRequest;
import com.nadoumi.application.web.request.UpdateApplicationRequest;
import com.nadoumi.application.web.response.ApplicationDetailResponse;
import com.nadoumi.application.web.response.ApplicationResponse;
import com.nadoumi.application.web.response.DecisionResponse;
import com.nadoumi.common.web.PageResponse;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.SecurityUtils;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Staff application case management (spec §II.6). */
@RestController
@RequestMapping("/api/staff/applications")
public class StaffApplicationController {

    private final ApplicationAdminService service;

    public StaffApplicationController(ApplicationAdminService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("@ss.hasPermi('nad:application:list')")
    public PageResponse<ApplicationResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String applicationType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long stageId,
            @RequestParam(required = false) Long assigneeUserId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        boolean orgWide = SecurityUtils.hasRole("ops_manager") || SecurityUtils.hasRole("nadoumi_super_admin");
        Long scope = orgWide ? null : SecurityUtils.getUserId();
        ApplicationSearch filter = new ApplicationSearch(q, applicationType, status, stageId, assigneeUserId, scope);
        return service.list(filter, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('nad:application:view')")
    public ApplicationDetailResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @PreAuthorize("@ss.hasPermi('nad:application:create')")
    @Log(title = "Application", businessType = BusinessType.INSERT)
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationResponse create(@Valid @RequestBody StartApplicationRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('nad:application:edit')")
    @Log(title = "Application", businessType = BusinessType.UPDATE)
    public ApplicationResponse update(@PathVariable Long id, @RequestBody UpdateApplicationRequest req) {
        return service.update(id, req);
    }

    @PostMapping("/{id}/transitions/{code}")
    @PreAuthorize("@ss.hasPermi('nad:application:transition')")
    @Log(title = "Application transition", businessType = BusinessType.UPDATE)
    public ApplicationResponse transition(@PathVariable Long id, @PathVariable String code,
            @Valid @RequestBody TransitionRequest req) {
        return service.transition(id, code, req);
    }

    @PostMapping("/{id}/decisions")
    @PreAuthorize("@ss.hasPermi('nad:application:decide')")
    @Log(title = "Application decision", businessType = BusinessType.INSERT)
    public DecisionResponse decide(@PathVariable Long id, @Valid @RequestBody DecisionRequest req) {
        return service.decide(id, req);
    }

    @PostMapping("/{id}/tasks/{taskId}/complete")
    @PreAuthorize("@ss.hasPermi('nad:application:transition')")
    @Log(title = "Application task", businessType = BusinessType.UPDATE)
    public void completeTask(@PathVariable Long id, @PathVariable Long taskId) {
        service.completeTask(id, taskId);
    }

    @PostMapping("/{id}/tasks/{taskId}/skip")
    @PreAuthorize("@ss.hasPermi('nad:application:transition')")
    @Log(title = "Application task", businessType = BusinessType.UPDATE)
    public void skipTask(@PathVariable Long id, @PathVariable Long taskId, @Valid @RequestBody SkipTaskRequest req) {
        service.skipTask(id, taskId, req);
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("@ss.hasPermi('nad:application:assign')")
    @Log(title = "Application assignment", businessType = BusinessType.UPDATE)
    public void assign(@PathVariable Long id, @Valid @RequestBody AssignRequest req) {
        service.assign(id, req);
    }

    @PostMapping("/{id}/claim")
    @PreAuthorize("@ss.hasPermi('nad:application:claim')")
    @Log(title = "Application assignment", businessType = BusinessType.UPDATE)
    public void claim(@PathVariable Long id) {
        service.claim(id);
    }

    @GetMapping("/workflow-definitions")
    @PreAuthorize("@ss.hasPermi('nad:application:view')")
    public List<WfDefinition> definitions() {
        return service.listDefinitions();
    }

    @GetMapping("/workflow-definitions/{id}/stages")
    @PreAuthorize("@ss.hasPermi('nad:application:view')")
    public List<WfStage> definitionStages(@PathVariable Long id) {
        return service.definitionStages(id);
    }
}
