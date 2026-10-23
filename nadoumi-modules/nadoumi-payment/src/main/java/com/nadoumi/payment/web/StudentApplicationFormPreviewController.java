package com.nadoumi.payment.web;

import com.nadoumi.common.exception.NadNotFoundException;
import com.nadoumi.application.service.ApplicationReadService;
import com.nadoumi.payment.service.PaymentDocumentService;
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

/** A look at the application form before it is paid for and sent: the same form, marked as a preview. */
@RestController
@RequestMapping("/api/student/applications/{id}/form-preview")
public class StudentApplicationFormPreviewController {

    private final PaymentDocumentService documents;
    private final ApplicationReadService applications;

    public StudentApplicationFormPreviewController(PaymentDocumentService documents, ApplicationReadService applications) {
        this.documents = documents;
        this.applications = applications;
    }

    @GetMapping
    @PreAuthorize("@na.canAccessApplication(#id, 'VIEW_APPLICATION')")
    public ResponseEntity<byte[]> preview(@PathVariable Long id) {
        long applicantId = applications.find(id).orElseThrow(() -> new NadNotFoundException("application not found")).applicantId();
        byte[] pdf = documents.previewForm(id, applicantId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename("application-form-preview.pdf").build().toString())
                .body(pdf);
    }
}
