package com.nadoumi.payment.web;

import com.nadoumi.application.service.ApplicationReadService;
import com.nadoumi.common.exception.NadNotFoundException;
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

/** The submitted application form as a PDF, for the staff reviewing the application. */
@RestController
@RequestMapping("/api/staff/applications/{id}/form")
public class StaffApplicationFormController {

    private final PaymentDocumentService documents;
    private final ApplicationReadService applications;

    public StaffApplicationFormController(PaymentDocumentService documents, ApplicationReadService applications) {
        this.documents = documents;
        this.applications = applications;
    }

    @GetMapping
    @PreAuthorize("@ss.hasPermi('nad:application:view')")
    public ResponseEntity<byte[]> form(@PathVariable Long id) {
        var application = applications.find(id).orElseThrow(() -> new NadNotFoundException("application not found"));
        byte[] pdf = documents.currentForm(id, application.applicantId());
        String filename = (application.reference() == null ? "application-" + id : application.reference()) + "-form.pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                .body(pdf);
    }
}
