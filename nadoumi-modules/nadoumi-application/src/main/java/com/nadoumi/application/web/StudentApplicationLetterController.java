package com.nadoumi.application.web;

import com.nadoumi.application.service.ApplicationLetterService;
import com.nadoumi.application.web.response.LetterResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The student's admission letter and JW202, released by the server only after the service fee is confirmed. */
@RestController
@RequestMapping("/api/student/applications/{id}/letters")
public class StudentApplicationLetterController {

    private final ApplicationLetterService service;

    public StudentApplicationLetterController(ApplicationLetterService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("@na.canAccessApplication(#id, 'VIEW_APPLICATION')")
    public List<LetterResponse> list(@PathVariable Long id) {
        return service.listForStudent(id);
    }

    @GetMapping("/{kind}")
    @PreAuthorize("@na.canAccessApplication(#id, 'VIEW_APPLICATION')")
    public ResponseEntity<InputStreamResource> open(@PathVariable Long id, @PathVariable String kind,
            HttpServletRequest request) {
        var letterKind = LetterResponses.kind(kind);
        return LetterResponses.stream(
                service.openForStudent(id, letterKind, LetterResponses.context(id, request)), letterKind.name().toLowerCase());
    }
}
