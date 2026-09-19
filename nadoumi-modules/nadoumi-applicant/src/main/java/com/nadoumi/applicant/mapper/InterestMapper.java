package com.nadoumi.applicant.mapper;

import com.nadoumi.applicant.domain.ApplicantInterest;
import com.nadoumi.applicant.domain.enums.InterestKind;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface InterestMapper {

    /** The interest row without its choices; null when the applicant has none yet. */
    ApplicantInterest find(@Param("applicantId") long applicantId);

    int insert(ApplicantInterest interest);

    int update(ApplicantInterest interest);

    List<String> findChoices(@Param("applicantId") long applicantId, @Param("kind") InterestKind kind);

    int deleteChoices(@Param("applicantId") long applicantId);

    int insertChoices(@Param("applicantId") long applicantId, @Param("kind") InterestKind kind,
            @Param("values") Collection<String> values);
}
