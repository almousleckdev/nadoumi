package com.nadoumi.identity.access;

import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.core.redis.RedisCache;
import java.util.Collection;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * Deletes a user's RuoYi Redis sessions ({@code login_tokens:*}). Used after a
 * password reset (revoke every session) or an in-dashboard password change (revoke
 * every session except the caller's). RuoYi's own {@code updatePwd} does neither.
 */
@Component
public class SessionRevoker {

    private final RedisCache redis;

    public SessionRevoker(RedisCache redis) {
        this.redis = redis;
    }

    /**
     * @param userId      whose sessions to end
     * @param exceptToken a session token to keep alive, or {@code null} to end them all
     * @return the number of sessions deleted
     */
    public int revokeAll(Long userId, String exceptToken) {
        // SCAN, not KEYS — KEYS login_tokens:* is O(N) and blocks Redis's single
        // thread, freezing every login / rate-limit check while it runs.
        Collection<String> keys = redis.scanKeys(CacheConstants.LOGIN_TOKEN_KEY + "*");
        if (keys == null) {
            return 0;
        }
        int deleted = 0;
        for (String key : keys) {
            LoginUser session = redis.getCacheObject(key);
            if (session == null || !Objects.equals(session.getUserId(), userId)) {
                continue;
            }
            if (exceptToken != null && exceptToken.equals(session.getToken())) {
                continue;
            }
            redis.deleteObject(key);
            deleted++;
        }
        return deleted;
    }
}
