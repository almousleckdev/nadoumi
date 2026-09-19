package com.nadoumi.applicant.service;

import com.nadoumi.applicant.domain.ApplicantInterest;
import com.nadoumi.applicant.domain.enums.InterestKind;
import com.nadoumi.applicant.mapper.InterestMapper;
import com.nadoumi.applicant.web.request.InterestRequest;
import com.nadoumi.applicant.web.response.InterestResponse;
import com.nadoumi.common.access.ApplicantCapability;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** What the applicant wants to study and where: one record per applicant, replaced as a whole on save. */
@Service
public class InterestService {

    private final InterestMapper mapper;
    private final ApplicantAccessGuard guard;

    public InterestService(InterestMapper mapper, ApplicantAccessGuard guard) {
        this.mapper = mapper;
        this.guard = guard;
    }

    @Transactional(readOnly = true)
    public Optional<InterestResponse> get(Long applicantId) {
        guard.require(applicantId, ApplicantCapability.VIEW_PROFILE);
        return load(applicantId).map(InterestResponse::of);
    }

    @Transactional(rollbackFor = Exception.class)
    public InterestResponse save(Long applicantId, InterestRequest req) {
        guard.require(applicantId, ApplicantCapability.EDIT_PROFILE);
        ApplicantInterest interest = new ApplicantInterest();
        interest.setApplicantId(applicantId);
        interest.setDesiredLevel(req.desiredLevel());
        interest.setScholarshipInterest(req.scholarshipInterest());
        interest.setIntakeYear(req.intakeYear());
        interest.setIntakeTerm(req.intakeTerm());
        interest.setTeachingLanguage(req.teachingLanguage());
        interest.setNotes(req.notes());
        if (mapper.find(applicantId) == null) {
            mapper.insert(interest);
        }
        else {
            mapper.update(interest);
        }
        List<String> fields = distinct(req.fields());
        List<String> cities = distinct(req.cities());
        mapper.deleteChoices(applicantId);
        mapper.insertChoices(applicantId, InterestKind.FIELD, fields);
        mapper.insertChoices(applicantId, InterestKind.CITY, cities);
        interest.setFields(fields);
        interest.setCities(cities);
        return InterestResponse.of(interest);
    }

    /** The interest with its choices, without an access check: for callers that have already authorized. */
    public Optional<ApplicantInterest> load(Long applicantId) {
        ApplicantInterest interest = mapper.find(applicantId);
        if (interest == null) {
            return Optional.empty();
        }
        interest.setFields(mapper.findChoices(applicantId, InterestKind.FIELD));
        interest.setCities(mapper.findChoices(applicantId, InterestKind.CITY));
        return Optional.of(interest);
    }

    private static List<String> distinct(List<String> values) {
        return values.stream().map(String::strip).distinct().toList();
    }
}
