package com.ruoyi.framework.web.service;

/**
 * Extension point for rejecting a username at the {@code /login} endpoint
 * before credentials are checked. RuoYi ships no domain rule of its own — a
 * business module (e.g. Nadoumi's {@code StaffLoginGuard}) registers the real
 * bean; {@link #NOOP} is the default when none does, keeping this module
 * usable standalone.
 */
public interface LoginUsernameGuard {

    LoginUsernameGuard NOOP = username -> { };

    /** Throws if {@code username} must not be allowed to authenticate here. */
    void assertLoginAllowed(String username);
}
