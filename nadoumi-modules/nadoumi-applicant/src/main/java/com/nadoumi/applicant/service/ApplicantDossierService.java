package com.nadoumi.applicant.service;

import com.nadoumi.applicant.domain.Applicant;
import com.nadoumi.applicant.domain.enums.PersonRole;
import com.nadoumi.applicant.mapper.ApplicantMapper;
import com.nadoumi.applicant.mapper.FamilyMapper;
import com.nadoumi.applicant.mapper.ResidenceMapper;
import com.nadoumi.applicant.mapper.WorkMapper;
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
import com.nadoumi.common.exception.NadNotFoundException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads a whole applicant profile for a document that the system generates on the applicant's behalf (the printed
 * application form). It performs <b>no caller access check</b>: the only callers are trusted server jobs that
 * have already authorised the work, so never expose it through a controller.
 */
@Service
public class ApplicantDossierService {

    private final ApplicantMapper applicants;
    private final ResidenceMapper residences;
    private final WorkMapper work;
    private final InterestService interests;
    private final FamilyMapper family;

    public ApplicantDossierService(ApplicantMapper applicants, ResidenceMapper residences, WorkMapper work,
            InterestService interests, FamilyMapper family) {
        this.applicants = applicants;
        this.residences = residences;
        this.work = work;
        this.interests = interests;
        this.family = family;
    }

    @Transactional(readOnly = true)
    public ApplicantDossier read(long applicantId) {
        Applicant applicant = applicants.findById(applicantId);
        if (applicant == null) {
            throw new NadNotFoundException("applicant not found");
        }
        var residence = residences.find(applicantId);
        return new ApplicantDossier(
                ApplicantResponse.of(applicant, true),
                PassportStatusResponse.of(applicant, LocalDate.now(ZoneOffset.UTC), true),
                residence == null ? null : ResidenceResponse.of(residence),
                interests.load(applicantId).map(InterestResponse::of).orElse(null),
                applicants.findEducation(applicantId).stream().map(EducationResponse::of).toList(),
                work.findByApplicant(applicantId).stream().map(WorkResponse::of).toList(),
                applicants.findTestScores(applicantId).stream().map(TestScoreResponse::of).toList(),
                Optional.ofNullable(family.findPersonal(applicantId)).map(PersonalDetailsResponse::of).orElse(null),
                family.findMembers(applicantId).stream().map(FamilyMemberResponse::of).toList(),
                Optional.ofNullable(family.findPerson(applicantId, PersonRole.SUPPORTER)).map(PersonResponse::of).orElse(null),
                Optional.ofNullable(family.findPerson(applicantId, PersonRole.CHINA_EMERGENCY)).map(PersonResponse::of).orElse(null));
    }
}
