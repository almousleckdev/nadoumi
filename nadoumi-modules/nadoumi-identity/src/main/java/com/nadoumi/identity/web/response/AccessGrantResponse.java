package com.nadoumi.identity.web.response;

import com.nadoumi.common.access.AccessCapabilityMatrix;
import com.nadoumi.identity.access.CapabilityOverrides;
import com.nadoumi.identity.domain.UserApplicantAccess;
import java.time.LocalDateTime;
import java.util.List;

public record AccessGrantResponse(
        Long id,
        Long userId,
        Long applicantId,
        Long applicationId,
        String accessRole,
        String status,
        String invitedEmail,
        boolean interim,
        LocalDateTime grantedAt,
        LocalDateTime expiresAt,
        LocalDateTime revokedAt,
        String revokeReason,
        List<String> effectiveCapabilities) {

    public static AccessGrantResponse of(UserApplicantAccess g) {
        CapabilityOverrides ov = CapabilityOverrides.parse(g.getCapabilityOverridesJson());
        List<String> caps = AccessCapabilityMatrix
                .effectiveCapabilities(g.getAccessRole(), ov.add(), ov.remove())
                .stream().map(Enum::name).sorted().toList();
        return new AccessGrantResponse(g.getId(), g.getUserId(), g.getApplicantId(), g.getApplicationId(),
                g.getAccessRole().name(), g.getStatus().name(), g.getInvitedEmail(), g.isInterim(),
                g.getGrantedAt(), g.getExpiresAt(), g.getRevokedAt(), g.getRevokeReason(), caps);
    }
}
