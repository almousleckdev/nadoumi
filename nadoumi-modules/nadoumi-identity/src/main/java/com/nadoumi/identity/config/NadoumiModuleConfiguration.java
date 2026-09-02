package com.nadoumi.identity.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/** Registers the {@code com.nadoumi.**.mapper} interfaces alongside RuoYi's own scan. */
@Configuration
@MapperScan("com.nadoumi.**.mapper")
public class NadoumiModuleConfiguration {
}
