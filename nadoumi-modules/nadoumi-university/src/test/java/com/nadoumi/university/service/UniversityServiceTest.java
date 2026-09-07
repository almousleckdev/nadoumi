package com.nadoumi.university.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nadoumi.common.media.MediaGateway;
import com.nadoumi.identity.exception.NadBadRequestException;
import com.nadoumi.identity.exception.NadNotFoundException;
import com.nadoumi.university.domain.University;
import com.nadoumi.university.domain.enums.HighlightKind;
import com.nadoumi.university.domain.enums.PublishStatus;
import com.nadoumi.university.domain.enums.UniversityStatus;
import com.nadoumi.university.domain.enums.UniversityType;
import com.nadoumi.university.mapper.UniversityMapper;
import com.nadoumi.university.web.request.UniversityRequest;
import com.nadoumi.university.web.response.UniversityResponse;
import java.util.List;
import org.junit.jupiter.api.Test;

class UniversityServiceTest {

    private final UniversityMapper mapper = mock(UniversityMapper.class);
    private final MediaGateway media = mock(MediaGateway.class);
    private final com.nadoumi.common.outbox.OutboxWriter outbox = mock(com.nadoumi.common.outbox.OutboxWriter.class);
    private final UniversityService service = new UniversityService(mapper, media, outbox);

    private static UniversityRequest req(String name, String country,
            List<UniversityRequest.RankingInput> rankings,
            List<UniversityRequest.HighlightInput> highlights) {
        return new UniversityRequest(
                name, "清华大学", country, UniversityType.PUBLIC, "Beijing", "Beijing",
                (short) 1911, 50_000, 6_000, 3_000, "https://x.edu", "T1",
                "intro", "history", "campus", "accommodation", "nearby",
                "adm@x.edu", "+86 10 0000", null, null, null, null, true, false, true, "PARTNER",
                UniversityStatus.ACTIVE, PublishStatus.DRAFT, "note",
                rankings, highlights, List.of());
    }

    private static UniversityRequest req(String name, String country) {
        return req(name, country, List.of(), List.of());
    }

    private static UniversityRequest reqWithLogoMedia(String name, String country, Long logoMediaId) {
        UniversityRequest base = req(name, country);
        return new UniversityRequest(
                base.name(), base.nameCn(), base.country(), base.type(), base.city(), base.province(),
                base.foundedYear(), base.totalStudents(), base.internationalStudents(), base.facultyCount(),
                base.website(), base.rankingTier(), base.introduction(), base.history(), base.campusInfo(),
                base.accommodationInfo(), base.nearbyInfo(), base.admissionsEmail(), base.officePhone(),
                base.logoImageUrl(), base.coverImageUrl(), logoMediaId, null, base.recommended(),
                base.featured(), base.publicPartner(), base.partnerStatus(), base.status(), base.publishStatus(), base.remark(),
                base.rankings(), base.highlights(), base.gallery());
    }

    @Test
    void create_persists_the_profile_and_replaces_children() {
        when(mapper.findIdByNameAndCountry("Tsinghua University", "CN")).thenReturn(null);
        when(mapper.findById(any())).thenReturn(new University());

        service.create(req("  Tsinghua University  ", "cn",
                List.of(new UniversityRequest.RankingInput("QS", 20, (short) 2026, null)),
                List.of(new UniversityRequest.HighlightInput(HighlightKind.HIGHLIGHT, "C9 League"))));

        verify(mapper).insert(any(University.class));
        verify(mapper).deleteRankings(any());
        verify(mapper).insertRanking(any());
        verify(mapper).deleteHighlights(any());
        verify(mapper).insertHighlight(any());
    }

    @Test
    void update_replaces_children_even_when_the_lists_are_empty() {
        University existing = new University();
        existing.setId(7L);
        when(mapper.findById(7L)).thenReturn(existing);
        when(mapper.findIdByNameAndCountry("Fudan University", "CN")).thenReturn(7L);

        service.update(7L, req("Fudan University", "CN"));

        verify(mapper).update(any(University.class));
        verify(mapper).deleteRankings(7L);
        verify(mapper).deleteHighlights(7L);
        verify(mapper, never()).insertRanking(any());
    }

    @Test
    void create_rejects_a_duplicate_name_in_the_same_country() {
        when(mapper.findIdByNameAndCountry("Peking University", "CN")).thenReturn(99L);

        assertThatThrownBy(() -> service.create(req("Peking University", "CN")))
                .isInstanceOf(NadBadRequestException.class);
        verify(mapper, never()).insert(any());
    }

