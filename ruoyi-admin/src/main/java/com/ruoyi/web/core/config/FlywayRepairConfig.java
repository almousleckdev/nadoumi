package com.ruoyi.web.core.config;

import java.util.Arrays;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Recovers from a migration that failed half way. MySQL DDL is not transactional, so such a migration is recorded as
 * failed and Flyway then refuses to start. Failed rows are repaired (deleted, checksums realigned; no application table
 * is touched) before migrating, but only when every failed version is listed as rerunnable, i.e. written to be safe to
 * run again. Any other failed migration, or {@code repair-on-start=true} left off and no rerunnable match, still stops
 * the start so a real failure is never hidden. {@code repair-on-start=true} repairs unconditionally for one start.
 */
@Configuration
public class FlywayRepairConfig {

    private static final Logger log = LoggerFactory.getLogger(FlywayRepairConfig.class);

    @Bean
    FlywayMigrationStrategy repairThenMigrate(
            @Value("${nadoumi.flyway.repair-on-start:false}") boolean repairAlways,
            @Value("${nadoumi.flyway.rerunnable-versions:121}") List<String> rerunnableVersions) {
        return flyway -> run(flyway, repairAlways, rerunnableVersions);
    }

    static void run(Flyway flyway, boolean repairAlways, List<String> rerunnableVersions) {
        List<String> failed = Arrays.stream(flyway.info().all())
                .filter(m -> m.getState().isFailed())
                .map(FlywayRepairConfig::version)
                .toList();
        if (!failed.isEmpty() && (repairAlways || rerunnableVersions.containsAll(failed))) {
            log.warn("Repairing the Flyway history for failed migration(s) {} before migrating", failed);
            flyway.repair();
        }
        flyway.migrate();
    }

    private static String version(MigrationInfo info) {
        return info.getVersion() == null ? "repeatable" : info.getVersion().getVersion();
    }
}
