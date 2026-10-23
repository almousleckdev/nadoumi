package com.ruoyi.web.core.config;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * An opt-in recovery switch for a migration that failed half way. MySQL DDL is not transactional, so a migration that
 * stops part way is recorded as failed and Flyway then refuses to start the application. With
 * {@code NADOUMI_FLYWAY_REPAIR_ON_START=true} the next start first runs Flyway's {@code repair} (it deletes the failed
 * history rows and realigns checksums; it does not touch application tables) and then migrates, so a rerunnable
 * migration can finish. It is off by default and must be switched off again after one successful start, because
 * repairing on every start would hide real migration failures.
 */
@Configuration
public class FlywayRepairConfig {

    private static final Logger log = LoggerFactory.getLogger(FlywayRepairConfig.class);

    @Bean
    @ConditionalOnProperty(name = "nadoumi.flyway.repair-on-start", havingValue = "true")
    FlywayMigrationStrategy repairThenMigrate() {
        return FlywayRepairConfig::repairThenMigrate;
    }

    static void repairThenMigrate(Flyway flyway) {
        log.warn("nadoumi.flyway.repair-on-start is on: repairing the Flyway history before migrating. "
                + "Switch NADOUMI_FLYWAY_REPAIR_ON_START off after this start succeeds.");
        flyway.repair();
        flyway.migrate();
    }
}
