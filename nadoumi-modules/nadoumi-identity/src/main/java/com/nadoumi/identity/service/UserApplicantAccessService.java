package com.nadoumi.identity.service;

import com.nadoumi.common.access.AccessGrantStatus;
import com.nadoumi.common.access.AccessRole;
import com.nadoumi.identity.access.CurrentCaller;
import com.nadoumi.identity.domain.UserApplicantAccess;
import com.nadoumi.identity.exception.GrantException;
import com.nadoumi.identity.mapper.UserApplicantAccessMapper;
import com.ruoyi.framework.web.service.PermissionService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lifecycle of {@code nad_user_applicant_access} (docs/DOMAIN_MODEL.md §4.3):
 * self-registration owner, staff-created applicant (interim owner + pending invite),
 * invite acceptance, delegation, revocation, ownership transfer.
 *
 * <p>Invariant INV1 (one ACTIVE OWNER per applicant) is also enforced by the DB
 * {@code owner_guard} unique key; the ownership paths take a row lock first so a
 * concurrent transfer fails cleanly rather than on the constraint.</p>
 */
@Service
public class UserApplicantAccessService {

    static final int INTERIM_OWNER_DAYS = 30;

    private final UserApplicantAccessMapper mapper;
    private final CurrentCaller caller;
    private final PermissionService rbac;

    public UserApplicantAccessService(UserApplicantAccessMapper mapper, CurrentCaller caller, PermissionService rbac) {
        this.mapper = mapper;
        this.caller = caller;
        this.rbac = rbac;
    }

    /** Self-registration: the registering user becomes the applicant's ACTIVE OWNER. */
    @Transactional(rollbackFor = Exception.class)
    public UserApplicantAccess grantOwnerOnSelfRegistration(Long userId, Long applicantId) {
        if (mapper.findActiveOwner(applicantId) != null) {
            throw new GrantException("applicant already has an active owner");
        }
        UserApplicantAccess owner = newGrant(applicantId, AccessRole.OWNER, AccessGrantStatus.ACTIVE);
        owner.setUserId(userId);
        owner.setGrantedByUserId(userId);
        owner.setGrantedAt(LocalDateTime.now());
        owner.setCreateBy(String.valueOf(userId));
        mapper.insert(owner);
        return owner;
    }

    /**
     * Staff-created applicant: a transitional staff-held ACTIVE OWNER
     * ({@code is_interim=1}, expires in {@value #INTERIM_OWNER_DAYS} days) plus a
     * PENDING OWNER invite to {@code invitedEmail}.
     */
    @Transactional(rollbackFor = Exception.class)
    public UserApplicantAccess createStaffApplicantAccess(Long applicantId, String invitedEmail, Long staffUserId) {
        if (mapper.findActiveOwner(applicantId) != null) {
            throw new GrantException("applicant already has an active owner");
        }
        UserApplicantAccess interim = newGrant(applicantId, AccessRole.OWNER, AccessGrantStatus.ACTIVE);
        interim.setUserId(staffUserId);
        interim.setInterim(true);
        interim.setExpiresAt(LocalDateTime.now().plusDays(INTERIM_OWNER_DAYS));
        interim.setGrantedByUserId(staffUserId);
        interim.setGrantedAt(LocalDateTime.now());
        interim.setCreateBy(String.valueOf(staffUserId));
        mapper.insert(interim);

        UserApplicantAccess invite = newGrant(applicantId, AccessRole.OWNER, AccessGrantStatus.PENDING);
        invite.setInvitedEmail(invitedEmail);
        invite.setGrantedByUserId(staffUserId);
        invite.setGrantedAt(LocalDateTime.now());
        invite.setCreateBy(String.valueOf(staffUserId));
        mapper.insert(invite);
        return interim;
    }

    /**
     * Activate every PENDING invite addressed to {@code email} for this just-authenticated
     * user. An OWNER invite atomically promotes and revokes the interim owner.
     */
    @Transactional(rollbackFor = Exception.class)
    public int acceptInvitesFor(Long userId, String email) {
        if (email == null || email.isBlank()) {
            return 0;
        }
        List<UserApplicantAccess> forUser = mapper.findActiveGrantsForUser(userId);
        int accepted = 0;
        for (UserApplicantAccess pending : mapper.findPendingInvitesByEmail(email)) {
            if (alreadyGranted(forUser, pending.getApplicantId())) {
                continue;
            }
            if (pending.getAccessRole() == AccessRole.OWNER) {
                UserApplicantAccess interim = mapper.lockActiveOwner(pending.getApplicantId());
                if (interim != null && interim.isInterim()) {
                    interim.setStatus(AccessGrantStatus.REVOKED);
                    interim.setRevokedByUserId(userId);
                    interim.setRevokedAt(LocalDateTime.now());
                    interim.setRevokeReason("ownership_transferred");
                    interim.setUpdateBy(String.valueOf(userId));
                    mapper.update(interim);
                }
            }
            pending.setUserId(userId);
            pending.setStatus(AccessGrantStatus.ACTIVE);
            pending.setUpdateBy(String.valueOf(userId));
            mapper.update(pending);
            accepted++;
        }
        return accepted;
    }

