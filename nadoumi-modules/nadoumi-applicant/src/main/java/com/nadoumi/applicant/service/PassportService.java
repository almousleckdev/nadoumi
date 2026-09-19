package com.nadoumi.applicant.service;

import com.nadoumi.applicant.domain.Applicant;
import com.nadoumi.applicant.mapper.ApplicantMapper;
import com.nadoumi.applicant.rules.PassportRules;
import com.nadoumi.applicant.web.request.PassportRequest;
import com.nadoumi.applicant.web.response.PassportStatusResponse;
import com.nadoumi.common.access.ApplicantCapability;
import com.nadoumi.common.access.NadoumiAccessService;
import com.nadoumi.common.exception.NadNotFoundException;
import com.nadoumi.common.rules.NameRules;
import com.nadoumi.identity.access.CurrentCaller;
import com.ruoyi.framework.web.service.PermissionService;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Locale;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Saves the passport's holder data and reports how it lines up with the profile. A
 * passport that will not stay valid for more than six months is refused outright; a
 * disagreement with the profile is saved (it is evidence) and reported so the student
 * can correct whichever side is wrong. Onboarding stays blocked until they agree.
 */
@Service
public class PassportService {

    private static final String PII_PERMISSION = "nad:applicant:pii:view";

    private final ApplicantMapper mapper;
    private final NadoumiAccessService access;
    private final CurrentCaller caller;
    private final PermissionService rbac;

    public PassportService(ApplicantMapper mapper, NadoumiAccessService access, CurrentCaller caller,
            PermissionService rbac) {
        this.mapper = mapper;
        this.access = access;
        this.caller = caller;
        this.rbac = rbac;
    }

    @Transactional(rollbackFor = Exception.class)
    public PassportStatusResponse save(Long applicantId, PassportRequest req) {
        requireCapability(applicantId, ApplicantCapability.EDIT_PROFILE);
        Applicant applicant = load(applicantId);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        PassportRules.requireAcceptable(req.issueDate(), req.expiryDate(), today);

        applicant.setPassportNo(req.passportNo().toUpperCase(Locale.ROOT));
        applicant.setPassportGivenName(NameRules.normalize(req.givenName()));
        applicant.setPassportFamilyName(NameRules.normalize(req.familyName()));
        applicant.setPassportDob(req.dob());
        applicant.setPassportIssueDate(req.issueDate());
        applicant.setPassportExpiryDate(req.expiryDate());
        applicant.setPassportReadMethod(req.readMethod());
        applicant.setPassportDataEdited(req.edited());
        applicant.setUpdateBy(String.valueOf(caller.requireUserId()));
        mapper.updatePassportData(applicant);
        return PassportStatusResponse.of(applicant, today, includePii());
    }

    @Transactional(readOnly = true)
    public PassportStatusResponse status(Long applicantId) {
        requireCapability(applicantId, ApplicantCapability.VIEW_PROFILE);
        return PassportStatusResponse.of(load(applicantId), LocalDate.now(ZoneOffset.UTC), includePii());
    }

    private Applicant load(Long id) {
        Applicant applicant = mapper.findById(id);
        if (applicant == null) {
            throw new NadNotFoundException("applicant not found");
        }
        return applicant;
    }

    private void requireCapability(Long applicantId, ApplicantCapability capability) {
        if (!access.canAccessApplicant(applicantId, capability.name())) {
            throw new AccessDeniedException("missing " + capability + " on applicant " + applicantId);
        }
    }

    private boolean includePii() {
        return caller.isExternal() || rbac.hasPermi(PII_PERMISSION);
    }
}