    @Test
    void get_and_delete_throw_when_the_row_is_missing() {
        when(mapper.findById(1L)).thenReturn(null);
        when(mapper.delete(1L)).thenReturn(0);

        assertThatThrownBy(() -> service.get(1L)).isInstanceOf(NadNotFoundException.class);
        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(NadNotFoundException.class);
    }

    @Test
    void publicGet_hides_a_draft_or_inactive_university() {
        University draft = new University();
        draft.setId(3L);
        draft.setStatus(UniversityStatus.ACTIVE);
        draft.setPublishStatus(PublishStatus.DRAFT);
        when(mapper.findById(3L)).thenReturn(draft);
        when(mapper.findRankings(anyLong())).thenReturn(List.of());
        when(mapper.findHighlights(anyLong())).thenReturn(List.of());
        when(mapper.findGallery(anyLong())).thenReturn(List.of());

        assertThatThrownBy(() -> service.publicGet("3")).isInstanceOf(NadNotFoundException.class);

        draft.setPublishStatus(PublishStatus.PUBLISHED);
        draft.setStatus(UniversityStatus.INACTIVE);
        assertThatThrownBy(() -> service.publicGet("3")).isInstanceOf(NadNotFoundException.class);

        draft.setStatus(UniversityStatus.ACTIVE);
        assertThat(service.publicGet("3").id()).isEqualTo(3L);
    }

    @Test
    void get_assembles_the_children() {
        University u = new University();
        u.setId(5L);
        when(mapper.findById(5L)).thenReturn(u);
        service.get(5L);
        verify(mapper).findRankings(5L);
        verify(mapper).findHighlights(5L);
    }

    @Test
    void createResolvesLogoUrlFromMediaId() {
        when(mapper.findIdByNameAndCountry("Zhejiang University", "CN")).thenReturn(null);
        University persisted = new University();
        persisted.setId(1L);
        persisted.setLogoMediaId(7L);
        when(mapper.findById(any())).thenReturn(persisted);
        when(media.publicUrl(7L)).thenReturn("https://res.cloudinary.com/x/logo.png");

        UniversityResponse res = service.create(reqWithLogoMedia("Zhejiang University", "CN", 7L));

        assertThat(res.logoMediaId()).isEqualTo(7L);
        assertThat(res.logoUrl()).isEqualTo("https://res.cloudinary.com/x/logo.png");
    }

    @Test
    void legacyImageUrlStillRenders() {
        University row = new University();
        row.setId(2L);
        row.setLogoImageUrl("/profile/upload/logo.png");
        when(mapper.findById(2L)).thenReturn(row);

        UniversityResponse res = service.get(2L);

        assertThat(res.logoMediaId()).isNull();
        assertThat(res.logoUrl()).isEqualTo("/profile/upload/logo.png");
        verify(media, never()).publicUrl(anyLong());
    }

    @Test
    void galleryCapStillSix() {
        University row = new University();
        row.setId(3L);
        when(mapper.findById(3L)).thenReturn(row);
        when(mapper.findGallery(3L)).thenReturn(List.of(
                gal(1), gal(2), gal(3), gal(4), gal(5), gal(6)));

        assertThatThrownBy(() -> service.uploadGalleryImage(3L, upload()))
                .isInstanceOf(NadBadRequestException.class);
        verify(media, never()).upload(any(), any(), any(), anyLong(), any(), any(), any(), anyLong());
    }

    @Test
    void uploadLogoStoresMediaIdAndReturnsUrl() {
        University row = new University();
        row.setId(4L);
        when(mapper.findById(4L)).thenReturn(row);
        when(media.upload(any(), any(), any(), anyLong(), any(), any(), any(), anyLong()))
                .thenReturn(new com.nadoumi.common.media.MediaUploadResult(42L, "https://res.cloudinary.com/x/l.png"));

        var result = service.uploadLogo(4L, upload());

        assertThat(result.mediaId()).isEqualTo(42L);
        verify(mapper).updateLogoMediaId(4L, 42L);
    }

    private static com.nadoumi.university.domain.UniversityGalleryImage gal(int n) {
        return new com.nadoumi.university.domain.UniversityGalleryImage((long) n, "/g" + n + ".png", null, null);
    }

    private static org.springframework.web.multipart.MultipartFile upload() {
        return new org.springframework.mock.web.MockMultipartFile(
                "file", "logo.png", "image/png", new byte[] { 1, 2, 3 });
    }
}
