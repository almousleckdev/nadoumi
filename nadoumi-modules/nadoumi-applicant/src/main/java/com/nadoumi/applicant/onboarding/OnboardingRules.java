package com.nadoumi.applicant.onboarding;

import com.nadoumi.applicant.domain.Applicant;
import com.nadoumi.applicant.rules.AgeRules;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * What a student must have provided before the dashboard opens. Pure, so the same
 * rules serve the Review step and the server-side completion check. New sections
 * are added here as onboarding grows.
 */
public final class OnboardingRules {

    public static final String PROFILE = "PROFILE";

    private OnboardingRules() {
    }

    public static List<OnboardingSection> evaluate(Applicant a, LocalDate today) {
        return List.of(profile(a, today));
    }

    private static OnboardingSection profile(Applicant a, LocalDate today) {
        List<String> missing = new ArrayList<>();
        requireText(missing, "givenName", a.getGivenName());
        requireText(missing, "familyName", a.getFamilyName());
        if (!AgeRules.isEligible(a.getDob(), today)) {
            missing.add("dob");
        }
        requireText(missing, "nationality", a.getNationality());
        requireText(missing, "gender", a.getGender());
        requireText(missing, "countryOfOrigin", a.getCountryOfOrigin());
        requireText(missing, "countryOfResidence", a.getCountryOfResidence());
        requireText(missing, "nativeLanguage", a.getNativeLanguage());
        requireText(missing, "phone", a.getPhone());
        if (isBlank(a.getWechatId()) && isBlank(a.getWhatsapp())) {
            missing.add("wechatOrWhatsapp");
        }
        if (a.getEmailVerifiedAt() == null) {
            missing.add("emailVerified");
        }
        return OnboardingSection.of(PROFILE, missing);
    }

    private static void requireText(List<String> missing, String field, String value) {
        if (isBlank(value)) {
            missing.add(field);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
