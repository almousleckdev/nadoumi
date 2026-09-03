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
        String slug,
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
        String logoImageUrl,
        String coverImageUrl,
        Long logoMediaId,
        String logoUrl,
        Long bannerMediaId,
        String bannerUrl,
        boolean recommended,
        boolean featured,
        List<UniversityResponse.Ranking> rankings,
        List<UniversityResponse.Highlight> highlights,
        List<UniversityResponse.GalleryImage> gallery) {

    /** Legacy factory — used where no MediaGateway resolution is available (e.g. cross-module tests). */
    public static PublicUniversityResponse of(University u) {
        return of(UniversityResponse.of(u));
    }

    /** Projects an already media-resolved {@link UniversityResponse} down to the public shape. */
    public static PublicUniversityResponse of(UniversityResponse full) {
        return new PublicUniversityResponse(
                full.id(), full.name(), full.nameCn(), full.slug(), full.country(), full.type(),
                full.city(), full.province(), full.foundedYear(),
                full.totalStudents(), full.internationalStudents(), full.facultyCount(),
                full.website(), full.rankingTier(),
                full.introduction(), full.history(), full.campusInfo(),
                full.accommodationInfo(), full.nearbyInfo(),
                full.admissionsEmail(), full.officePhone(),
                full.logoDocumentId(), full.bannerDocumentId(),
                full.logoImageUrl(), full.coverImageUrl(),
                full.logoMediaId(), full.logoUrl(), full.bannerMediaId(), full.bannerUrl(),
                full.recommended(), full.featured(),
                full.rankings(), full.highlights(), full.gallery());
    }
}
