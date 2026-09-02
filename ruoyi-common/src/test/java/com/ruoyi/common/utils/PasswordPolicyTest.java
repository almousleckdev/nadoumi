package com.ruoyi.common.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class PasswordPolicyTest
{
    @Test
    void accepts_a_compliant_password()
    {
        assertThat(PasswordPolicy.violation("Abcdef1!", null)).isEmpty();
    }

    @Test
    void flags_each_rule_in_order()
    {
        assertThat(PasswordPolicy.violation("Ab1!", null)).contains("password.tooShort");
        assertThat(PasswordPolicy.violation("A".repeat(20) + "b1!aaaaaaaaaaaaa", null)).contains("password.tooLong");
        assertThat(PasswordPolicy.violation("abcdef1!", null)).contains("password.needUpper");
        assertThat(PasswordPolicy.violation("ABCDEF1!", null)).contains("password.needLower");
        assertThat(PasswordPolicy.violation("Abcdefg!", null)).contains("password.needDigit");
        assertThat(PasswordPolicy.violation("Abcdefg1", null)).contains("password.needSpecial");
    }

    @Test
    void rejects_reuse_of_the_current_password()
    {
        String encoded = SecurityUtils.encryptPassword("Abcdef1!");
        assertThat(PasswordPolicy.violation("Abcdef1!", encoded)).contains("password.sameAsCurrent");
    }

    @Test
    void a_different_new_password_is_allowed_against_a_current_one()
    {
        String encoded = SecurityUtils.encryptPassword("Abcdef1!");
        assertThat(PasswordPolicy.violation("Zxcvbn2@", encoded)).isEmpty();
    }

    @Test
    void rejects_a_password_that_contains_a_personal_term()
    {
        assertThat(PasswordPolicy.violation("Lovelace1!", null, List.of("Ada", "Lovelace", "ada")))
                .contains("password.noPersonal");
        // short terms (< 3 chars) are ignored
        assertThat(PasswordPolicy.violation("Abcdef1!", null, List.of("Ab"))).isEmpty();
        // a clean password with personal terms supplied still passes
        assertThat(PasswordPolicy.violation("Zxcvbn2@", null, List.of("Ada", "Lovelace"))).isEmpty();
    }
}
