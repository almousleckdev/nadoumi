package com.nadoumi.application.web;

import com.nadoumi.application.service.ApplicationInterviewService;
import com.nadoumi.application.web.response.InterviewResponse;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The student reads the interview of their own application; scheduling belongs to staff. */
@RestController
@RequestMapping("/api/student/applications/{id}/interviews")
public class StudentApplicationInterviewController {

    private final ApplicationInterviewService service;

    public StudentApplicationInterviewController(ApplicationInterviewService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("@na.canAccessApplication(#id, 'VIEW_APPLICATION')")
    public List<InterviewResponse> list(@PathVariable Long id) {
        return service.list(id);
    }
}
