package com.nadoumi.payment.web;

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
 * Student payment surface for one application. Every endpoint is authorization-checked against the caller's
 * grant on the application, never by trusting the path: reading needs {@code VIEW_APPLICATION}; opening a
 * session, reporting a payment and cancelling need {@code SUBMIT_APPLICATION}.
 */
@RestController
@RequestMapping("/api/student/applications/{id}/payment")
public class StudentPaymentController {

    private final ApplicationPaymentService service;

    public StudentPaymentController(ApplicationPaymentService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("@na.canAccessApplication(#id, 'VIEW_APPLICATION')")
    public StudentPaymentResponse overview(@PathVariable Long id) {
        return service.overview(id);
    }

    @PostMapping("/session")
    @PreAuthorize("@na.canAccessApplication(#id, 'SUBMIT_APPLICATION')")
    public StudentPaymentResponse start(@PathVariable Long id) {
        return service.start(id);
    }

    /** The QR image; only while the student's own session is open. Never cached, never public. */
    @GetMapping("/qr")
    @PreAuthorize("@na.canAccessApplication(#id, 'SUBMIT_APPLICATION')")
    public ResponseEntity<byte[]> qr(@PathVariable Long id) {
        QrImage image = service.qr(id);
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
        return service.submitProof(id, transactionNo, file);
    }

    @PostMapping("/hold")
    @PreAuthorize("@na.canAccessApplication(#id, 'SUBMIT_APPLICATION')")
    public StudentPaymentResponse hold(@PathVariable Long id) {
        return service.hold(id);
    }

    @PostMapping("/cancel")
    @PreAuthorize("@na.canAccessApplication(#id, 'SUBMIT_APPLICATION')")
    public StudentPaymentResponse cancel(@PathVariable Long id) {
        return service.cancel(id);
    }
}
