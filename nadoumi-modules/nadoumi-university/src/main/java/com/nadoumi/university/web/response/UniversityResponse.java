package com.nadoumi.university.web.response;

import com.nadoumi.university.domain.University;
import com.nadoumi.university.domain.UniversityGalleryImage;
import com.nadoumi.university.domain.UniversityHighlight;
import com.nadoumi.university.domain.UniversityRanking;
import java.util.List;

/** Full staff-side university view (public catalog fields only — no commercial data). */
public record UniversityResponse(
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
        String status,
        String publishStatus,
        String remark,
        String createdAt,
        String updatedAt,
        List<Ranking> rankings,
        List<Highlight> highlights,
        List<GalleryImage> gallery) {

    public record GalleryImage(Long id, String imageUrl, String caption) {
        static GalleryImage of(UniversityGalleryImage g) {
            return new GalleryImage(g.id(), g.imageUrl(), g.caption());
        }
    }

    public record Ranking(Long id, String source, Integer rankPosition, Short rankYear, String note) {
        static Ranking of(UniversityRanking r) {
            return new Ranking(r.getId(), r.getSource(), r.getRankPosition(),
                    r.getRankYear() == null ? null : r.getRankYear().shortValue(), r.getNote());
        }
    }

    public record Highlight(Long id, String kind, String text) {
        static Highlight of(UniversityHighlight h) {
            return new Highlight(h.getId(), h.getKind() == null ? null : h.getKind().name(), h.getText());
        }
    }

    public static UniversityResponse of(University u) {
        return new UniversityResponse(
                u.getId(), u.getName(), u.getNameCn(), u.getCountry(),
                u.getType() == null ? null : u.getType().name(),
                u.getCity(), u.getProvince(), u.getFoundedYear(),
                u.getTotalStudents(), u.getInternationalStudents(), u.getFacultyCount(),
                u.getWebsite(), u.getRankingTier(),
                u.getIntroduction(), u.getHistory(), u.getCampusInfo(),
                u.getAccommodationInfo(), u.getNearbyInfo(),
                u.getAdmissionsEmail(), u.getOfficePhone(),
                u.getLogoDocumentId(), u.getBannerDocumentId(),
                u.isRecommended(), u.isFeatured(),
                u.getStatus() == null ? null : u.getStatus().name(),
                u.getPublishStatus() == null ? null : u.getPublishStatus().name(),
                u.getRemark(), str(u.getCreateTime()), str(u.getUpdateTime()),
                u.getRankings().stream().map(Ranking::of).toList(),
                u.getHighlights().stream().map(Highlight::of).toList(),
                u.getGallery().stream().map(GalleryImage::of).toList());
    }

    private static String str(Object v) {
        return v == null ? null : v.toString();
    }
}
