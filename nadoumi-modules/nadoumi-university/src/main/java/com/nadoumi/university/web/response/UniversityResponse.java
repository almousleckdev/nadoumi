package com.nadoumi.university.web.response;

import com.nadoumi.university.domain.University;

/** University on the wire. Public catalog fields only. */
public record UniversityResponse(
        Long id,
        String name,
        String country,
        String city,
        String website,
        String rankingTier,
        Long logoDocumentId,
        String status,
        String createdAt,
        String updatedAt) {

    public static UniversityResponse of(University u) {
        return new UniversityResponse(
                u.getId(), u.getName(), u.getCountry(), u.getCity(), u.getWebsite(),
                u.getRankingTier(), u.getLogoDocumentId(),
                u.getStatus() == null ? null : u.getStatus().name(),
                str(u.getCreateTime()), str(u.getUpdateTime()));
    }

    private static String str(Object v) {
        return v == null ? null : v.toString();
    }
}
