package com.nadoumi.applicant.onboarding;

import com.nadoumi.applicant.domain.Applicant;
import com.nadoumi.applicant.mapper.ApplicantMapper;
import com.nadoumi.common.access.ApplicantCapability;
import com.nadoumi.applicant.service.ApplicantAccessGuard;
import com.nadoumi.applicant.service.InterestService;
import com.nadoumi.applicant.service.ResidenceService;
import com.nadoumi.common.exception.NadBadRequestException;
import com.nadoumi.common.exception.NadNotFoundException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
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
    private final InterestService interests;
    private final ResidenceService residence;
    private final ApplicantAccessGuard guard;

    public OnboardingService(ApplicantMapper mapper, InterestService interests, ResidenceService residence,
            ApplicantAccessGuard guard) {
        this.mapper = mapper;
        this.interests = interests;
        this.residence = residence;
        this.guard = guard;
    }

    @Transactional(readOnly = true)
    public OnboardingStatus status(Long applicantId) {
        guard.require(applicantId, ApplicantCapability.VIEW_PROFILE);
        return statusOf(dataFor(load(applicantId)));
    }

    @Transactional(rollbackFor = Exception.class)
    public OnboardingStatus complete(Long applicantId) {
        guard.require(applicantId, ApplicantCapability.EDIT_PROFILE);
        Applicant applicant = load(applicantId);
        if (applicant.getOnboardedAt() != null) {
            return statusOf(dataFor(applicant));
        }
        OnboardingStatus status = statusOf(dataFor(applicant));
        if (!status.ready()) {
            throw new NadBadRequestException("onboarding is incomplete: " + incompleteKeys(status));
        }
        mapper.markOnboarded(applicantId);
        return statusOf(dataFor(load(applicantId)));
    }

    /** Records that the student has seen the welcome celebration, so it is shown once. */
    @Transactional(rollbackFor = Exception.class)
    public void markWelcomed(Long applicantId) {
        guard.require(applicantId, ApplicantCapability.EDIT_PROFILE);
        mapper.markWelcomed(applicantId);
    }

    private OnboardingData dataFor(Applicant applicant) {
        Long id = applicant.getId();
        return new OnboardingData(applicant, mapper.findEducation(id), interests.load(id).orElse(null),
                residence.load(id).orElse(null), mapper.findContacts(id));
    }

    private static OnboardingStatus statusOf(OnboardingData data) {
        List<OnboardingSection> sections = OnboardingRules.evaluate(data, LocalDate.now(ZoneOffset.UTC));
        boolean ready = sections.stream().allMatch(OnboardingSection::complete);
        return new OnboardingStatus(data.applicant().getOnboardedAt() != null, ready, sections);
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
}
