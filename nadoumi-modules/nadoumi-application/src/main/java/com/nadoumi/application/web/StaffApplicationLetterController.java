package com.nadoumi.application.web;

import com.nadoumi.application.service.ApplicationLetterService;
import com.nadoumi.application.web.response.LetterResponse;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.AuditActor;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Staff upload and read the admission letter and JW202 of an application. */
@RestController
@RequestMapping("/api/staff/applications/{id}/letters")
public class StaffApplicationLetterController {

    private final ApplicationLetterService service;

    public StaffApplicationLetterController(ApplicationLetterService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("@ss.hasPermi('nad:application:view')")
    public List<LetterResponse> list(@PathVariable Long id) {
        return service.listForStaff(id);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@ss.hasPermi('nad:application:transition')")
    @Log(title = "Application letter", businessType = BusinessType.INSERT)
    @ResponseStatus(HttpStatus.CREATED)
    public LetterResponse upload(@PathVariable Long id, @RequestParam String kind, @RequestParam("file") MultipartFile file) {
        return service.upload(id, LetterResponses.kind(kind), file, AuditActor.userId());
    }

    @GetMapping("/{kind}")
    @PreAuthorize("@ss.hasPermi('nad:application:view')")
    public ResponseEntity<InputStreamResource> open(@PathVariable Long id, @PathVariable String kind,
            HttpServletRequest request) {
        var letterKind = LetterResponses.kind(kind);
        return LetterResponses.stream(
                service.openForStaff(id, letterKind, LetterResponses.context(id, request)), letterKind.name().toLowerCase());
    }
}
