package com.nadoumi.identity.service.otp;

import com.ruoyi.common.core.redis.RedisCache;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;

/**
 * An opaque, single-use handle proving that an email address was OTP-verified for a
 * given purpose. Held in Redis with a short TTL; consumed exactly once by
 * {@code register} / {@code password/reset}.
 */
@Service
public class TicketService {

    static final int TTL_SECONDS = 600;

    private static final SecureRandom RNG = new SecureRandom();

    private final RedisCache redis;

    public TicketService(RedisCache redis) {
        this.redis = redis;
    }

    public String mint(String email, OtpPurpose purpose) {
        byte[] raw = new byte[24];
        RNG.nextBytes(raw);
        String id = "tkt_" + HexFormat.of().formatHex(raw);
        redis.setCacheObject(key(purpose, id), email.toLowerCase(), TTL_SECONDS, TimeUnit.SECONDS);
        return id;
    }

    public String consume(String ticketId, OtpPurpose purpose) {
        String storageKey = key(purpose, ticketId);
        String email = redis.getCacheObject(storageKey);
        if (email == null) {
            throw new OtpException("verification ticket is invalid or expired");
        }
        redis.deleteObject(storageKey);
        return email;
    }

    private static String key(OtpPurpose purpose, String ticketId) {
        return "nad:ticket:" + purpose.name() + ":" + ticketId;
    }
}
