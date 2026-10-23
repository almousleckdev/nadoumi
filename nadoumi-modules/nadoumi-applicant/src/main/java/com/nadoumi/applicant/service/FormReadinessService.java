package com.nadoumi.applicant.service;

import com.nadoumi.applicant.domain.ApplicantPersonal;
import com.nadoumi.applicant.domain.enums.PersonRole;
import com.nadoumi.applicant.mapper.FamilyMapper;
import com.nadoumi.applicant.rules.FamilyRules;
import com.nadoumi.applicant.web.response.FormReadinessResponse;
import com.nadoumi.common.access.ApplicantCapability;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Which parts of the Nadoumi application form an applicant still has to fill in. Guidance, not a gate. */
@Service
public class FormReadinessService {

    public static final String PERSONAL_DETAILS = "PERSONAL_DETAILS";
    public static final String FAMILY = "FAMILY";
    public static final String SUPPORTER = "SUPPORTER";
    public static final String CHINA_EMERGENCY = "CHINA_EMERGENCY";
    public static final String SIGNATURE = "SIGNATURE";

    private final FamilyMapper mapper;
    private final ApplicantAccessGuard guard;

    public FormReadinessService(FamilyMapper mapper, ApplicantAccessGuard guard) {
        this.mapper = mapper;
        this.guard = guard;
    }

    @Transactional(readOnly = true)
    public FormReadinessResponse of(Long applicantId) {
        guard.require(applicantId, ApplicantCapability.VIEW_PROFILE);
        List<String> missing = new ArrayList<>();
        if (!personalComplete(mapper.findPersonal(applicantId))) {
            missing.add(PERSONAL_DETAILS);
        }
        if (!FamilyRules.isComplete(mapper.findMembers(applicantId))) {
            missing.add(FAMILY);
        }
        if (mapper.findPerson(applicantId, PersonRole.SUPPORTER) == null) {
            missing.add(SUPPORTER);
        }
        if (mapper.findPerson(applicantId, PersonRole.CHINA_EMERGENCY) == null) {
            missing.add(CHINA_EMERGENCY);
        }
        ApplicantPersonal personal = mapper.findPersonal(applicantId);
        if (personal == null || personal.getSignatureMediaId() == null) {
            missing.add(SIGNATURE);
        }
        return new FormReadinessResponse(List.copyOf(missing));
    }

    static boolean personalComplete(ApplicantPersonal p) {
        return p != null && p.getMaritalStatus() != null && filled(p.getReligion()) && filled(p.getPlaceOfBirth())
                && filled(p.getPermanentAddress());
    }

    private static boolean filled(String value) {
        return value != null && !value.isBlank();
    }
}
