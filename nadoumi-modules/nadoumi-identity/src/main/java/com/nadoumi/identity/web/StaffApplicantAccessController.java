package com.nadoumi.identity.web;

import com.nadoumi.identity.service.UserApplicantAccessService;
import com.nadoumi.identity.web.response.AccessGrantResponse;
import com.nadoumi.identity.web.request.GrantAccessRequest;
import com.nadoumi.identity.web.request.TransferOwnershipRequest;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.enums.BusinessType;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/staff/applicants/{applicantId}/access")
public class StaffApplicantAccessController {

    private final UserApplicantAccessService grants;

    public StaffApplicantAccessController(UserApplicantAccessService grants) {
        this.grants = grants;
    }

    @GetMapping
    @PreAuthorize("@ss.hasPermi('nad:applicant:access:view')")
    public List<AccessGrantResponse> list(@PathVariable Long applicantId) {
        return grants.listForApplicant(applicantId).stream().map(AccessGrantResponse::of).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@ss.hasPermi('nad:applicant:access:manage')")
    @Log(title = "Applicant access", businessType = BusinessType.GRANT)
    public AccessGrantResponse grant(@PathVariable Long applicantId, @Valid @RequestBody GrantAccessRequest req) {
        return AccessGrantResponse.of(
                grants.delegate(applicantId, req.userId(), req.email(), req.role(), req.expiresAt()));
    }

    @DeleteMapping("/{grantId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@ss.hasPermi('nad:applicant:access:manage')")
    @Log(title = "Applicant access", businessType = BusinessType.UPDATE)
    public void revoke(@PathVariable Long applicantId, @PathVariable Long grantId,
            @RequestParam(required = false) String reason) {
        grants.revoke(grantId, reason);
    }

    @PostMapping("/transfer-ownership")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@ss.hasPermi('nad:applicant:access:manage')")
    @Log(title = "Applicant access", businessType = BusinessType.UPDATE)
    public void transfer(@PathVariable Long applicantId, @Valid @RequestBody TransferOwnershipRequest req) {
        grants.transferOwnership(applicantId, req.toUserId(), req.previousOwnerNewRole());
    }
}
