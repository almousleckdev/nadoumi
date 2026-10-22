package com.nadoumi.application.web;

import com.nadoumi.application.service.ApplicationTemplateService;
import com.nadoumi.common.media.MediaAccessLogContext;
import com.nadoumi.common.media.ProxyStream;
import com.nadoumi.document.domain.DocumentTemplate;
import com.nadoumi.document.service.DocumentTemplateService;
import com.nadoumi.document.web.response.DocumentTemplateResponse;
import com.ruoyi.common.utils.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The form templates of the programme or scholarship an application is for. */
@RestController
@RequestMapping("/api/student/applications/{id}/templates")
public class StudentApplicationTemplateController {

    private final ApplicationTemplateService applicationTemplates;
    private final DocumentTemplateService templates;

    public StudentApplicationTemplateController(ApplicationTemplateService applicationTemplates,
            DocumentTemplateService templates) {
        this.applicationTemplates = applicationTemplates;
        this.templates = templates;
    }

    @GetMapping
    @PreAuthorize("@na.canAccessApplication(#id, 'VIEW_APPLICATION')")
    public List<DocumentTemplateResponse> list(@PathVariable Long id) {
        return applicationTemplates.forApplication(id).stream().map(DocumentTemplateResponse::of).toList();
    }

    @GetMapping("/{templateId}")
    @PreAuthorize("@na.canAccessApplication(#id, 'VIEW_APPLICATION')")
    public ResponseEntity<InputStreamResource> download(@PathVariable Long id, @PathVariable Long templateId,
            HttpServletRequest request) {
        DocumentTemplate template = applicationTemplates.find(id, templateId);
        ProxyStream stream = templates.open(template, new MediaAccessLogContext(SecurityUtils.getUserId(), null, id, null,
                request.getRemoteAddr(), request.getHeader("User-Agent")));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(stream.contentType()))
                .contentLength(stream.contentLength())
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(template.getFileName()).build().toString())
                .body(new InputStreamResource(stream.body()));
    }
}
