package com.nadoumi.applicant.onboarding;

import com.nadoumi.applicant.domain.Applicant;
import com.nadoumi.applicant.domain.ApplicantInterest;
import com.nadoumi.applicant.domain.ApplicantResidence;
import com.nadoumi.applicant.domain.enums.ContactRelation;
import com.nadoumi.applicant.rules.AgeRules;
import com.nadoumi.applicant.rules.PassportRules;
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
    public static final String PHOTO = "PHOTO";
    public static final String PASSPORT = "PASSPORT";
    public static final String EDUCATION = "EDUCATION";
    public static final String INTERESTS = "INTERESTS";
    public static final String LOCATION = "LOCATION";
    public static final String CONTACT = "CONTACT";

    private OnboardingRules() {
    }

    public static List<OnboardingSection> evaluate(OnboardingData data, LocalDate today) {
        Applicant a = data.applicant();
        return List.of(profile(a, today), photo(a), passport(a, today), education(data), interests(data),
                location(data, today), contact(data));
    }

    private static OnboardingSection education(OnboardingData data) {
        return OnboardingSection.of(EDUCATION, data.education().isEmpty() ? List.of("educationRecord") : List.of());
    }

    private static OnboardingSection interests(OnboardingData data) {
        ApplicantInterest interest = data.interest();
        if (interest == null) {
            return OnboardingSection.of(INTERESTS, List.of("interests"));
        }
        List<String> missing = new ArrayList<>();
        if (interest.getFields() == null || interest.getFields().isEmpty()) {
            missing.add("fields");
        }
        if (interest.getCities() == null || interest.getCities().isEmpty()) {
            missing.add("cities");
        }
        return OnboardingSection.of(INTERESTS, missing);
    }

    /** A student in China must still hold a valid visa, so an expiry passing re-opens this section. */
    private static OnboardingSection location(OnboardingData data, LocalDate today) {
        ApplicantResidence residence = data.residence();
        if (residence == null) {
            return OnboardingSection.of(LOCATION, List.of("residence"));
        }
        boolean visaLapsed = residence.isInChina()
                && (residence.getVisaExpiryDate() == null || !residence.getVisaExpiryDate().isAfter(today));
        return OnboardingSection.of(LOCATION, visaLapsed ? List.of("visaExpiry") : List.of());
    }

    /** At least one guardian or emergency contact who can be reached by phone. */
    private static OnboardingSection contact(OnboardingData data) {
        boolean reachable = data.contacts().stream().anyMatch(c ->
                (c.getRelation() == ContactRelation.GUARDIAN || c.getRelation() == ContactRelation.EMERGENCY)
                        && !isBlank(c.getPhone()));
        return OnboardingSection.of(CONTACT, reachable ? List.of() : List.of("guardianOrEmergencyContact"));
    }

    private static OnboardingSection photo(Applicant a) {
        return OnboardingSection.of(PHOTO, a.getPhotoMediaId() == null ? List.of("photo") : List.of());
    }

    /**
     * The passport must be scanned, its details saved, valid for more than six months, and
     * agree with the profile. Editing the profile names or date of birth afterwards
     * re-opens this section, because the comparison is re-run here.
     */
    private static OnboardingSection passport(Applicant a, LocalDate today) {
        List<String> missing = new ArrayList<>();
        if (a.getPassportMediaId() == null) {
            missing.add("passportScan");
        }
        boolean hasDetails = !isBlank(a.getPassportNo()) && a.getPassportIssueDate() != null
                && a.getPassportExpiryDate() != null && a.getPassportGivenName() != null
                && a.getPassportFamilyName() != null && a.getPassportDob() != null;
        if (!hasDetails) {
            missing.add("passportDetails");
            return OnboardingSection.of(PASSPORT, missing);
        }
        if (!PassportRules.isValidForAdmission(a.getPassportExpiryDate(), today)) {
            missing.add("passportExpiry");
        }
        if (!PassportRules.mismatches(a.getGivenName(), a.getFamilyName(), a.getDob(),
                a.getPassportGivenName(), a.getPassportFamilyName(), a.getPassportDob()).isEmpty()) {
            missing.add("passportMatchesProfile");
        }
        return OnboardingSection.of(PASSPORT, missing);
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
