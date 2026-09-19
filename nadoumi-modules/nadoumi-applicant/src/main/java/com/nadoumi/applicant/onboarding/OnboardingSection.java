package com.nadoumi.applicant.onboarding;

import java.util.List;

/** One onboarding section: whether it is complete and which required fields are still missing. */
public record OnboardingSection(String key, boolean complete, List<String> missing) {

    public static OnboardingSection of(String key, List<String> missing) {
        return new OnboardingSection(key, missing.isEmpty(), List.copyOf(missing));
    }
}
