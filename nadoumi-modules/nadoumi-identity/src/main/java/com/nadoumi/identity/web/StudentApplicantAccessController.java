package com.nadoumi.identity.web;

import com.nadoumi.identity.service.UserApplicantAccessService;
import com.nadoumi.identity.web.response.AccessGrantResponse;
import com.nadoumi.identity.web.request.GrantAccessRequest;
import com.nadoumi.identity.web.request.TransferOwnershipRequest;
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

/** Owner-driven delegation. Only OWNER holds MANAGE_ACCESS, so the predicate gates it. */
@RestController
@RequestMapping("/api/student/applicants/{applicantId}/access")
public class StudentApplicantAccessController {

    private final UserApplicantAccessService grants;

    public StudentApplicantAccessController(UserApplicantAccessService grants) {
        this.grants = grants;
    }

    @GetMapping
    @PreAuthorize("@na.canAccessApplicant(#applicantId, 'VIEW_PROFILE')")
    public List<AccessGrantResponse> list(@PathVariable Long applicantId) {
        return grants.listForApplicant(applicantId).stream().map(AccessGrantResponse::of).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@na.canAccessApplicant(#applicantId, 'MANAGE_ACCESS')")
    public AccessGrantResponse grant(@PathVariable Long applicantId, @Valid @RequestBody GrantAccessRequest req) {
        return AccessGrantResponse.of(
                grants.delegate(applicantId, req.userId(), req.email(), req.role(), req.expiresAt()));
    }

    @DeleteMapping("/{grantId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@na.canAccessApplicant(#applicantId, 'MANAGE_ACCESS')")
    public void revoke(@PathVariable Long applicantId, @PathVariable Long grantId,
            @RequestParam(required = false) String reason) {
        grants.revoke(grantId, reason);
    }

    @PostMapping("/transfer-ownership")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@na.canAccessApplicant(#applicantId, 'MANAGE_ACCESS')")
    public void transfer(@PathVariable Long applicantId, @Valid @RequestBody TransferOwnershipRequest req) {
        grants.transferOwnership(applicantId, req.toUserId(), req.previousOwnerNewRole());
    }
}
