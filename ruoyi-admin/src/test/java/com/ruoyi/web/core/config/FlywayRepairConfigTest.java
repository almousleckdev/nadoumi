package com.ruoyi.web.core.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.flywaydb.core.api.MigrationState;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;

class FlywayRepairConfigTest {

    private static final List<String> RERUNNABLE = List.of("121");

    private static MigrationInfo info(String version, MigrationState state) {
        MigrationInfo info = mock(MigrationInfo.class);
        when(info.getVersion()).thenReturn(MigrationVersion.fromVersion(version));
        when(info.getState()).thenReturn(state);
        return info;
    }

    private static Flyway flywayWith(MigrationInfo... infos) {
        Flyway flyway = mock(Flyway.class);
        MigrationInfoService service = mock(MigrationInfoService.class);
        when(service.all()).thenReturn(infos);
        when(flyway.info()).thenReturn(service);
        return flyway;
    }

    @Test
    void shouldRepairThenMigrate_whenOnlyARerunnableMigrationFailed() {
        Flyway flyway = flywayWith(info("120", MigrationState.SUCCESS), info("121", MigrationState.FAILED));

        FlywayRepairConfig.run(flyway, false, RERUNNABLE);

        var order = inOrder(flyway);
        order.verify(flyway).repair();
        order.verify(flyway).migrate();
    }

    @Test
    void shouldNotRepair_whenANonRerunnableMigrationFailed() {
        Flyway flyway = flywayWith(info("121", MigrationState.FAILED), info("99", MigrationState.FAILED));

        FlywayRepairConfig.run(flyway, false, RERUNNABLE);

        verify(flyway, never()).repair();
        verify(flyway).migrate();
    }

    @Test
    void shouldNotRepair_whenNothingFailed() {
        Flyway flyway = flywayWith(info("121", MigrationState.SUCCESS));

        FlywayRepairConfig.run(flyway, false, RERUNNABLE);

        verify(flyway, never()).repair();
        verify(flyway).migrate();
    }

    @Test
    void shouldRepairAnyFailure_whenTheSwitchIsOn() {
        Flyway flyway = flywayWith(info("99", MigrationState.FAILED));

        FlywayRepairConfig.run(flyway, true, RERUNNABLE);

        verify(flyway).repair();
        verify(flyway).migrate();
    }
}
