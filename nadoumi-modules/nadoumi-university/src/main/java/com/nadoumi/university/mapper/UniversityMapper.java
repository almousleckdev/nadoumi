package com.nadoumi.university.mapper;

import com.nadoumi.university.domain.University;
import com.nadoumi.university.domain.UniversityHighlight;
import com.nadoumi.university.domain.UniversityRanking;
import com.nadoumi.university.domain.enums.PublishStatus;
import com.nadoumi.university.domain.enums.UniversityStatus;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface UniversityMapper {

    University findById(@Param("id") Long id);

    /**
     * Staff list, PageHelper-paged. All filters optional. When
     * {@code publishStatus} is set the list is restricted to that visibility
     * (used by the public endpoint together with {@code status = ACTIVE}).
     */
    List<University> search(@Param("q") String q,
            @Param("country") String country,
            @Param("status") UniversityStatus status,
            @Param("publishStatus") PublishStatus publishStatus);

    int insert(University university);

    int update(University university);

    int delete(@Param("id") Long id);

    Long findIdByNameAndCountry(@Param("name") String name, @Param("country") String country);

    // ---- children ----

    List<UniversityRanking> findRankings(@Param("universityId") Long universityId);

    int deleteRankings(@Param("universityId") Long universityId);

    int insertRanking(UniversityRanking ranking);

    List<UniversityHighlight> findHighlights(@Param("universityId") Long universityId);

    int deleteHighlights(@Param("universityId") Long universityId);

    int insertHighlight(UniversityHighlight highlight);
}
