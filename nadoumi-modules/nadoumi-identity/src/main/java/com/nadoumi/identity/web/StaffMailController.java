package com.nadoumi.identity.web;

import com.nadoumi.identity.service.mail.MailDiagnostics;
import com.nadoumi.identity.service.mail.MailDiagnostics.MailConfig;
import com.nadoumi.identity.service.mail.MailDiagnostics.TestResult;
import com.nadoumi.identity.web.request.MailTestRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin-only diagnostics for outbound email. {@code GET /config} reports the
 * effective wiring (no secrets); {@code POST /test} sends one message and returns
 * the real outcome, so a "no OTP email" report can be pinned to transport,
 * credentials or delivery without host access.
 */
@RestController
@RequestMapping("/api/staff/mail")
@PreAuthorize("@ss.hasRole('admin')")
public class StaffMailController {

    private final MailDiagnostics diagnostics;

    public StaffMailController(MailDiagnostics diagnostics) {
        this.diagnostics = diagnostics;
    }

    @GetMapping("/config")
    public MailConfig config() {
        return diagnostics.config();
    }

    @PostMapping("/test")
    public TestResult test(@Valid @RequestBody MailTestRequest req) {
        return diagnostics.sendTest(req.to());
    }
}
