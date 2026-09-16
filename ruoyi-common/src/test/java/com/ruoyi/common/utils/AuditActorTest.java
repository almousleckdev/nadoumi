package com.ruoyi.common.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * No Spring Security context is set up in a plain unit test, so every call
 * here exercises {@link AuditActor}'s fallback path — the behaviour that
 * matters is that it falls back instead of throwing.
 */
class AuditActorTest
{
    @Test
    void username_fallsBackToSystem_whenThereIsNoAuthenticatedCaller()
    {
        SecurityContextHolder.clearContext();

        assertThat(AuditActor.username()).isEqualTo(AuditActor.SYSTEM_USERNAME);
    }

    @Test
    void userId_fallsBackToZero_whenThereIsNoAuthenticatedCaller()
    {
        SecurityContextHolder.clearContext();

        assertThat(AuditActor.userId()).isEqualTo(AuditActor.SYSTEM_USER_ID);
    }
}
