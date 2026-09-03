package com.nadoumi.university.web.request;

import com.nadoumi.university.domain.enums.HighlightKind;
import com.nadoumi.university.domain.enums.PublishStatus;
import com.nadoumi.university.domain.enums.UniversityStatus;
import com.nadoumi.university.domain.enums.UniversityType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Create / update body for {@code /api/staff/universities}. */
public record UniversityRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 200) String nameCn,
        @NotBlank @Pattern(regexp = "[A-Za-z]{2}", message = "country must be an ISO alpha-2 code")
        String country,
        UniversityType type,
        @Size(max = 120) String city,
        @Size(max = 120) String province,
        Short foundedYear,
        @PositiveOrZero Integer totalStudents,
        @PositiveOrZero Integer internationalStudents,
        @PositiveOrZero Integer facultyCount,
        @Size(max = 255) String website,
        @Size(max = 24) String rankingTier,
        @Size(max = 8000) String introduction,
        @Size(max = 8000) String history,
        @Size(max = 4000) String campusInfo,
        @Size(max = 4000) String accommodationInfo,
        @Size(max = 4000) String nearbyInfo,
        @Size(max = 120) String admissionsEmail,
        @Size(max = 32) String officePhone,
        @Size(max = 500) String logoImageUrl,
        @Size(max = 500) String coverImageUrl,
        Boolean recommended,
        Boolean featured,
        @NotNull UniversityStatus status,
        @NotNull PublishStatus publishStatus,
        @Size(max = 500) String remark,
        @Valid List<RankingInput> rankings,
        @Valid List<HighlightInput> highlights,
        @Valid @Size(max = 6, message = "at most 6 gallery images") List<GalleryInput> gallery) {

    public record RankingInput(
            @NotBlank @Size(max = 40) String source,
            @NotNull Integer rankPosition,
            Short rankYear,
            @Size(max = 200) String note) {
    }

    public record HighlightInput(
            @NotNull HighlightKind kind,
            @NotBlank @Size(max = 400) String text) {
    }

    public record GalleryInput(
            @NotBlank @Size(max = 500) String imageUrl,
            @Size(max = 200) String caption) {
    }
}
