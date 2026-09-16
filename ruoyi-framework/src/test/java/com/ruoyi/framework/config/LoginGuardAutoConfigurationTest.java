package com.ruoyi.framework.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.ruoyi.framework.web.service.LoginUsernameGuard;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class LoginGuardAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(LoginGuardAutoConfiguration.class));

    @Test
    void registersTheNoopDefault_whenNoOtherGuardIsPresent() {
        runner.run(context -> assertThat(context).getBean(LoginUsernameGuard.class)
                .isSameAs(LoginUsernameGuard.NOOP));
    }

    @Test
    void backsOff_whenABusinessModuleAlreadyRegisteredAGuard() {
        runner.withUserConfiguration(RealGuardConfig.class)
                .run(context -> assertThat(context).getBean(LoginUsernameGuard.class)
                        .isNotSameAs(LoginUsernameGuard.NOOP));
    }

    @Configuration(proxyBeanMethods = false)
    static class RealGuardConfig {
        @Bean
        LoginUsernameGuard realGuard() {
            return username -> { };
        }
    }
}
