package com.nadoumi.identity.service.otp;

import com.nadoumi.identity.service.mail.EmailContent;
import com.nadoumi.identity.service.mail.EmailLayout;
import com.nadoumi.identity.service.mail.EmailMessage;
import com.nadoumi.identity.service.mail.EmailRender;
import com.nadoumi.identity.service.mail.MailSender;
import com.nadoumi.identity.service.mail.MailTemplates;
import com.ruoyi.common.core.redis.RedisCache;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Six-digit email one-time codes with a TTL, an attempt cap and a resend cooldown,
 * all held in Redis. A successful {@link #verify} burns the code and returns a
 * single-use {@link TicketService} handle. Nothing here reveals whether an email is
 * registered — that decision belongs to the caller.
 *
 * <p>Redis values are compact strings ({@code "<sha256(code)>|<attempts>"}) to avoid
 * any dependence on the cache's object serializer.
 */
@Service
public class OtpService {

    static final int TTL_SECONDS = 600;
    static final int COOLDOWN_SECONDS = 60;
    static final int MAX_ATTEMPTS = 5;

    private final RedisCache redis;
    private final MailSender mail;
    private final MailTemplates templates;
    private final EmailLayout emailLayout;
    private final TicketService tickets;
    private final String loginUrl;
    private final Supplier<String> codeGenerator;

    @Autowired
    public OtpService(RedisCache redis, MailSender mail, MailTemplates templates, EmailLayout emailLayout,
            TicketService tickets,
            @Value("${nadoumi.web.loginUrl:http://localhost:3000/login}") String loginUrl) {
        this(redis, mail, templates, emailLayout, tickets, loginUrl, OtpService::randomSixDigits);
    }

    OtpService(RedisCache redis, MailSender mail, MailTemplates templates, EmailLayout emailLayout,
            TicketService tickets, String loginUrl, Supplier<String> codeGenerator) {
        this.redis = redis;
        this.mail = mail;
        this.templates = templates;
        this.emailLayout = emailLayout;
        this.tickets = tickets;
        this.loginUrl = loginUrl;
        this.codeGenerator = codeGenerator;
    }

    /** Outcome of {@link #issue}: whether a mail went out this call, and when a resend is allowed. */
    public record IssueResult(boolean sent, int retryAfterSeconds) {
    }

    /**
     * Issue an email for {@code email}: a fresh OTP, or — when {@code accountExists}
     * for a {@code REGISTER} request — the "account already exists" notice instead.
     * A single 60 s cooldown covers both branches so the response cannot be used to
     * tell a registered address from an unregistered one.
     *
     * @return {@link IssueResult#sent()} {@code false} with the remaining cooldown
     *         when still throttled (no code, no mail); {@code true} otherwise.
     */
    public IssueResult issue(String email, OtpPurpose purpose, boolean accountExists) {
        long remaining = redis.getExpire(cooldownKey(purpose, email));
        if (remaining > 0) {
            return new IssueResult(false, (int) remaining);
        }
        redis.setCacheObject(cooldownKey(purpose, email), "1", COOLDOWN_SECONDS, TimeUnit.SECONDS);

        try {
            if (accountExists && purpose == OtpPurpose.REGISTER) {
                mail.send(accountExistsMail(email));
            }
            else {
                String code = codeGenerator.get();
                redis.setCacheObject(otpKey(purpose, email), sha256(code) + "|0", TTL_SECONDS, TimeUnit.SECONDS);
                mail.send(otpMail(email, purpose, code));
            }
        }
        catch (RuntimeException e) {
            // The mail never went out. Drop the code and the cooldown so the caller
            // can retry straight away once mail delivery is restored, then let the
            // failure surface (NadApiExceptionHandler maps MailException -> 502).
            redis.deleteObject(otpKey(purpose, email));
            redis.deleteObject(cooldownKey(purpose, email));
            throw e;
        }
        return new IssueResult(true, COOLDOWN_SECONDS);
    }

    /**
     * @return a fresh single-use ticket id on success
     * @throws OtpException on a missing, expired or wrong code (and on the final wrong attempt)
     */
    public String verify(String email, OtpPurpose purpose, String code) {
        String key = otpKey(purpose, email);
        String stored = redis.getCacheObject(key);
        if (stored == null) {
            throw new OtpException("verification code is invalid or expired");
        }
        int separator = stored.lastIndexOf('|');
        String storedHash = stored.substring(0, separator);
        int attempts = Integer.parseInt(stored.substring(separator + 1));

        boolean matches = MessageDigest.isEqual(
                storedHash.getBytes(StandardCharsets.UTF_8),
                sha256(code).getBytes(StandardCharsets.UTF_8));
        if (!matches) {
            int next = attempts + 1;
            if (next >= MAX_ATTEMPTS) {
                redis.deleteObject(key);
            }
            else {
                redis.setCacheObject(key, storedHash + "|" + next, TTL_SECONDS, TimeUnit.SECONDS);
            }
            throw new OtpException("verification code is invalid or expired");
        }
        redis.deleteObject(key);
        return tickets.mint(email, purpose);
    }

    private EmailMessage otpMail(String email, OtpPurpose purpose, String code) {
        boolean register = purpose == OtpPurpose.REGISTER;
        String template = register ? "otp-register" : "otp-password-reset";
        EmailContent content = EmailContent.builder(register ? "Verify your email address" : "Reset your password")
                .preheader(register ? "Your Nadoumi verification code" : "Your Nadoumi password reset code")
                .paragraph(templates.render(template, Map.of()).strip())
                .code(code)
                .paragraph("This code expires in " + (TTL_SECONDS / 60) + " minutes and can be used once.")
                .paragraph(register
                        ? "If you didn't start creating a Nadoumi account, you can ignore this email."
                        : "If you didn't ask to reset your password, you can ignore this email — nothing changes.")
                .build();
        EmailRender r = emailLayout.render(content);
        String subject = register ? "Verify your email — Nadoumi" : "Reset your Nadoumi password";
        return new EmailMessage(email, subject, r.text(), r.html());
    }

    private EmailMessage accountExistsMail(String email) {
        EmailContent content = EmailContent.builder("You already have a Nadoumi account")
                .preheader("An account with this email address already exists")
                .paragraphs(templates.render("account-exists", Map.of()))
                .cta("Sign in", loginUrl)
                .build();
        EmailRender r = emailLayout.render(content);
        return new EmailMessage(email, "Your Nadoumi account", r.text(), r.html());
    }

    private static String otpKey(OtpPurpose purpose, String email) {
        return "nad:otp:" + purpose.name() + ":" + sha256(email.toLowerCase());
    }

    private static String cooldownKey(OtpPurpose purpose, String email) {
        return "nad:otp:cooldown:" + purpose.name() + ":" + sha256(email.toLowerCase());
    }

    private static String randomSixDigits() {
        return String.format("%06d", new SecureRandom().nextInt(1_000_000));
    }

    static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        }
        catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
