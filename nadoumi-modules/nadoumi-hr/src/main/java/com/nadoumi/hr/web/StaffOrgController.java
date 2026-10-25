package com.nadoumi.hr.web;

import com.nadoumi.hr.service.OrgOverviewService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Head counts behind the roles, departments and posts screens. */
@RestController
@RequestMapping("/api/staff/org")
public class StaffOrgController {

    private final OrgOverviewService service;

    public StaffOrgController(OrgOverviewService service) {
        this.service = service;
    }

    @GetMapping("/overview")
    @PreAuthorize("@ss.hasAnyPermi('system:role:list,system:dept:list,system:post:list')")
    public OrgOverviewService.Overview overview() {
        return service.overview();
    }
}
