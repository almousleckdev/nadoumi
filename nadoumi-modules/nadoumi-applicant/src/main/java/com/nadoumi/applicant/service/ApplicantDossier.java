package com.nadoumi.applicant.service;

import com.nadoumi.applicant.web.response.ApplicantResponse;
import com.nadoumi.applicant.web.response.EducationResponse;
import com.nadoumi.applicant.web.response.FamilyMemberResponse;
import com.nadoumi.applicant.web.response.InterestResponse;
import com.nadoumi.applicant.web.response.PassportStatusResponse;
import com.nadoumi.applicant.web.response.PersonResponse;
import com.nadoumi.applicant.web.response.PersonalDetailsResponse;
import com.nadoumi.applicant.web.response.ResidenceResponse;
import com.nadoumi.applicant.web.response.TestScoreResponse;
import com.nadoumi.applicant.web.response.WorkResponse;
import java.util.List;

/** Everything on an applicant's profile that a printed application form shows. Null parts were never filled in. */
public record ApplicantDossier(
        ApplicantResponse profile,
        PassportStatusResponse passport,
        ResidenceResponse residence,
        InterestResponse interests,
        List<EducationResponse> education,
        List<WorkResponse> work,
        List<TestScoreResponse> testScores,
        PersonalDetailsResponse personal,
        List<FamilyMemberResponse> family,
        PersonResponse supporter,
        PersonResponse chinaEmergency) {
}
