package com.nadoumi.applicant.mapper;

import com.nadoumi.applicant.domain.Applicant;
import com.nadoumi.applicant.domain.ApplicantContact;
import com.nadoumi.applicant.domain.ApplicantEducation;
import com.nadoumi.applicant.domain.ApplicantTestScore;
import com.nadoumi.applicant.domain.enums.ApplicantStatus;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface ApplicantMapper {

    Applicant findById(Long id);

    /** Staff list, PageHelper-paged. All args optional filters. */
    List<Applicant> search(@Param("name") String name,
                           @Param("status") ApplicantStatus status,
                           @Param("nationality") String nationality,
                           @Param("createdAfter") java.time.LocalDateTime createdAfter);

    /** Student list — restricted to the caller's accessible applicant ids. Empty ids -> empty. */
    List<Applicant> findByIds(@Param("ids") Collection<Long> ids);

    int insert(Applicant applicant);

    int update(Applicant applicant);

    int updateStatus(
            @Param("id") Long id,
            @Param("status") ApplicantStatus status,
            @Param("updateBy") String updateBy
    );

    List<ApplicantEducation> findEducation(@Param("applicantId") Long applicantId);
    ApplicantEducation findEducationById(@Param("id") Long id);
    int insertEducation(ApplicantEducation education);
    int updateEducation(ApplicantEducation education);
    int deleteEducation(@Param("id") Long id, @Param("applicantId") Long applicantId);

    List<ApplicantTestScore> findTestScores(@Param("applicantId") Long applicantId);
    ApplicantTestScore findTestScoreById(@Param("id") Long id);
    int insertTestScore(ApplicantTestScore score);
    int updateTestScore(ApplicantTestScore score);
    int deleteTestScore(@Param("id") Long id, @Param("applicantId") Long applicantId);

    List<ApplicantContact> findContacts(@Param("applicantId") Long applicantId);
    ApplicantContact findContactById(@Param("id") Long id);
    int insertContact(ApplicantContact contact);
    int updateContact(ApplicantContact contact);
    int deleteContact(@Param("id") Long id, @Param("applicantId") Long applicantId);
}
