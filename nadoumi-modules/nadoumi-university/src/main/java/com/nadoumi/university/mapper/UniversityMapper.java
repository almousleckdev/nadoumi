package com.nadoumi.university.mapper;

import com.nadoumi.university.domain.University;
import com.nadoumi.university.domain.UniversityGalleryImage;
import com.nadoumi.university.domain.UniversityHighlight;
import com.nadoumi.university.domain.UniversityRanking;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface UniversityMapper {

    University findById(@Param("id") Long id);

    /**
     * PageHelper-paged list. Every {@link UniversitySearch} field is optional; a
     * null field is not applied. The service sets {@code status} /
     * {@code publishStatus} (public reads force ACTIVE + PUBLISHED).
     */
    List<University> search(UniversitySearch filter);

    int insert(University university);

    int update(University university);

    int delete(@Param("id") Long id);

    int updateLogoMediaId(@Param("id") long id, @Param("mediaId") Long mediaId);

    int updateBannerMediaId(@Param("id") long id, @Param("mediaId") Long mediaId);

    Long findIdByNameAndCountry(@Param("name") String name, @Param("country") String country);

    Long findIdBySlug(@Param("slug") String slug);

    // ---- children ----

    List<UniversityRanking> findRankings(@Param("universityId") Long universityId);

    int deleteRankings(@Param("universityId") Long universityId);

    int insertRanking(UniversityRanking ranking);

    List<UniversityHighlight> findHighlights(@Param("universityId") Long universityId);

    int deleteHighlights(@Param("universityId") Long universityId);

    int insertHighlight(UniversityHighlight highlight);

    List<UniversityGalleryImage> findGallery(@Param("universityId") Long universityId);

    int deleteGallery(@Param("universityId") Long universityId);

    int insertGalleryImage(@Param("universityId") Long universityId,
            @Param("img") UniversityGalleryImage image, @Param("sortOrder") int sortOrder);
}
