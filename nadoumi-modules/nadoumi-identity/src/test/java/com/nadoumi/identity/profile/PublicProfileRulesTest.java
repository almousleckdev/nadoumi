package com.nadoumi.identity.profile;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PublicProfileRulesTest {

    private static final String STAFF = "00";
    private static final String STUDENT = "10";

    // ---- display name: staff are public authors ----

    @Test
    void shouldShowTheFullNickName_whenTheUserIsStaff() {
        assertThat(PublicProfileRules.displayName(STAFF, "Jane Smith", "jsmith", null)).isEqualTo("Jane Smith");
    }

    @Test
    void shouldFallBackToTheUserName_whenStaffHasNoNickName() {
        assertThat(PublicProfileRules.displayName(STAFF, "  ", "almousleck", null)).isEqualTo("almousleck");
    }

    // ---- display name: students are shown by first name only ----

    @Test
    void shouldShowOnlyTheFirstGivenName_whenTheStudentHasAnApplicantProfile() {
        assertThat(PublicProfileRules.displayName(STUDENT, "Ava Chen", "stu1", "Ava")).isEqualTo("Ava");
        assertThat(PublicProfileRules.displayName(STUDENT, "x", "stu1", "  Mary Anne  ")).isEqualTo("Mary");
    }

    @Test
    void shouldNeverShowASurname_whenTheStudentHasNoApplicantProfile() {
        assertThat(PublicProfileRules.displayName(STUDENT, "Ava Chen", "stu1", null)).isEqualTo("Ava");
        assertThat(PublicProfileRules.displayName(STUDENT, "Ava Chen", "stu1", " ")).isEqualTo("Ava");
    }

    @Test
    void shouldSoftenAnAllCapsPassportName() {
        assertThat(PublicProfileRules.displayName(STUDENT, "x", "stu1", "AVA")).isEqualTo("Ava");
        assertThat(PublicProfileRules.displayName(STUDENT, "AVA CHEN", "stu1", null)).isEqualTo("Ava");
        assertThat(PublicProfileRules.displayName(STUDENT, "x", "stu1", "McDonald")).isEqualTo("McDonald");
    }

    @Test
    void shouldUseTheUserName_whenTheStudentHasNoNameAtAll() {
        assertThat(PublicProfileRules.displayName(STUDENT, null, "stu_testchat", null)).isEqualTo("stu_testchat");
    }

    @Test
    void shouldNotLeakAnEmailAddress_whenTheUserNameLooksLikeOne() {
        assertThat(PublicProfileRules.displayName(STUDENT, "", "ava.chen@example.com", null)).isEqualTo("Student");
    }

    @Test
    void shouldCapTheLengthAndDropControlCharacters() {
        String name = PublicProfileRules.displayName(STUDENT, "x", "stu", "A".repeat(80) + "\u0007");

        assertThat(name).hasSizeLessThanOrEqualTo(PublicProfileRules.MAX_NAME_LENGTH);
        assertThat(name).doesNotContain("\u0007");
    }

    @Test
    void shouldTreatAnUnknownUserTypeAsAStudent_soNothingExtraIsExposed() {
        assertThat(PublicProfileRules.displayName(null, "Ava Chen", "stu", null)).isEqualTo("Ava");
        assertThat(PublicProfileRules.displayName("20", "Ava Chen", "stu", null)).isEqualTo("Ava");
    }

    // ---- avatar: only staff photos, only absolute https urls ----

    @Test
    void shouldReturnTheStaffAvatar_whenItIsAnAbsoluteHttpsUrl() {
        assertThat(PublicProfileRules.avatarUrl(STAFF, "https://res.cloudinary.com/demo/a.jpg"))
                .isEqualTo("https://res.cloudinary.com/demo/a.jpg");
    }

    @Test
    void shouldDropALegacyLocalAvatarPath_whichThePublicSiteCannotReach() {
        assertThat(PublicProfileRules.avatarUrl(STAFF, "/profile/avatar/2026/10/01/a.png")).isNull();
    }

    @Test
    void shouldRefuseNonHttpsAvatarSchemes() {
        assertThat(PublicProfileRules.avatarUrl(STAFF, "http://cdn.example/a.jpg")).isNull();
        assertThat(PublicProfileRules.avatarUrl(STAFF, "javascript:alert(1)")).isNull();
        assertThat(PublicProfileRules.avatarUrl(STAFF, "data:image/png;base64,AAAA")).isNull();
        assertThat(PublicProfileRules.avatarUrl(STAFF, "")).isNull();
        assertThat(PublicProfileRules.avatarUrl(STAFF, null)).isNull();
    }

    @Test
    void shouldNeverPublishAStudentPhoto() {
        assertThat(PublicProfileRules.avatarUrl(STUDENT, "https://res.cloudinary.com/demo/private.jpg")).isNull();
        assertThat(PublicProfileRules.avatarUrl(null, "https://res.cloudinary.com/demo/private.jpg")).isNull();
    }
}
