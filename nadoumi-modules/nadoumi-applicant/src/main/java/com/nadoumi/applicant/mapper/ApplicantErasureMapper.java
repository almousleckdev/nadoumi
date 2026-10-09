package com.nadoumi.applicant.mapper;

import org.apache.ibatis.annotations.Param;

/** Deletes the rows the applicant module owns for one applicant. Used only by permanent erasure. */
public interface ApplicantErasureMapper {

    int deleteContacts(@Param("applicantId") long applicantId);

    int deleteEducation(@Param("applicantId") long applicantId);

    int deleteInterestChoices(@Param("applicantId") long applicantId);

    int deleteInterests(@Param("applicantId") long applicantId);

    int deleteResidence(@Param("applicantId") long applicantId);

    int deleteTestScores(@Param("applicantId") long applicantId);

    int deleteWork(@Param("applicantId") long applicantId);

    int deleteApplicant(@Param("applicantId") long applicantId);
}
