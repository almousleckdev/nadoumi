package com.nadoumi.identity.access;

import com.nadoumi.common.access.AccessCapabilityMatrix;
import com.nadoumi.common.access.ApplicantCapability;
import com.nadoumi.common.access.ApplicationApplicantResolver;
import com.nadoumi.common.access.NadoumiAccessService;
import com.nadoumi.identity.domain.UserApplicantAccess;
import com.nadoumi.identity.mapper.UserApplicantAccessMapper;
import com.ruoyi.framework.web.service.PermissionService;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * The {@code @na} bean used by {@code @PreAuthorize} and re-checked inside services.
 * Staff go through RuoYi RBAC; external users go through a live
 * {@code nad_user_applicant_access} lookup + the {@link AccessCapabilityMatrix}.
 * Nothing here reads a cached permission set.
 */
@Service("na")
public class NadoumiAccessServiceImpl implements NadoumiAccessService {

    private final CurrentCaller caller;
    private final UserApplicantAccessMapper grants;
    private final PermissionService rbac;
    private final Optional<ApplicationApplicantResolver> applicationResolver;

    public NadoumiAccessServiceImpl(CurrentCaller caller, UserApplicantAccessMapper grants,
            PermissionService rbac, Optional<ApplicationApplicantResolver> applicationResolver) {
        this.caller = caller;
        this.grants = grants;
        this.rbac = rbac;
        this.applicationResolver = applicationResolver;
    }

    @Override
    public boolean canAccessApplicant(Long applicantId, String capability) {
        ApplicantCapability cap = parse(capability);
        Long userId = caller.userIdOrNull();
        if (applicantId == null || cap == null || userId == null) {
            return false;
        }
        if (caller.isStaff()) {
            return staffHolds(cap);
        }
        return externalHolds(grants.findActiveApplicantGrant(userId, applicantId), cap);
    }

    @Override
    public boolean canAccessApplication(Long applicationId, String capability) {
        ApplicantCapability cap = parse(capability);
        Long userId = caller.userIdOrNull();
        if (applicationId == null || cap == null || userId == null) {
            return false;
        }
        if (caller.isStaff()) {
            return staffHolds(cap);
        }
        Long applicantId = applicationResolver.map(r -> r.applicantIdOf(applicationId)).orElse(null);
        if (applicantId == null) {
            return false;
        }
        return externalHolds(grants.findActiveApplicationGrant(userId, applicantId, applicationId), cap);
    }

    @Override
    public boolean canViewScholarshipInternal() {
        return rbac.hasPermi("nad:scholarship:internal:view");
    }

    @Override
    public boolean canReviewDocument(Long documentId) {
        // Coarse RBAC check; the Document slice refines this with per-document assignment.
        return rbac.hasPermi("nad:document:verify") || rbac.hasPermi("nad:document:reject");
    }

    /** Accessible applicant ids for an external caller — the {@code authScope} for MyBatis finders. */
    public List<Long> accessibleApplicantIds() {
        Long userId = caller.userIdOrNull();
        return userId == null ? List.of() : grants.accessibleApplicantIds(userId);
    }

    private boolean externalHolds(UserApplicantAccess grant, ApplicantCapability cap) {
        if (grant == null) {
            return false;
        }
        CapabilityOverrides overrides = CapabilityOverrides.parse(grant.getCapabilityOverridesJson());
        return AccessCapabilityMatrix.hasCapability(grant.getAccessRole(), cap, overrides.add(), overrides.remove());
    }

    private boolean staffHolds(ApplicantCapability cap) {
        return switch (cap) {
            case VIEW_PROFILE, VIEW_APPLICATION, VIEW_DOCUMENT -> rbac.hasPermi("nad:applicant:view");
            case EDIT_PROFILE -> rbac.hasPermi("nad:applicant:edit");
            case MANAGE_ACCESS -> rbac.hasPermi("nad:applicant:access:manage");
            // cross-domain actions: staff use their own domain endpoints/tokens, not this predicate
            case CREATE_APPLICATION, SUBMIT_APPLICATION, UPLOAD_DOCUMENT, MESSAGE_STAFF -> false;
        };
    }

    private static ApplicantCapability parse(String capability) {
        try {
            return ApplicantCapability.valueOf(capability);
        } catch (IllegalArgumentException | NullPointerException e) {
            return null;
        }
    }
}
