package com.nadoumi.applicant.mapper;

import com.nadoumi.applicant.domain.ApplicantResidence;
import org.apache.ibatis.annotations.Param;

public interface ResidenceMapper {

    /** Null when the applicant has not said where they are yet. */
    ApplicantResidence find(@Param("applicantId") long applicantId);

    int insert(ApplicantResidence residence);

    int update(ApplicantResidence residence);
}
