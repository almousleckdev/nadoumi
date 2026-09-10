package com.nadoumi.identity.config;

import com.nadoumi.identity.service.mail.BrandProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Registers the {@code com.nadoumi.**.mapper} interfaces alongside RuoYi's own scan. */
@Configuration
@MapperScan("com.nadoumi.**.mapper")
@EnableConfigurationProperties(BrandProperties.class)
public class NadoumiModuleConfiguration {
}
