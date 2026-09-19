package com.nadoumi.applicant.onboarding;

import com.nadoumi.applicant.domain.Applicant;
import com.nadoumi.applicant.domain.ApplicantContact;
import com.nadoumi.applicant.domain.ApplicantEducation;
import com.nadoumi.applicant.domain.ApplicantInterest;
import com.nadoumi.applicant.domain.ApplicantResidence;
import java.util.List;

/** Everything the completion rules look at. {@code interest} and {@code residence} are null until filled in. */
public record OnboardingData(
        Applicant applicant,
        List<ApplicantEducation> education,
        ApplicantInterest interest,
        ApplicantResidence residence,
        List<ApplicantContact> contacts) {
}
