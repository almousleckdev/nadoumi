package com.nadoumi.identity.service.otp;

import com.nadoumi.identity.service.mail.EmailMessage;
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
    private final TicketService tickets;
    private final String loginUrl;
    private final Supplier<String> codeGenerator;

    @Autowired
    public OtpService(RedisCache redis, MailSender mail, MailTemplates templates, TicketService tickets,
            @Value("${nadoumi.web.loginUrl:http://localhost:3000/login}") String loginUrl) {
        this(redis, mail, templates, tickets, loginUrl, OtpService::randomSixDigits);
    }

    OtpService(RedisCache redis, MailSender mail, MailTemplates templates, TicketService tickets,
            String loginUrl, Supplier<String> codeGenerator) {
        this.redis = redis;
        this.mail = mail;
        this.templates = templates;
        this.tickets = tickets;
        this.loginUrl = loginUrl;
        this.codeGenerator = codeGenerator;
    }

    /**
     * Issue and email a code for {@code email}. No-op (no new code, no mail) while a
     * previous request is still within the resend cooldown.
     */
    public void issue(String email, OtpPurpose purpose) {
        if (Boolean.TRUE.equals(redis.hasKey(cooldownKey(purpose, email)))) {
            return;
        }
        String code = codeGenerator.get();
        redis.setCacheObject(otpKey(purpose, email), sha256(code) + "|0", TTL_SECONDS, TimeUnit.SECONDS);
        redis.setCacheObject(cooldownKey(purpose, email), "1", COOLDOWN_SECONDS, TimeUnit.SECONDS);

        String template = purpose == OtpPurpose.REGISTER ? "otp-register" : "otp-password-reset";
        String subject = purpose == OtpPurpose.REGISTER ? "Verify your email" : "Reset your password";
        mail.send(new EmailMessage(email, subject, templates.render(template,
                Map.of("otp", code, "ttlMinutes", Integer.toString(TTL_SECONDS / 60)))));
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

    /** Sent instead of an OTP when a REGISTER request targets an address that already has an account. */
    public void sendAccountExists(String email) {
        mail.send(new EmailMessage(email, "Your Nadoumi account",
                templates.render("account-exists", Map.of("loginUrl", loginUrl))));
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
