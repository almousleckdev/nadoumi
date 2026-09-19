package com.nadoumi.applicant.onboarding;

import com.nadoumi.applicant.domain.Applicant;
import com.nadoumi.applicant.mapper.ApplicantMapper;
import com.nadoumi.common.access.ApplicantCapability;
import com.nadoumi.common.access.NadoumiAccessService;
import com.nadoumi.common.exception.NadBadRequestException;
import com.nadoumi.common.exception.NadNotFoundException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The server-side record of onboarding. {@code onboarded_at} is written only here,
 * and only when every section is satisfied, so the dashboard gate never depends on
 * what the browser claims.
 */
@Service
public class OnboardingService {

    private final ApplicantMapper mapper;
    private final NadoumiAccessService access;

    public OnboardingService(ApplicantMapper mapper, NadoumiAccessService access) {
        this.mapper = mapper;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public OnboardingStatus status(Long applicantId) {
        requireCapability(applicantId, ApplicantCapability.VIEW_PROFILE);
        return statusOf(load(applicantId));
    }

    @Transactional(rollbackFor = Exception.class)
    public OnboardingStatus complete(Long applicantId) {
        requireCapability(applicantId, ApplicantCapability.EDIT_PROFILE);
        Applicant applicant = load(applicantId);
        if (applicant.getOnboardedAt() != null) {
            return statusOf(applicant);
        }
        OnboardingStatus status = statusOf(applicant);
        if (!status.ready()) {
            throw new NadBadRequestException("onboarding is incomplete: " + incompleteKeys(status));
        }
        mapper.markOnboarded(applicantId);
        return statusOf(load(applicantId));
    }

    private static OnboardingStatus statusOf(Applicant applicant) {
        List<OnboardingSection> sections = OnboardingRules.evaluate(applicant, LocalDate.now(ZoneOffset.UTC));
        boolean ready = sections.stream().allMatch(OnboardingSection::complete);
        return new OnboardingStatus(applicant.getOnboardedAt() != null, ready, sections);
    }

    private static String incompleteKeys(OnboardingStatus status) {
        return String.join(", ", status.sections().stream()
                .filter(section -> !section.complete()).map(OnboardingSection::key).toList());
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
}
