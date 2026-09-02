package com.nadoumi.identity.service.otp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nadoumi.identity.service.mail.EmailMessage;
import com.nadoumi.identity.service.mail.MailSender;
import com.nadoumi.identity.service.mail.MailTemplates;
import com.ruoyi.common.core.redis.RedisCache;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class OtpServiceTest {

    private static final String EMAIL = "a@x.com";
    private static final String CODE = "482913";

    private final RedisCache redis = mock(RedisCache.class);
    private final MailSender mail = mock(MailSender.class);
    private final TicketService tickets = mock(TicketService.class);
    private final OtpService otp = new OtpService(
            redis, mail, new MailTemplates(), tickets, "https://web.local/login", () -> CODE);

    private static String otpKey(OtpPurpose purpose) {
        return "nad:otp:" + purpose.name() + ":" + OtpService.sha256(EMAIL);
    }

    private static String cooldownKey(OtpPurpose purpose) {
        return "nad:otp:cooldown:" + purpose.name() + ":" + OtpService.sha256(EMAIL);
    }

    @Test
    void issue_stores_the_hashed_code_with_ttl_and_sends_mail_when_not_cooling_down() {
        when(redis.hasKey(cooldownKey(OtpPurpose.REGISTER))).thenReturn(false);

        otp.issue(EMAIL, OtpPurpose.REGISTER);

        verify(redis).setCacheObject(eq(otpKey(OtpPurpose.REGISTER)),
                eq(OtpService.sha256(CODE) + "|0"), eq(OtpService.TTL_SECONDS), eq(TimeUnit.SECONDS));
        verify(redis).setCacheObject(eq(cooldownKey(OtpPurpose.REGISTER)),
                eq("1"), eq(OtpService.COOLDOWN_SECONDS), eq(TimeUnit.SECONDS));
        verify(mail).send(any(EmailMessage.class));
    }

    @Test
    void issue_is_silent_during_the_resend_cooldown() {
        when(redis.hasKey(cooldownKey(OtpPurpose.REGISTER))).thenReturn(true);

        otp.issue(EMAIL, OtpPurpose.REGISTER);

        verify(redis, never()).setCacheObject(eq(otpKey(OtpPurpose.REGISTER)), any(), anyInt(), any());
        verify(mail, never()).send(any());
    }

    @Test
    void verify_returns_a_ticket_on_match_and_burns_the_code() {
        String key = otpKey(OtpPurpose.PASSWORD_RESET);
        when(redis.getCacheObject(key)).thenReturn(OtpService.sha256(CODE) + "|0");
        when(tickets.mint(EMAIL, OtpPurpose.PASSWORD_RESET)).thenReturn("tkt_1");

        assertThat(otp.verify(EMAIL, OtpPurpose.PASSWORD_RESET, CODE)).isEqualTo("tkt_1");
        verify(redis).deleteObject(key);
    }

    @Test
    void verify_throws_and_burns_the_code_on_the_final_wrong_attempt() {
        String key = otpKey(OtpPurpose.REGISTER);
        when(redis.getCacheObject(key)).thenReturn(OtpService.sha256("000000") + "|" + (OtpService.MAX_ATTEMPTS - 1));

        assertThatThrownBy(() -> otp.verify(EMAIL, OtpPurpose.REGISTER, CODE))
                .isInstanceOf(OtpException.class);
        verify(redis).deleteObject(key);
    }

    @Test
    void verify_increments_attempts_on_a_non_final_wrong_attempt() {
        String key = otpKey(OtpPurpose.REGISTER);
        when(redis.getCacheObject(key)).thenReturn(OtpService.sha256("000000") + "|0");

        assertThatThrownBy(() -> otp.verify(EMAIL, OtpPurpose.REGISTER, CODE))
                .isInstanceOf(OtpException.class);
        verify(redis).setCacheObject(eq(key), eq(OtpService.sha256("000000") + "|1"),
                eq(OtpService.TTL_SECONDS), eq(TimeUnit.SECONDS));
    }

    @Test
    void verify_throws_when_the_code_is_absent() {
        when(redis.getCacheObject(any())).thenReturn(null);
        assertThatThrownBy(() -> otp.verify(EMAIL, OtpPurpose.REGISTER, CODE))
                .isInstanceOf(OtpException.class);
    }
}
