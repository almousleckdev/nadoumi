package com.nadoumi.university.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nadoumi.common.exception.NadBadRequestException;
import com.nadoumi.common.media.MediaGateway;
import com.nadoumi.university.domain.UniversityGalleryImage;
import com.nadoumi.university.domain.UniversityHighlight;
import com.nadoumi.university.domain.UniversityRanking;
import com.nadoumi.university.domain.enums.HighlightKind;
import com.nadoumi.university.mapper.UniversityMapper;
import com.nadoumi.university.web.request.UniversityRequest.GalleryInput;
import com.nadoumi.university.web.request.UniversityRequest.HighlightInput;
import com.nadoumi.university.web.request.UniversityRequest.RankingInput;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class UniversityChildrenWriterTest {

    private static final long ID = 8L;

    private final UniversityMapper mapper = mock(UniversityMapper.class);
    private final MediaGateway media = mock(MediaGateway.class);
    private final UniversityChildrenWriter writer = new UniversityChildrenWriter(mapper, media);

    private List<UniversityGalleryImage> savedGallery(int expected) {
        ArgumentCaptor<UniversityGalleryImage> image = ArgumentCaptor.forClass(UniversityGalleryImage.class);
        verify(mapper, times(expected)).insertGalleryImage(eq(ID), image.capture(), anyInt());
        return image.getAllValues();
    }

    @Test
    void shouldClearEveryCollection_evenWhenTheRequestOmitsThem() {
        writer.replace(ID, null, null, null);

        verify(mapper).deleteRankings(ID);
        verify(mapper).deleteHighlights(ID);
        verify(mapper).deleteGallery(ID);
        verify(mapper, never()).insertRanking(any());
        verify(mapper, never()).insertHighlight(any());
    }

    @Test
    void shouldTrimRankingsAndBlankOutAnEmptyNote() {
        writer.replace(ID, List.of(new RankingInput(" QS ", 34, (short) 2026, "  ")), null, null);

        ArgumentCaptor<UniversityRanking> saved = ArgumentCaptor.forClass(UniversityRanking.class);
        verify(mapper).insertRanking(saved.capture());
        assertThat(saved.getValue().getSource()).isEqualTo("QS");
        assertThat(saved.getValue().getRankYear()).isEqualTo(2026);
        assertThat(saved.getValue().getNote()).isNull();
        assertThat(saved.getValue().getUniversityId()).isEqualTo(ID);
    }

    @Test
    void shouldKeepTheOrderOfHighlightsAndTrimTheirText() {
        writer.replace(ID, null, List.of(
                new HighlightInput(HighlightKind.HIGHLIGHT, " C9 League "),
                new HighlightInput(HighlightKind.ADVANTAGE, "Strong research")), null);

        ArgumentCaptor<UniversityHighlight> saved = ArgumentCaptor.forClass(UniversityHighlight.class);
        verify(mapper, times(2)).insertHighlight(saved.capture());
        assertThat(saved.getAllValues()).extracting(UniversityHighlight::getText).containsExactly("C9 League", "Strong research");
        assertThat(saved.getAllValues()).extracting(UniversityHighlight::getSortOrder).containsExactly(0, 1);
    }

    @Test
    void shouldSkipGalleryRowsWithNeitherAnImageUrlNorAMediaId() {
        writer.replace(ID, null, null, java.util.Arrays.asList(
                new GalleryInput("   ", null, null),
                new GalleryInput(null, null, "orphan caption"),
                new GalleryInput("https://cdn/a.jpg", null, " Hall ")));

        var saved = savedGallery(1);
        assertThat(saved.get(0).imageUrl()).isEqualTo("https://cdn/a.jpg");
        assertThat(saved.get(0).caption()).isEqualTo("Hall");
    }

    @Test
    void shouldResolveTheUrlFromTheMediaAsset_whenTheRowCarriesOnlyAMediaId() {
        when(media.publicUrl(71L)).thenReturn("https://res.cloudinary.com/x/71.jpg");
        when(media.publicUrl(73L)).thenReturn("https://res.cloudinary.com/x/73.jpg");

        writer.replace(ID, null, null, List.of(new GalleryInput(null, 71L, null), new GalleryInput(null, 73L, "Library")));

        var saved = savedGallery(2);
        assertThat(saved).extracting(UniversityGalleryImage::mediaId).containsExactly(71L, 73L);
        assertThat(saved).extracting(UniversityGalleryImage::imageUrl)
                .containsExactly("https://res.cloudinary.com/x/71.jpg", "https://res.cloudinary.com/x/73.jpg");
    }

    @Test
    void shouldPreferTheGivenUrlOverTheMediaLookup() {
        writer.replace(ID, null, null, List.of(new GalleryInput(" https://cdn/b.jpg ", 5L, null)));

        assertThat(savedGallery(1).get(0).imageUrl()).isEqualTo("https://cdn/b.jpg");
        verify(media, never()).publicUrl(5L);
    }

    @Test
    void shouldRejectAGalleryMediaAssetThatCannotBeResolved() {
        when(media.publicUrl(99L)).thenThrow(new IllegalStateException("not public"));

        assertThatThrownBy(() -> writer.replace(ID, null, null, List.of(new GalleryInput(null, 99L, null))))
                .isInstanceOf(NadBadRequestException.class).hasMessageContaining("99");
    }
}
