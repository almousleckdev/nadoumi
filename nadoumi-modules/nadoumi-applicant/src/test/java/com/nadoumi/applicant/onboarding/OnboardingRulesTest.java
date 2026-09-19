package com.nadoumi.applicant.onboarding;

import static org.assertj.core.api.Assertions.assertThat;

import com.nadoumi.applicant.domain.Applicant;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    private static OnboardingSection section(Applicant a, String key) {
        return OnboardingRules.evaluate(a, TODAY).stream().filter(s -> s.key().equals(key)).findFirst().orElseThrow();
    }

    private static OnboardingSection profile(Applicant a) {
        return OnboardingRules.evaluate(a, TODAY).get(0);
    }

    private static Applicant with(Consumer<Applicant> change) {
        Applicant a = complete();
        change.accept(a);
        return a;
    }

    @Test
    void shouldBeReady_onlyWhenProfilePhotoAndPassportAreAllSatisfied() {
        assertThat(OnboardingRules.evaluate(complete(), TODAY)).allSatisfy(s -> assertThat(s.complete()).isTrue());
        assertThat(OnboardingRules.evaluate(complete(), TODAY)).extracting(OnboardingSection::key)
                .containsExactly("PROFILE", "PHOTO", "PASSPORT");
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
}
