package com.nadoumi.university.mapper;

import com.nadoumi.university.domain.enums.PublishStatus;
import com.nadoumi.university.domain.enums.UniversityStatus;
import com.nadoumi.university.domain.enums.UniversityType;

/**
 * Filter for {@link UniversityMapper#search}. Every field is optional; a null
 * field is not applied. {@code status} / {@code publishStatus} are set by the
 * service (public reads force ACTIVE + PUBLISHED), never by the caller.
 */
public record UniversitySearch(
        String q,
        String country,
        String province,
        String city,
        UniversityType type,
        Boolean featured,
        Boolean recommended,
        Boolean publicPartner,
        UniversityStatus status,
        PublishStatus publishStatus) {

    public static UniversitySearch staff(String q, String country, String province, String city,
            UniversityType type, UniversityStatus status) {
        return new UniversitySearch(q, country, province, city, type, null, null, null, status, null);
    }

    public static UniversitySearch publicCatalog(String q, String country, String province, String city,
            UniversityType type, Boolean featured, Boolean recommended, Boolean publicPartner) {
        return new UniversitySearch(q, country, province, city, type, featured, recommended, publicPartner,
                UniversityStatus.ACTIVE, PublishStatus.PUBLISHED);
    }
}
