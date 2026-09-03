package com.nadoumi.program.mapper;

import com.nadoumi.program.domain.enums.ProgramStatus;
import com.nadoumi.program.domain.enums.ProgramTeachingLanguage;
import com.nadoumi.program.domain.enums.ProgramType;
import com.nadoumi.program.domain.enums.PublishStatus;

/**
 * Filter for {@link ProgramMapper#search}. Every field is optional; a null field
 * is not applied. {@code status} / {@code publishStatus} are set by the service
 * (public reads force ACTIVE + PUBLISHED), never by the caller.
 */
public record ProgramSearch(
        String q,
        Long universityId,
        ProgramType type,
        ProgramTeachingLanguage language,
        String field,
        Boolean featured,
        Boolean hot,
        ProgramStatus status,
        PublishStatus publishStatus) {

    public static ProgramSearch staff(String q, Long universityId, ProgramType type,
            ProgramTeachingLanguage language, String field, ProgramStatus status) {
        return new ProgramSearch(q, universityId, type, language, field, null, null, status, null);
    }

    public static ProgramSearch publicCatalog(String q, Long universityId, ProgramType type,
            ProgramTeachingLanguage language, String field, Boolean featured, Boolean hot) {
        return new ProgramSearch(q, universityId, type, language, field, featured, hot,
                ProgramStatus.ACTIVE, PublishStatus.PUBLISHED);
    }
}
