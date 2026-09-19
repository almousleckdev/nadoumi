package com.nadoumi.applicant.onboarding;

import java.util.List;

/**
 * {@code complete} is the server's record that the student finished (onboarded_at);
 * {@code ready} means every section is currently satisfied, so Finish may be accepted.
 */
public record OnboardingStatus(boolean complete, boolean ready, List<OnboardingSection> sections) {
}
