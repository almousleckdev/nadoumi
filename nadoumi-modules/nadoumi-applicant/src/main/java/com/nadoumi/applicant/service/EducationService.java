package com.nadoumi.applicant.service;

import com.nadoumi.applicant.domain.ApplicantEducation;
import com.nadoumi.applicant.mapper.ApplicantMapper;
import com.nadoumi.applicant.rules.EducationRules;
import com.nadoumi.applicant.web.request.EducationRequest;
import com.nadoumi.applicant.web.response.EducationResponse;
import com.nadoumi.common.access.ApplicantCapability;
import com.nadoumi.common.exception.NadNotFoundException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Education history: any number of records, from high school to the applicant's current level. */
@Service
public class EducationService {

    private final ApplicantMapper mapper;
    private final ApplicantAccessGuard guard;

    public EducationService(ApplicantMapper mapper, ApplicantAccessGuard guard) {
        this.mapper = mapper;
        this.guard = guard;
    }

    @Transactional(readOnly = true)
    public List<EducationResponse> list(Long applicantId) {
        guard.require(applicantId, ApplicantCapability.VIEW_PROFILE);
        return mapper.findEducation(applicantId).stream().map(EducationResponse::of).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public EducationResponse add(Long applicantId, EducationRequest req) {
        guard.require(applicantId, ApplicantCapability.EDIT_PROFILE);
        ApplicantEducation education = new ApplicantEducation();
        education.setApplicantId(applicantId);
        apply(education, req);
        mapper.insertEducation(education);
        return EducationResponse.of(education);
    }

    @Transactional(rollbackFor = Exception.class)
    public EducationResponse update(Long applicantId, Long educationId, EducationRequest req) {
        guard.require(applicantId, ApplicantCapability.EDIT_PROFILE);
        ApplicantEducation education = mapper.findEducationById(educationId);
        if (education == null || !education.getApplicantId().equals(applicantId)) {
            throw new NadNotFoundException("education not found");
        }
        apply(education, req);
        mapper.updateEducation(education);
        return EducationResponse.of(education);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long applicantId, Long educationId) {
        guard.require(applicantId, ApplicantCapability.EDIT_PROFILE);
        if (mapper.deleteEducation(educationId, applicantId) == 0) {
            throw new NadNotFoundException("education not found");
        }
    }

    private static void apply(ApplicantEducation education, EducationRequest req) {
        boolean current = Boolean.TRUE.equals(req.current());
        EducationRules.validate(req.startDate(), req.endDate(), current, LocalDate.now(ZoneOffset.UTC));
        education.setInstitution(req.institution().strip());
        education.setCountry(req.country().toUpperCase(Locale.ROOT));
        education.setCity(req.city());
        education.setLevel(req.level());
        education.setQualification(req.qualification());
        education.setField(req.field());
        education.setGpa(req.gpa());
        education.setGpaScale(req.gpaScale());
        education.setStartDate(req.startDate());
        education.setEndDate(req.endDate());
        education.setCurrent(current);
    }
}
