package com.ruoyi.framework.web.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * {@link TokenService#requireNonDefaultSecret} is the startup guard against
 * booting with the publicly-committed dev-default JWT secret outside local
 * development/tests — see {@code docs/SECURITY.md} S1/S2.
 */
class TokenServiceTest {

    private static final String DEV_DEFAULT_SECRET =
            "ZGV2LW9ubHktZG8tbm90LXVzZS1pbi1wcm9kLW5hZG91bWktcnVveWktaHM1MTItamp3dC1zZWNyZXQtMDAxMjM0NQ";

    @Test
    void requireNonDefaultSecret_throws_whenSecretIsDevDefaultAndNotAllowed() {
        assertThatThrownBy(() -> TokenService.requireNonDefaultSecret(DEV_DEFAULT_SECRET, false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("TOKEN_SECRET");
    }

    @Test
    void requireNonDefaultSecret_passes_whenSecretIsDevDefaultAndExplicitlyAllowed() {
        assertThatCode(() -> TokenService.requireNonDefaultSecret(DEV_DEFAULT_SECRET, true))
                .doesNotThrowAnyException();
    }

    @Test
    void requireNonDefaultSecret_passes_whenSecretIsNotTheDevDefault() {
        assertThatCode(() -> TokenService.requireNonDefaultSecret(
                        "a-real-64-plus-byte-secret-value-set-via-token-secret-env-var", false))
                .doesNotThrowAnyException();
    }
}
