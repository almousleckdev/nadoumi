package com.nadoumi.applicant.onboarding;

import static org.assertj.core.api.Assertions.assertThat;

import com.nadoumi.applicant.domain.Applicant;
import com.nadoumi.applicant.domain.ApplicantContact;
import com.nadoumi.applicant.domain.ApplicantEducation;
import com.nadoumi.applicant.domain.ApplicantInterest;
import com.nadoumi.applicant.domain.ApplicantResidence;
import com.nadoumi.applicant.domain.enums.ContactRelation;
import com.nadoumi.applicant.domain.enums.StudyLevel;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class OnboardingRulesTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 19);

    private static Applicant complete() {
        Applicant a = new Applicant();
        a.setGivenName("AHMED");
        a.setFamilyName("HASSAN");
        a.setDob(LocalDate.of(2004, 5, 1));
        a.setNationality("EG");
        a.setGender("MALE");
        a.setCountryOfOrigin("EG");
        a.setCountryOfResidence("CN");
        a.setNativeLanguage("ar");
        a.setPhone("+8613800000000");
        a.setWhatsapp("+8613800000000");
        a.setEmail("a@example.com");
        a.setEmailVerifiedAt(LocalDateTime.now());
        a.setDob(LocalDate.of(2004, 5, 1));
        a.setPhotoMediaId(11L);
        a.setPassportMediaId(12L);
        a.setPassportNo("P1234567");
        a.setPassportGivenName("AHMED");
        a.setPassportFamilyName("HASSAN");
        a.setPassportDob(LocalDate.of(2004, 5, 1));
        a.setPassportIssueDate(TODAY.minusYears(2));
        a.setPassportExpiryDate(TODAY.plusYears(8));
        return a;
    }

    private static OnboardingData data(Applicant applicant) {
        ApplicantEducation education = new ApplicantEducation();
        ApplicantInterest interest = new ApplicantInterest();
        interest.setDesiredLevel(StudyLevel.MASTER);
        interest.setFields(List.of("BUSINESS"));
        interest.setCities(List.of("Beijing"));
        ApplicantResidence residence = new ApplicantResidence();
        residence.setInChina(false);
        ApplicantContact guardian = new ApplicantContact();
        guardian.setRelation(ContactRelation.GUARDIAN);
        guardian.setPhone("+201000000");
        return new OnboardingData(applicant, List.of(education), interest, residence, List.of(guardian));
    }

    private static OnboardingSection section(OnboardingData data, String key) {
        return OnboardingRules.evaluate(data, TODAY).stream().filter(s -> s.key().equals(key)).findFirst().orElseThrow();
    }

    private static OnboardingSection section(Applicant a, String key) {
        return section(data(a), key);
    }

    private static OnboardingSection profile(Applicant a) {
        return section(a, "PROFILE");
    }

    private static Applicant with(Consumer<Applicant> change) {
        Applicant a = complete();
        change.accept(a);
        return a;
    }

    @Test
    void shouldBeReady_onlyWhenProfilePhotoAndPassportAreAllSatisfied() {
        assertThat(OnboardingRules.evaluate(data(complete()), TODAY)).allSatisfy(s -> assertThat(s.complete()).isTrue());
        assertThat(OnboardingRules.evaluate(data(complete()), TODAY)).extracting(OnboardingSection::key)
                .containsExactly("PROFILE", "PHOTO", "PASSPORT", "EDUCATION", "INTERESTS", "LOCATION", "CONTACT");
    }

    @Test
    void shouldRequireAPhoto() {
        assertThat(section(with(a -> a.setPhotoMediaId(null)), "PHOTO").missing()).containsExactly("photo");
    }

    @Test
    void shouldRequireThePassportScanAndDetails() {
        assertThat(section(with(a -> a.setPassportMediaId(null)), "PASSPORT").missing())
                .containsExactly("passportScan");
        assertThat(section(with(a -> { a.setPassportNo(null); a.setPassportExpiryDate(null); }), "PASSPORT").missing())
                .containsExactly("passportDetails");
    }

    @Test
    void shouldRejectAPassportThatExpiresWithinSixMonths() {
        assertThat(section(with(a -> a.setPassportExpiryDate(TODAY.plusMonths(5))), "PASSPORT").missing())
                .containsExactly("passportExpiry");
    }

    @Test
    void shouldReopenThePassportSection_whenTheProfileNameNoLongerMatches() {
        assertThat(section(with(a -> a.setGivenName("AHMAD")), "PASSPORT").missing())
                .containsExactly("passportMatchesProfile");
    }

    @Test
    void shouldReportProfileComplete_whenEveryRequiredFieldIsPresent() {
        OnboardingSection section = profile(complete());

        assertThat(section.key()).isEqualTo("PROFILE");
        assertThat(section.complete()).isTrue();
        assertThat(section.missing()).isEmpty();
    }

    @Test
    void shouldListTheMissingFields() {
        OnboardingSection section = profile(with(a -> {
            a.setGender(null);
            a.setNativeLanguage(" ");
            a.setPhone(null);
        }));

        assertThat(section.complete()).isFalse();
        assertThat(section.missing()).containsExactlyInAnyOrder("gender", "nativeLanguage", "phone");
    }

    @Test
    void shouldRequireVerifiedEmail() {
        OnboardingSection section = profile(with(a -> a.setEmailVerifiedAt(null)));

        assertThat(section.missing()).containsExactly("emailVerified");
    }

    @Test
    void shouldAcceptEitherWechatOrWhatsapp() {
        assertThat(profile(with(a -> { a.setWhatsapp(null); a.setWechatId("wx_ahmed"); })).complete()).isTrue();
        assertThat(profile(with(a -> { a.setWhatsapp(null); a.setWechatId(null); })).missing())
                .containsExactly("wechatOrWhatsapp");
    }

    @Test
    void shouldTreatUnderageOrFutureDobAsMissing() {
        assertThat(profile(with(a -> a.setDob(TODAY.minusYears(16)))).missing()).containsExactly("dob");
        assertThat(profile(with(a -> a.setDob(TODAY.plusDays(1)))).missing()).containsExactly("dob");
        assertThat(profile(with(a -> a.setDob(null))).missing()).containsExactly("dob");
    }

    @Test
    void shouldRequireAnEducationRecord() {
        OnboardingData none = new OnboardingData(complete(), List.of(), data(complete()).interest(),
                data(complete()).residence(), data(complete()).contacts());

        assertThat(section(none, "EDUCATION").missing()).containsExactly("educationRecord");
    }

    @Test
    void shouldRequireInterestsWithFieldsAndCities() {
        OnboardingData d = data(complete());
        ApplicantInterest bare = new ApplicantInterest();
        bare.setDesiredLevel(StudyLevel.MASTER);
        bare.setFields(List.of());
        bare.setCities(List.of());

        assertThat(section(new OnboardingData(d.applicant(), d.education(), null, d.residence(), d.contacts()), "INTERESTS")
                .missing()).containsExactly("interests");
        assertThat(section(new OnboardingData(d.applicant(), d.education(), bare, d.residence(), d.contacts()), "INTERESTS")
                .missing()).containsExactlyInAnyOrder("fields", "cities");
    }

    @Test
    void shouldRequireALocationAndAValidVisaForAStudentInChina() {
        OnboardingData d = data(complete());
        ApplicantResidence inChina = new ApplicantResidence();
        inChina.setInChina(true);
        inChina.setVisaExpiryDate(TODAY.plusMonths(3));
        ApplicantResidence lapsed = new ApplicantResidence();
        lapsed.setInChina(true);
        lapsed.setVisaExpiryDate(TODAY.minusDays(1));

        assertThat(section(new OnboardingData(d.applicant(), d.education(), d.interest(), null, d.contacts()), "LOCATION")
                .missing()).containsExactly("residence");
        assertThat(section(new OnboardingData(d.applicant(), d.education(), d.interest(), inChina, d.contacts()), "LOCATION")
                .complete()).isTrue();
        assertThat(section(new OnboardingData(d.applicant(), d.education(), d.interest(), lapsed, d.contacts()), "LOCATION")
                .missing()).containsExactly("visaExpiry");
    }

    @Test
    void shouldRequireAGuardianOrEmergencyContactWithAPhone() {
        OnboardingData d = data(complete());
        ApplicantContact other = new ApplicantContact();
        other.setRelation(ContactRelation.OTHER);
        other.setPhone("+1");
        ApplicantContact noPhone = new ApplicantContact();
        noPhone.setRelation(ContactRelation.EMERGENCY);

        assertThat(section(new OnboardingData(d.applicant(), d.education(), d.interest(), d.residence(), List.of(other, noPhone)),
                "CONTACT").missing()).containsExactly("guardianOrEmergencyContact");
    }
}
