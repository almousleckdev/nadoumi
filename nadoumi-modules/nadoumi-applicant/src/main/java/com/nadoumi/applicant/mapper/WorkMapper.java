package com.nadoumi.applicant.mapper;

import com.nadoumi.applicant.domain.ApplicantWork;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface WorkMapper {

    List<ApplicantWork> findByApplicant(@Param("applicantId") long applicantId);

    ApplicantWork findById(@Param("id") long id);

    int insert(ApplicantWork work);

    int update(ApplicantWork work);

    int delete(@Param("id") long id, @Param("applicantId") long applicantId);
}
