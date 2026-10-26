package com.nadoumi.application.mapper;

import com.nadoumi.application.domain.ApplicationLetter;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface ApplicationLetterMapper {

    /** The current letter of each kind: the newest row per kind. */
    List<ApplicationLetter> findCurrent(@Param("applicationId") Long applicationId);

    ApplicationLetter findCurrentOfKind(@Param("applicationId") Long applicationId, @Param("kind") String kind);

    int insert(ApplicationLetter row);
}
