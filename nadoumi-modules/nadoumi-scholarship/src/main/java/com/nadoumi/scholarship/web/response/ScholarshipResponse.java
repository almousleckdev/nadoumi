package com.nadoumi.scholarship.web.response;

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
        return new ScholarshipResponse(
                PublicScholarshipResponse.detail(s),
                s.getStatus() == null ? null : s.getStatus().name(),
                s.getPublishStatus() == null ? null : s.getPublishStatus().name(),
                str(s.getPublishedAt()), s.getRemark(),
                str(s.getCreateTime()), str(s.getUpdateTime()));
    }

    private static String str(Object v) {
        return v == null ? null : v.toString();
    }
}
