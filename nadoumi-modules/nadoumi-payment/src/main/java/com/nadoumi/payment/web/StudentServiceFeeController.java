package com.nadoumi.payment.web;

import com.nadoumi.application.service.PaymentGuardProvider;
import com.nadoumi.payment.service.ApplicationPaymentService;
import com.nadoumi.payment.service.PaymentQrSource.QrImage;
import com.nadoumi.payment.web.response.StudentPaymentResponse;
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

/**
 * The Nadoumi service fee a student pays once the university has confirmed the admission: the same session, QR and
 * receipt flow as the application fee, for the SERVICE_FEE kind. Every endpoint is authorization-checked against the
 * caller's grant on the application: reading needs {@code VIEW_APPLICATION}; the rest needs {@code SUBMIT_APPLICATION}.
 */
@RestController
@RequestMapping("/api/student/applications/{id}/service-fee")
public class StudentServiceFeeController {

    private static final String KIND = PaymentGuardProvider.SERVICE_FEE;

    private final ApplicationPaymentService service;

    public StudentServiceFeeController(ApplicationPaymentService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("@na.canAccessApplication(#id, 'VIEW_APPLICATION')")
    public StudentPaymentResponse overview(@PathVariable Long id) {
        return service.overview(id, KIND);
    }

    @PostMapping("/session")
    @PreAuthorize("@na.canAccessApplication(#id, 'SUBMIT_APPLICATION')")
    public StudentPaymentResponse start(@PathVariable Long id) {
        return service.start(id, KIND);
    }

    /** The QR image; only while the student's own session is open. Never cached, never public. */
    @GetMapping("/qr")
    @PreAuthorize("@na.canAccessApplication(#id, 'SUBMIT_APPLICATION')")
    public ResponseEntity<byte[]> qr(@PathVariable Long id) {
        QrImage image = service.qr(id, KIND);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .body(image.bytes());
    }

    @PostMapping(value = "/proof", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@na.canAccessApplication(#id, 'SUBMIT_APPLICATION')")
    public StudentPaymentResponse proof(@PathVariable Long id, @RequestParam String transactionNo,
            @RequestParam("file") MultipartFile file) {
        return service.submitProof(id, KIND, transactionNo, file);
    }

    @PostMapping("/hold")
    @PreAuthorize("@na.canAccessApplication(#id, 'SUBMIT_APPLICATION')")
    public StudentPaymentResponse hold(@PathVariable Long id) {
        return service.hold(id, KIND);
    }

    @PostMapping("/cancel")
    @PreAuthorize("@na.canAccessApplication(#id, 'SUBMIT_APPLICATION')")
    public StudentPaymentResponse cancel(@PathVariable Long id) {
        return service.cancel(id, KIND);
    }
}
