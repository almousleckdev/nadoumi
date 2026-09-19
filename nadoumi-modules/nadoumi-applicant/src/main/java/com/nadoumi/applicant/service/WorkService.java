package com.nadoumi.applicant.service;

import com.nadoumi.applicant.domain.ApplicantWork;
import com.nadoumi.applicant.mapper.WorkMapper;
import com.nadoumi.applicant.rules.ResidenceRules;
import com.nadoumi.applicant.rules.WorkRules;
import com.nadoumi.applicant.web.request.WorkRequest;
import com.nadoumi.applicant.web.response.WorkResponse;
import com.nadoumi.common.access.ApplicantCapability;
import com.nadoumi.common.exception.NadNotFoundException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Work experience: optional, repeatable; work in China also records the work visa. */
@Service
public class WorkService {

    private final WorkMapper mapper;
    private final ApplicantAccessGuard guard;

    public WorkService(WorkMapper mapper, ApplicantAccessGuard guard) {
        this.mapper = mapper;
        this.guard = guard;
    }

    @Transactional(readOnly = true)
    public List<WorkResponse> list(Long applicantId) {
        guard.require(applicantId, ApplicantCapability.VIEW_PROFILE);
        return mapper.findByApplicant(applicantId).stream().map(WorkResponse::of).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public WorkResponse add(Long applicantId, WorkRequest req) {
        guard.require(applicantId, ApplicantCapability.EDIT_PROFILE);
        ApplicantWork work = new ApplicantWork();
        work.setApplicantId(applicantId);
        apply(work, req);
        mapper.insert(work);
        return WorkResponse.of(work);
    }

    @Transactional(rollbackFor = Exception.class)
    public WorkResponse update(Long applicantId, Long workId, WorkRequest req) {
        guard.require(applicantId, ApplicantCapability.EDIT_PROFILE);
        ApplicantWork work = mapper.findById(workId);
        if (work == null || !work.getApplicantId().equals(applicantId)) {
            throw new NadNotFoundException("work experience not found");
        }
        apply(work, req);
        mapper.update(work);
        return WorkResponse.of(work);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long applicantId, Long workId) {
        guard.require(applicantId, ApplicantCapability.EDIT_PROFILE);
        if (mapper.delete(workId, applicantId) == 0) {
            throw new NadNotFoundException("work experience not found");
        }
    }

    private static void apply(ApplicantWork work, WorkRequest req) {
        String country = req.country().toUpperCase(Locale.ROOT);
        boolean current = Boolean.TRUE.equals(req.current());
        WorkRules.validate(req.startDate(), req.endDate(), current, country, req.workVisaType(),
                LocalDate.now(ZoneOffset.UTC));
        work.setEmployer(req.employer().strip());
        work.setJobTitle(req.jobTitle().strip());
        work.setEmploymentType(req.employmentType());
        work.setCountry(country);
        work.setCity(req.city());
        work.setStartDate(req.startDate());
        work.setEndDate(req.endDate());
        work.setCurrent(current);
        work.setDescription(req.description());
        boolean inChina = ResidenceRules.CHINA.equals(country);
        work.setWorkVisaType(inChina ? req.workVisaType() : null);
        work.setWorkVisaExpiry(inChina ? req.workVisaExpiry() : null);
    }
}
