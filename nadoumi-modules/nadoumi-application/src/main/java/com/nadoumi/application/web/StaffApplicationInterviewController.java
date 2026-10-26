package com.nadoumi.application.web;

import com.nadoumi.application.service.ApplicationInterviewService;
import com.nadoumi.application.web.request.ScheduleInterviewRequest;
import com.nadoumi.application.web.response.InterviewResponse;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.AuditActor;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Staff schedule, reschedule and cancel the interview of an application. */
@RestController
@RequestMapping("/api/staff/applications/{id}/interviews")
public class StaffApplicationInterviewController {

    private final ApplicationInterviewService service;

    public StaffApplicationInterviewController(ApplicationInterviewService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("@ss.hasPermi('nad:application:view')")
    public List<InterviewResponse> list(@PathVariable Long id) {
        return service.list(id);
    }

    @PostMapping
    @PreAuthorize("@ss.hasPermi('nad:application:transition')")
    @Log(title = "Application interview", businessType = BusinessType.INSERT)
    @ResponseStatus(HttpStatus.CREATED)
    public InterviewResponse schedule(@PathVariable Long id, @Valid @RequestBody ScheduleInterviewRequest req) {
        return service.schedule(id, req, AuditActor.userId());
    }

    @PostMapping("/{interviewId}/cancel")
    @PreAuthorize("@ss.hasPermi('nad:application:transition')")
    @Log(title = "Application interview", businessType = BusinessType.UPDATE)
    public void cancel(@PathVariable Long id, @PathVariable Long interviewId) {
        service.cancel(id, interviewId, AuditActor.userId());
    }
}
