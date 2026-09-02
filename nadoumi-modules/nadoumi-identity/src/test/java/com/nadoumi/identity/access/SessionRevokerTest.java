package com.nadoumi.identity.access;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.core.redis.RedisCache;
import java.util.List;
import org.junit.jupiter.api.Test;

class SessionRevokerTest {

    private final RedisCache redis = mock(RedisCache.class);
    private final SessionRevoker revoker = new SessionRevoker(redis);

    private static LoginUser session(long userId, String token) {
        LoginUser user = new LoginUser();
        user.setUserId(userId);
        user.setToken(token);
        return user;
    }

    @Test
    void deletes_every_session_for_the_user_except_the_caller() {
        String a = CacheConstants.LOGIN_TOKEN_KEY + "aaa";
        String b = CacheConstants.LOGIN_TOKEN_KEY + "bbb";
        String c = CacheConstants.LOGIN_TOKEN_KEY + "ccc";
        when(redis.keys(CacheConstants.LOGIN_TOKEN_KEY + "*")).thenReturn(List.of(a, b, c));
        when(redis.getCacheObject(a)).thenReturn(session(7L, "aaa"));
        when(redis.getCacheObject(b)).thenReturn(session(7L, "bbb"));
        when(redis.getCacheObject(c)).thenReturn(session(9L, "ccc"));

        int deleted = revoker.revokeAll(7L, "aaa");

        assertThat(deleted).isEqualTo(1);
        verify(redis).deleteObject(b);
        verify(redis, never()).deleteObject(a);
        verify(redis, never()).deleteObject(c);
    }

    @Test
    void with_a_null_exception_token_it_deletes_all_of_the_users_sessions() {
        String a = CacheConstants.LOGIN_TOKEN_KEY + "aaa";
        String b = CacheConstants.LOGIN_TOKEN_KEY + "bbb";
        when(redis.keys(CacheConstants.LOGIN_TOKEN_KEY + "*")).thenReturn(List.of(a, b));
        when(redis.getCacheObject(a)).thenReturn(session(7L, "aaa"));
        when(redis.getCacheObject(b)).thenReturn(session(7L, "bbb"));

        assertThat(revoker.revokeAll(7L, null)).isEqualTo(2);
    }

    @Test
    void tolerates_no_sessions() {
        when(redis.keys(CacheConstants.LOGIN_TOKEN_KEY + "*")).thenReturn(null);
        assertThat(revoker.revokeAll(7L, null)).isZero();
    }
}
