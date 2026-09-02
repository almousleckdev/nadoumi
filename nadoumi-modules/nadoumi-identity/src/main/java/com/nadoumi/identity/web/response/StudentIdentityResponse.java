package com.nadoumi.identity.web.response;

import java.util.List;

/** {@code GET /api/student/me}: identity + reachable applicants. No roles / permissions. */
public record StudentIdentityResponse(
        Long userId,
        String username,
        String nickName,
        String email,
        List<AccessibleApplicant> accessibleApplicants) {
}
