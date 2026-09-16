package com.ruoyi.framework.config;

import com.ruoyi.framework.web.service.LoginUsernameGuard;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Registers the {@link LoginUsernameGuard#NOOP} default whenever no business
 * module provides a real one. Auto-configuration classes are always
 * processed after component-scanned beans, so this reliably backs off when
 * e.g. Nadoumi's {@code StaffLoginGuard} is on the classpath.
 */
@AutoConfiguration
public class LoginGuardAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(LoginUsernameGuard.class)
    LoginUsernameGuard defaultLoginUsernameGuard() {
        return LoginUsernameGuard.NOOP;
    }
}
