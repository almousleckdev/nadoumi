package com.ruoyi.web.core.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class FlywayRepairConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner().withUserConfiguration(FlywayRepairConfig.class);

    @Test
    void shouldNotRepairByDefault() {
        runner.run(context -> assertThat(context).doesNotHaveBean(FlywayMigrationStrategy.class));
    }

    @Test
    void shouldNotRepair_whenTheSwitchIsOff() {
        runner.withPropertyValues("nadoumi.flyway.repair-on-start=false")
                .run(context -> assertThat(context).doesNotHaveBean(FlywayMigrationStrategy.class));
    }

    @Test
    void shouldRepairThenMigrate_whenTheSwitchIsOn() {
        runner.withPropertyValues("nadoumi.flyway.repair-on-start=true").run(context -> {
            assertThat(context).hasSingleBean(FlywayMigrationStrategy.class);
            Flyway flyway = mock(Flyway.class);

            context.getBean(FlywayMigrationStrategy.class).migrate(flyway);

            var order = inOrder(flyway);
            order.verify(flyway).repair();
            order.verify(flyway).migrate();
        });
    }
}
