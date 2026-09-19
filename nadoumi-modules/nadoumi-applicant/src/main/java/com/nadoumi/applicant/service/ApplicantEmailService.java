package com.nadoumi.applicant.service;

import com.nadoumi.applicant.mapper.ApplicantMapper;
import com.nadoumi.applicant.web.response.ApplicantResponse;
import com.nadoumi.common.access.ApplicantCapability;
import com.nadoumi.common.access.NadoumiAccessService;
import com.nadoumi.identity.service.otp.OtpPurpose;
import com.nadoumi.identity.service.otp.OtpService;
import java.util.Locale;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Changing the applicant's contact email requires proving the new address: a code
 * is mailed to it and only a correct code stores it as the verified email.
 */
@Service
public class ApplicantEmailService {

    private final ApplicantMapper mapper;
    private final NadoumiAccessService access;
    private final OtpService otp;

    public ApplicantEmailService(ApplicantMapper mapper, NadoumiAccessService access, OtpService otp) {
        this.mapper = mapper;
        this.access = access;
        this.otp = otp;
    }

    public OtpService.IssueResult requestCode(Long applicantId, String email) {
        requireEdit(applicantId);
        return otp.issue(normalize(email), OtpPurpose.APPLICANT_EMAIL, false);
    }

    @Transactional(rollbackFor = Exception.class)
    public ApplicantResponse verify(Long applicantId, String email, String code) {
        requireEdit(applicantId);
        String address = normalize(email);
        otp.verify(address, OtpPurpose.APPLICANT_EMAIL, code);
        mapper.markEmailVerified(applicantId, address);
        return ApplicantResponse.of(mapper.findById(applicantId), true);
    }

    private void requireEdit(Long applicantId) {
        if (!access.canAccessApplicant(applicantId, ApplicantCapability.EDIT_PROFILE.name())) {
            throw new AccessDeniedException("missing EDIT_PROFILE on applicant " + applicantId);
        }
    }

    private static String normalize(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
