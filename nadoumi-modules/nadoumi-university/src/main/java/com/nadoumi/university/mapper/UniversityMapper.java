package com.nadoumi.university.mapper;

import com.nadoumi.university.domain.University;
import com.nadoumi.university.domain.enums.UniversityStatus;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface UniversityMapper {

    University findById(@Param("id") Long id);

    /** Staff list, PageHelper-paged. All filters optional. */
    List<University> search(@Param("q") String q,
            @Param("country") String country,
            @Param("status") UniversityStatus status);

    int insert(University university);

    int update(University university);

    int delete(@Param("id") Long id);

    /** For the unique-name guard: id of the row with this (name, country), or null. */
    Long findIdByNameAndCountry(@Param("name") String name, @Param("country") String country);
}
