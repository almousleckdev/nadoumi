package com.nadoumi.application.web;

import com.nadoumi.application.service.StudentApplicationService;
import com.nadoumi.application.web.request.StartApplicationRequest;
import com.nadoumi.application.web.response.StudentApplicationResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Apply Now entry point (spec §II.7). Despite the {@code /api/public} prefix this
 * requires authentication — it is deliberately NOT annotated {@code @Anonymous}
 * (see {@code PublicScholarshipController} for how that annotation is used
 * elsewhere; its absence here is the point). Authorization against the target
 * applicant is checked inside {@link StudentApplicationService#create}.
 */
@RestController
@RequestMapping("/api/public/applications")
public class PublicApplicationController {

    private final StudentApplicationService service;

    public PublicApplicationController(StudentApplicationService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StudentApplicationResponse create(@Valid @RequestBody StartApplicationRequest req) {
        return service.create(req);
    }
}
