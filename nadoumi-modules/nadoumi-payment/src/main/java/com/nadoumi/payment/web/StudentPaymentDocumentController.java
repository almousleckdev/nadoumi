package com.nadoumi.payment.web;

import com.nadoumi.common.media.MediaAccessLogContext;
import com.nadoumi.payment.domain.enums.DocumentKind;
import com.nadoumi.payment.service.PaymentDocumentService;
import com.nadoumi.payment.service.PaymentDocumentService.OpenedDocument;
import com.nadoumi.payment.web.response.PaymentDocumentResponse;
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
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The receipt and application form of an application, for the student who may view it. */
@RestController
@RequestMapping("/api/student/applications/{id}/payment/documents")
public class StudentPaymentDocumentController {

    private final PaymentDocumentService service;

    public StudentPaymentDocumentController(PaymentDocumentService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("@na.canAccessApplication(#id, 'VIEW_APPLICATION')")
    public List<PaymentDocumentResponse> list(@PathVariable Long id) {
        return service.listFor(id).stream().map(PaymentDocumentResponse::of).toList();
    }

    /** {@code kind} is {@code RECEIPT} or {@code APPLICATION_FORM}; anything else is a 404. */
    @GetMapping("/{kind}")
    @PreAuthorize("@na.canAccessApplication(#id, 'VIEW_APPLICATION')")
    public ResponseEntity<InputStreamResource> download(@PathVariable Long id, @PathVariable String kind,
            HttpServletRequest request) {
        OpenedDocument opened = service.open(id, parse(kind), accessContext(id, request));
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(opened.stream().contentLength())
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(opened.document().getDocNumber() + ".pdf").build().toString())
                .body(new InputStreamResource(opened.stream().body()));
    }

    /** Emails the receipt to the student again (at most once every few minutes). */
    @PostMapping("/RECEIPT/resend")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("@na.canAccessApplication(#id, 'VIEW_APPLICATION')")
    public void resendReceipt(@PathVariable Long id) {
        service.resendReceipt(id);
    }

    private static DocumentKind parse(String kind) {
        try {
            return DocumentKind.valueOf(kind.toUpperCase(java.util.Locale.ROOT));
        }
        catch (IllegalArgumentException e) {
            throw new com.nadoumi.common.exception.NadNotFoundException("document not found");
        }
    }

    private static MediaAccessLogContext accessContext(long applicationId, HttpServletRequest request) {
        return new MediaAccessLogContext(SecurityUtils.getUserId(), null, applicationId, null,
                request.getRemoteAddr(), request.getHeader("User-Agent"));
    }
}
