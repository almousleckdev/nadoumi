package com.ruoyi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;

/**
 * Application entry point. RuoYi provides the infrastructure; Nadoumi business
 * modules live under {@code com.nadoumi}.
 */
@SpringBootApplication(scanBasePackages = { "com.ruoyi", "com.nadoumi" }, exclude = { DataSourceAutoConfiguration.class })
public class RuoYiApplication {
    public static void main(String[] args) {
        SpringApplication.run(RuoYiApplication.class, args);
    }
}
