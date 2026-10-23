package com.nadoumi.application.web;

import com.nadoumi.application.service.StudentApplicationService;
import com.nadoumi.application.web.request.UpdateApplicationRequest;
import com.nadoumi.application.web.response.StudentApplicationResponse;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import com.nadoumi.application.service.ApplicationChecklistService;
import com.nadoumi.application.web.request.StartApplicationRequest;
import com.nadoumi.application.web.response.ChecklistItemResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Student application surface (spec §II.7). Every endpoint is authorization-checked
 * against the caller's {@code nad_user_applicant_access} grant — own applicant or a
 * live on-behalf-of grant, never by trusting the path alone.
 */
@RestController
@RequestMapping("/api/student/applications")
public class StudentApplicationController {

    private final StudentApplicationService service;
    private final ApplicationChecklistService checklist;

    public StudentApplicationController(StudentApplicationService service, ApplicationChecklistService checklist) {
        this.service = service;
        this.checklist = checklist;
    }

    @GetMapping
    public List<StudentApplicationResponse> listMine() {
        return service.listMine();
    }

    /** Starts a draft for an applicant the caller may act for; the grant is checked inside the service. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StudentApplicationResponse create(@Valid @RequestBody StartApplicationRequest req) {
        return service.create(req);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@na.canAccessApplication(#id, 'VIEW_APPLICATION')")
    public StudentApplicationResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    /** The documents this application needs and where the student stands on each. */
    @GetMapping("/{id}/documents/checklist")
    @PreAuthorize("@na.canAccessApplication(#id, 'VIEW_APPLICATION')")
    public List<ChecklistItemResponse> checklist(@PathVariable Long id) {
        return checklist.checklist(id).stream().map(ChecklistItemResponse::of).toList();
    }

    @PutMapping("/{id}")
    @PreAuthorize("@na.canAccessApplication(#id, 'CREATE_APPLICATION')")
    public StudentApplicationResponse update(@PathVariable Long id, @RequestBody UpdateApplicationRequest req) {
        return service.updateDraft(id, req);
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("@na.canAccessApplication(#id, 'SUBMIT_APPLICATION')")
    public StudentApplicationResponse submit(@PathVariable Long id) {
        return service.submit(id);
    }

    @PostMapping("/{id}/transitions/{code}")
    @PreAuthorize("@na.canAccessApplication(#id, 'SUBMIT_APPLICATION')")
    public StudentApplicationResponse transition(@PathVariable Long id, @PathVariable String code,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body == null ? null : body.get("reason");
        return service.transition(id, code, reason);
    }
}
