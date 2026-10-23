package com.nadoumi.payment.config;

import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** The clock payments read; UTC, and replaceable in tests. */
@Configuration
public class PaymentClockConfig {

    @Bean
    @ConditionalOnMissingBean(Clock.class)
    public Clock paymentClock() {
        return Clock.systemUTC();
    }
}
