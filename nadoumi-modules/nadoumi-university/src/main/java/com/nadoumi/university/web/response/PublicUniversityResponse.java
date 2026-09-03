package com.nadoumi.university.web.response;

import com.nadoumi.university.domain.University;
import java.util.List;

/**
 * Public university view — only published, active universities are ever mapped to
 * this. No operational status, no audit, no internal notes.
 */
public record PublicUniversityResponse(
        Long id,
        String name,
        String nameCn,
        String country,
        String type,
        String city,
        String province,
        Short foundedYear,
        Integer totalStudents,
        Integer internationalStudents,
        Integer facultyCount,
        String website,
        String rankingTier,
        String introduction,
        String history,
        String campusInfo,
        String accommodationInfo,
        String nearbyInfo,
        String admissionsEmail,
        String officePhone,
        Long logoDocumentId,
        Long bannerDocumentId,
        boolean recommended,
        boolean featured,
        List<UniversityResponse.Ranking> rankings,
        List<UniversityResponse.Highlight> highlights,
        List<UniversityResponse.GalleryImage> gallery) {

    public static PublicUniversityResponse of(University u) {
        UniversityResponse full = UniversityResponse.of(u);
        return new PublicUniversityResponse(
                full.id(), full.name(), full.nameCn(), full.country(), full.type(),
                full.city(), full.province(), full.foundedYear(),
                full.totalStudents(), full.internationalStudents(), full.facultyCount(),
                full.website(), full.rankingTier(),
                full.introduction(), full.history(), full.campusInfo(),
                full.accommodationInfo(), full.nearbyInfo(),
                full.admissionsEmail(), full.officePhone(),
                full.logoDocumentId(), full.bannerDocumentId(),
                full.recommended(), full.featured(),
                full.rankings(), full.highlights(), full.gallery());
    }
}
