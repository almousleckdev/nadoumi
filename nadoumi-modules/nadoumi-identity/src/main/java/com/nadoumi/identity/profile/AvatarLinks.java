package com.nadoumi.identity.profile;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Builds and checks the link to a student's profile photo. The photo itself is private media, so the link is
 * not a media URL: it is {@code /api/public/avatars/{userId}/{signature}}, where the signature is an HMAC of the
 * user id. Only the server can mint one, so the endpoint cannot be used to walk through user ids, and a link is
 * handed out only where the person already appears (a chat, a comment, a liker).
 */
@Component
public class AvatarLinks {

    static final String PATH_PREFIX = "/api/public/avatars/";
    private static final String HMAC = "HmacSHA256";
    private static final int SIGNATURE_CHARS = 22;

    private final byte[] key;

    public AvatarLinks(@Value("${token.secret}") String secret) {
        this.key = ("avatar-links:" + secret).getBytes(StandardCharsets.UTF_8);
    }

    public String linkFor(long userId) {
        return PATH_PREFIX + userId + "/" + sign(userId);
    }

    public boolean isValid(long userId, String signature) {
        if (signature == null) {
            return false;
        }
        return MessageDigest.isEqual(sign(userId).getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8));
    }

    private String sign(long userId) {
        try {
            Mac mac = Mac.getInstance(HMAC);
            mac.init(new SecretKeySpec(key, HMAC));
            byte[] digest = mac.doFinal(Long.toString(userId).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest).substring(0, SIGNATURE_CHARS);
        }
        catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA256 is not available", e);
        }
    }
}
