package com.nadoumi.payment.web;

import com.nadoumi.common.media.MediaAccessLogContext;
import com.nadoumi.common.media.ProxyStream;
import com.nadoumi.common.web.PageResponse;
import com.nadoumi.payment.domain.enums.PaymentStatus;
import com.nadoumi.payment.service.PaymentVerificationService;
import com.nadoumi.payment.web.request.RejectPaymentRequest;
import com.nadoumi.payment.web.response.StaffPaymentResponse;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Staff payment verification: the queue, the proof, and the confirm / reject decision. */
@RestController
@RequestMapping("/api/staff/payments")
public class StaffPaymentController {

    private final PaymentVerificationService service;

    public StaffPaymentController(PaymentVerificationService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("@ss.hasPermi('nad:payment:list')")
    public PageResponse<StaffPaymentResponse> list(
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.list(status, q, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('nad:payment:query')")
    public StaffPaymentResponse get(@PathVariable long id) {
        return service.get(id);
    }

    /** The payment screenshot, streamed by the backend and access-logged; never a storage URL. */
    @GetMapping("/{id}/proof")
    @PreAuthorize("@ss.hasPermi('nad:payment:query')")
    public ResponseEntity<InputStreamResource> proof(@PathVariable long id, HttpServletRequest request) {
        ProxyStream stream = service.openProof(id, accessContext(request));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(stream.contentType()))
                .contentLength(stream.contentLength())
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(stream.downloadFilename() == null ? "payment-proof" : stream.downloadFilename())
                        .build().toString())
                .body(new InputStreamResource(stream.body()));
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("@ss.hasPermi('nad:payment:verify')")
    @Log(title = "Application payment", businessType = BusinessType.UPDATE)
    public StaffPaymentResponse confirm(@PathVariable long id) {
        return service.confirm(id);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("@ss.hasPermi('nad:payment:verify')")
    @Log(title = "Application payment", businessType = BusinessType.UPDATE)
    public StaffPaymentResponse reject(@PathVariable long id, @Valid @RequestBody RejectPaymentRequest req) {
        return service.reject(id, req.reason());
    }

    private static MediaAccessLogContext accessContext(HttpServletRequest request) {
        LoginUser user = SecurityUtils.getLoginUser();
        return new MediaAccessLogContext(user.getUserId(), null, null, null,
                request.getRemoteAddr(), request.getHeader("User-Agent"));
    }
}