    /** All grants (any status) on an applicant, for the access-management screens. */
    public List<UserApplicantAccess> listForApplicant(Long applicantId) {
        return mapper.findByApplicant(applicantId);
    }

    /** ACTIVE OWNER (or staff with {@code nad:applicant:access:manage}) adds a delegate grant. */
    @Transactional(rollbackFor = Exception.class)
    public UserApplicantAccess delegate(Long applicantId, Long targetUserId, String targetEmail,
            AccessRole role, LocalDateTime expiresAt) {
        if (role == AccessRole.OWNER) {
            throw new GrantException("use transferOwnership to change the owner");
        }
        assertCanManageAccess(applicantId);
        boolean pending = targetUserId == null;
        UserApplicantAccess grant = newGrant(applicantId, role,
                pending ? AccessGrantStatus.PENDING : AccessGrantStatus.ACTIVE);
        grant.setUserId(targetUserId);
        grant.setInvitedEmail(pending ? targetEmail : null);
        grant.setExpiresAt(expiresAt);
        grant.setGrantedByUserId(caller.userIdOrNull());
        grant.setGrantedAt(LocalDateTime.now());
        grant.setCreateBy(String.valueOf(caller.userIdOrNull()));
        mapper.insert(grant);
        return grant;
    }

    /** Soft-revoke a non-owner grant. The owner grant only leaves via {@link #transferOwnership}. */
    @Transactional(rollbackFor = Exception.class)
    public void revoke(Long grantId, String reason) {
        UserApplicantAccess grant = mapper.findById(grantId);
        if (grant == null) {
            throw new GrantException("grant not found");
        }
        if (grant.getAccessRole() == AccessRole.OWNER) {
            throw new GrantException("owner access is changed by transfer, not revoke");
        }
        assertCanManageAccess(grant.getApplicantId());
        grant.setStatus(AccessGrantStatus.REVOKED);
        grant.setRevokedByUserId(caller.userIdOrNull());
        grant.setRevokedAt(LocalDateTime.now());
        grant.setRevokeReason(reason);
        grant.setUpdateBy(String.valueOf(caller.userIdOrNull()));
        mapper.update(grant);
    }

    /** Atomic ownership handover to a user who already holds an ACTIVE grant on the applicant. */
    @Transactional(rollbackFor = Exception.class)
    public void transferOwnership(Long applicantId, Long toUserId, AccessRole previousOwnerNewRole) {
        assertCanManageAccess(applicantId);
        UserApplicantAccess currentOwner = mapper.lockActiveOwner(applicantId);
        if (currentOwner == null) {
            throw new GrantException("applicant has no active owner");
        }
        if (toUserId.equals(currentOwner.getUserId())) {
            throw new GrantException("already the owner");
        }
        UserApplicantAccess incoming = mapper.findActiveApplicantGrant(toUserId, applicantId);
        if (incoming == null) {
            throw new GrantException("target user has no active grant on this applicant");
        }

        Long actor = caller.userIdOrNull();
        if (previousOwnerNewRole == null || previousOwnerNewRole == AccessRole.OWNER) {
            currentOwner.setStatus(AccessGrantStatus.REVOKED);
            currentOwner.setRevokedByUserId(actor);
            currentOwner.setRevokedAt(LocalDateTime.now());
            currentOwner.setRevokeReason("ownership_transferred");
        } else {
            currentOwner.setAccessRole(previousOwnerNewRole);
        }
        currentOwner.setUpdateBy(String.valueOf(actor));
        mapper.update(currentOwner);

        incoming.setAccessRole(AccessRole.OWNER);
        incoming.setUpdateBy(String.valueOf(actor));
        mapper.update(incoming);
    }

    private void assertCanManageAccess(Long applicantId) {
        if (caller.isStaff()) {
            if (!rbac.hasPermi("nad:applicant:access:manage")) {
                throw new GrantException("missing nad:applicant:access:manage");
            }
            return;
        }
        UserApplicantAccess owner = mapper.findActiveOwner(applicantId);
        if (owner == null || !owner.getUserId().equals(caller.userIdOrNull())) {
            throw new GrantException("only the active owner may manage access");
        }
    }

    private boolean alreadyGranted(List<UserApplicantAccess> grants, Long applicantId) {
        return grants.stream().anyMatch(g -> g.getApplicantId().equals(applicantId));
    }

    private static UserApplicantAccess newGrant(Long applicantId, AccessRole role, AccessGrantStatus status) {
        UserApplicantAccess g = new UserApplicantAccess();
        g.setApplicantId(applicantId);
        g.setAccessRole(role);
        g.setStatus(status);
        return g;
    }
}
