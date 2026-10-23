package com.nadoumi.applicant.web;

import com.nadoumi.applicant.service.SignatureService;
import com.nadoumi.applicant.service.SignatureService.Status;
import com.nadoumi.common.media.MediaAccessLogContext;
import com.nadoumi.common.media.ProxyStream;
import com.ruoyi.common.utils.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** The student's own handwritten signature. */
@RestController
@RequestMapping("/api/student/applicants/{id}/signature")
public class StudentSignatureController {

    private final SignatureService service;

    public StudentSignatureController(SignatureService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("@na.canAccessApplicant(#id, 'VIEW_PROFILE')")
    public Status status(@PathVariable Long id) {
        return service.status(id);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@na.canAccessApplicant(#id, 'EDIT_PROFILE')")
    public Status save(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return service.save(id, file);
    }

    @GetMapping("/image")
    @PreAuthorize("@na.canAccessApplicant(#id, 'VIEW_PROFILE')")
    public ResponseEntity<InputStreamResource> image(@PathVariable Long id, HttpServletRequest request) {
        ProxyStream stream = service.open(id, new MediaAccessLogContext(SecurityUtils.getUserId(), id, null, null,
                request.getRemoteAddr(), request.getHeader("User-Agent")));
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .contentLength(stream.contentLength())
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .body(new InputStreamResource(stream.body()));
    }
}
