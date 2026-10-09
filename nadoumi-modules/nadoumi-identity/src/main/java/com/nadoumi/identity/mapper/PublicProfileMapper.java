package com.nadoumi.identity.mapper;

import com.nadoumi.identity.profile.PublicProfileRow;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PublicProfileMapper {

    /** One row per known user, with the given name of their active OWNER applicant (null for staff). */
    List<PublicProfileRow> findByUserIds(@Param("userIds") Collection<Long> userIds);

    /** The photo asset of a student's active OWNER applicant, or null when there is none (or the user is staff). */
    Long findStudentPhotoMediaId(@Param("userId") long userId);

    /** The photo of one applicant, or null. */
    Long findApplicantPhotoMediaId(@Param("applicantId") long applicantId);
}
