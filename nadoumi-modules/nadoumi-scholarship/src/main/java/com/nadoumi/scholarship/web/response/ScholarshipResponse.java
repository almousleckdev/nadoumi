package com.nadoumi.scholarship.web.response;

import com.nadoumi.common.text.Texts;
import com.nadoumi.scholarship.domain.Scholarship;

/**
 * Staff view of a scholarship: the full student-safe payload ({@code view}) plus
 * the operational fields. Still no confidential linkage — that is
 * {@link ScholarshipInternalResponse}, behind {@code nad:scholarship:internal:*}.
 */
public record ScholarshipResponse(
        PublicScholarshipResponse view,
        String status,
        String publishStatus,
        String publishedAt,
        String remark,
        String createdAt,
        String updatedAt) {

    public static ScholarshipResponse of(Scholarship s) {
        return of(s, s.getHeroImageUrl(), s.getCoverImageUrl());
    }

    public static ScholarshipResponse of(Scholarship s, String heroUrl, String coverUrl) {
        return new ScholarshipResponse(
                PublicScholarshipResponse.detail(s, heroUrl, coverUrl),
                s.getStatus() == null ? null : s.getStatus().name(),
                s.getPublishStatus() == null ? null : s.getPublishStatus().name(),
                Texts.stringOrNull(s.getPublishedAt()), s.getRemark(),
                Texts.stringOrNull(s.getCreateTime()), Texts.stringOrNull(s.getUpdateTime()));
    }
}
