package com.nadoumi.university.service;

import com.nadoumi.common.exception.NadBadRequestException;
import com.nadoumi.common.media.MediaGateway;
import com.nadoumi.common.media.MediaUrls;
import com.nadoumi.common.text.Texts;
import com.nadoumi.university.domain.UniversityGalleryImage;
import com.nadoumi.university.domain.UniversityHighlight;
import com.nadoumi.university.domain.UniversityRanking;
import com.nadoumi.university.mapper.UniversityMapper;
import com.nadoumi.university.web.request.UniversityRequest;
import java.util.List;
import org.springframework.stereotype.Component;

/** Replaces a university's rankings, highlights and gallery with the request's, inside the caller's transaction. */
@Component
public class UniversityChildrenWriter {

    private final UniversityMapper mapper;
    private final MediaGateway media;

    public UniversityChildrenWriter(UniversityMapper mapper, MediaGateway media) {
        this.mapper = mapper;
        this.media = media;
    }

    public void replace(Long universityId, List<UniversityRequest.RankingInput> rankings,
            List<UniversityRequest.HighlightInput> highlights, List<UniversityRequest.GalleryInput> gallery) {
        replaceRankings(universityId, rankings);
        replaceHighlights(universityId, highlights);
        replaceGallery(universityId, gallery);
    }

    private void replaceRankings(Long universityId, List<UniversityRequest.RankingInput> rankings) {
        mapper.deleteRankings(universityId);
        if (rankings == null) {
            return;
        }
        for (UniversityRequest.RankingInput in : rankings) {
            UniversityRanking ranking = new UniversityRanking();
            ranking.setUniversityId(universityId);
            ranking.setSource(in.source().trim());
            ranking.setRankPosition(in.rankPosition());
            ranking.setRankYear(in.rankYear() == null ? null : in.rankYear().intValue());
            ranking.setNote(Texts.blankToNull(in.note()));
            mapper.insertRanking(ranking);
        }
    }

    private void replaceHighlights(Long universityId, List<UniversityRequest.HighlightInput> highlights) {
        mapper.deleteHighlights(universityId);
        if (highlights == null) {
            return;
        }
        int order = 0;
        for (UniversityRequest.HighlightInput in : highlights) {
            UniversityHighlight highlight = new UniversityHighlight();
            highlight.setUniversityId(universityId);
            highlight.setKind(in.kind());
            highlight.setSortOrder(order++);
            highlight.setText(in.text().trim());
            mapper.insertHighlight(highlight);
        }
    }

    private void replaceGallery(Long universityId, List<UniversityRequest.GalleryInput> gallery) {
        mapper.deleteGallery(universityId);
        if (gallery == null) {
            return;
        }
        int order = 0;
        for (UniversityRequest.GalleryInput in : gallery) {
            String url = imageUrl(in);
            if (url == null) {
                continue;
            }
            mapper.insertGalleryImage(universityId,
                    new UniversityGalleryImage(null, url, in.mediaId(), Texts.blankToNull(in.caption())), order++);
        }
    }

    /**
     * Uploaded rows carry only a {@code mediaId} and {@code image_url} is NOT NULL, so the URL is
     * resolved from the media asset. Rows with neither (trailing empty form rows) are skipped.
     */
    private String imageUrl(UniversityRequest.GalleryInput in) {
        if (in.imageUrl() != null && !in.imageUrl().isBlank()) {
            return in.imageUrl().trim();
        }
        if (in.mediaId() == null) {
            return null;
        }
        String resolved = MediaUrls.resolve(media, in.mediaId(), null);
        if (resolved == null) {
            throw new NadBadRequestException("gallery image " + in.mediaId() + " is not available");
        }
        return resolved;
    }
}
