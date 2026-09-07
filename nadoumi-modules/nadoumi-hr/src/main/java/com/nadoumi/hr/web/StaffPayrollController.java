package com.nadoumi.hr.web;

import com.nadoumi.hr.service.PayrollService;
import com.nadoumi.hr.web.response.PayrollSummary;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Read-only staff compensation summary. */
@RestController
@RequestMapping("/api/staff/payroll")
public class StaffPayrollController {

    private final PayrollService service;

    public StaffPayrollController(PayrollService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("@ss.hasPermi('nad:payroll:view')")
    public PayrollSummary summary() {
        return service.summary();
    }
}
