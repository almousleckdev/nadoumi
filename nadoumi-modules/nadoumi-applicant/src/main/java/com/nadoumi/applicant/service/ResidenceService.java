package com.nadoumi.applicant.service;

import com.nadoumi.applicant.domain.ApplicantResidence;
import com.nadoumi.applicant.mapper.ResidenceMapper;
import com.nadoumi.applicant.rules.ResidenceRules;
import com.nadoumi.applicant.web.request.ResidenceRequest;
import com.nadoumi.applicant.web.response.ResidenceResponse;
import com.nadoumi.common.access.ApplicantCapability;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Where the applicant is now; a student in China also gives their level, school and visa. */
@Service
public class ResidenceService {

    private final ResidenceMapper mapper;
    private final ApplicantAccessGuard guard;

    public ResidenceService(ResidenceMapper mapper, ApplicantAccessGuard guard) {
        this.mapper = mapper;
        this.guard = guard;
    }

    @Transactional(readOnly = true)
    public Optional<ResidenceResponse> get(Long applicantId) {
        guard.require(applicantId, ApplicantCapability.VIEW_PROFILE);
        return load(applicantId).map(ResidenceResponse::of);
    }

    @Transactional(rollbackFor = Exception.class)
    public ResidenceResponse save(Long applicantId, ResidenceRequest req) {
        guard.require(applicantId, ApplicantCapability.EDIT_PROFILE);
        String country = req.country().toUpperCase(Locale.ROOT);
        boolean inChina = req.inChina();
        ResidenceRules.validate(inChina, country, req.chinaEducationLevel(), req.visaType(),
                req.visaExpiryDate(), LocalDate.now(ZoneOffset.UTC));

        ApplicantResidence residence = new ApplicantResidence();
        residence.setApplicantId(applicantId);
        residence.setInChina(inChina);
        residence.setCountry(country);
        residence.setCity(req.city().strip());
        residence.setAddress(req.address());
        // the China details belong to the "in China" answer only; drop them if the student is elsewhere
        if (inChina) {
            residence.setChinaEducationLevel(req.chinaEducationLevel());
            residence.setChinaSchool(req.chinaSchool());
            residence.setVisaType(req.visaType());
            residence.setVisaExpiryDate(req.visaExpiryDate());
        }
        if (mapper.find(applicantId) == null) {
            mapper.insert(residence);
        }
        else {
            mapper.update(residence);
        }
        return ResidenceResponse.of(residence);
    }

    /** No access check: for callers that have already authorized. */
    public Optional<ApplicantResidence> load(Long applicantId) {
        return Optional.ofNullable(mapper.find(applicantId));
    }
}
