package com.ruoyi.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Runs the real {@code db/migration} chain (V1 baseline + the Nadoumi V2..V4
 * migrations) against a throwaway MySQL, both on a fresh database and on one that
 * already carries the RuoYi schema. Auto-skips without Docker so {@code mvn package}
 * still passes locally; CI has a daemon.
 */
@Testcontainers(disabledWithoutDocker = true)
class FlywayMigrationsIT {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withUsername("root")
            .withPassword("test");

    // Failsafe runs after the boot repackage, so "classpath:db/migration" can be
    // shadowed by the fat jar. Point Flyway straight at the source tree.
    private static final String MIGRATIONS = "filesystem:"
            + Paths.get("src/main/resources/db/migration").toAbsolutePath().normalize();

    private DataSource freshSchema(String name) throws Exception {
        try (Connection c = java.sql.DriverManager.getConnection(
                MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
             Statement s = c.createStatement()) {
            s.execute("DROP DATABASE IF EXISTS `" + name + "`");
            s.execute("CREATE DATABASE `" + name + "` DEFAULT CHARACTER SET utf8mb4");
        }
        var ds = new com.mysql.cj.jdbc.MysqlDataSource();
        ds.setServerName(MYSQL.getHost());
        ds.setPortNumber(MYSQL.getMappedPort(MySQLContainer.MYSQL_PORT));
        ds.setDatabaseName(name);
        ds.setUser(MYSQL.getUsername());
        ds.setPassword(MYSQL.getPassword());
        ds.setAllowPublicKeyRetrieval(true);
        return ds;
    }

    /** Mirrors {@code spring.flyway.*} in application.yml. */
    private FluentConfiguration flyway(DataSource ds) {
        return Flyway.configure()
                .dataSource(ds)
                .locations(MIGRATIONS)
                .failOnMissingLocations(true)
                .baselineOnMigrate(true)
                .baselineVersion("1")
                .baselineDescription("RuoYi + Quartz baseline schema")
                .validateOnMigrate(true)
                .cleanDisabled(true);
    }

    private boolean tableExists(DataSource ds, String table) throws Exception {
        try (Connection c = ds.getConnection();
             ResultSet rs = c.getMetaData().getTables(c.getCatalog(), null, table, new String[] { "TABLE" })) {
            return rs.next();
        }
    }

    private String single(DataSource ds, String sql) throws Exception {
        try (Connection c = ds.getConnection(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            return rs.next() ? rs.getString(1) : null;
        }
    }

    @Test
    void freshDatabase_appliesTheWholeChain() throws Exception {
        DataSource ds = freshSchema("fresh_db");

        int applied = flyway(ds).load().migrate().migrationsExecuted;

        assertThat(applied).isEqualTo(11);
        assertThat(tableExists(ds, "sys_user")).isTrue();
        assertThat(tableExists(ds, "nad_user_applicant_access")).isTrue();
        assertThat(tableExists(ds, "nad_applicant")).isTrue();
        assertThat(tableExists(ds, "nad_applicant_education")).isTrue();
        assertThat(single(ds, "SELECT COUNT(*) FROM sys_dict_data WHERE dict_type = 'nad_user_type'")).isEqualTo("4");
        assertThat(single(ds, "SELECT COUNT(*) FROM sys_menu WHERE perms LIKE 'nad:applicant:%'")).isEqualTo("10");
        // V9 — university catalog + its menu/permission set
        assertThat(tableExists(ds, "nad_university")).isTrue();
        assertThat(single(ds, "SELECT COUNT(*) FROM sys_menu WHERE perms LIKE 'nad:university:%'")).isEqualTo("6");
        assertThat(single(ds, "SELECT COUNT(*) FROM sys_role_menu rm JOIN sys_menu m ON m.menu_id = rm.menu_id "
                + "WHERE rm.role_id = 3 AND m.perms LIKE 'nad:university:%'")).isEqualTo("6");
        // V10 — university profile depth
        assertThat(single(ds, "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() "
                + "AND table_name = 'nad_university' AND column_name = 'introduction'")).isEqualTo("1");
        assertThat(tableExists(ds, "nad_university_ranking")).isTrue();
        assertThat(tableExists(ds, "nad_university_highlight")).isTrue();
        // V11 — public contact inbox
        assertThat(tableExists(ds, "nad_contact_inquiry")).isTrue();
        assertThat(single(ds, "SELECT extra FROM information_schema.columns WHERE table_schema = DATABASE() "
                + "AND table_name = 'nad_user_applicant_access' AND column_name = 'owner_guard'"))
                .isEqualTo("STORED GENERATED");

        // V5 — RuoYi demo data replaced by the Nadoumi baseline
        assertThat(single(ds, "SELECT user_type FROM sys_user WHERE user_name = 'almousleck'")).isEqualTo("00");
        assertThat(single(ds, "SELECT status FROM sys_user WHERE user_id = 1")).isEqualTo("1");
        assertThat(single(ds, "SELECT COUNT(*) FROM sys_user WHERE user_id = 2")).isEqualTo("0");
        assertThat(single(ds, "SELECT COUNT(*) FROM sys_dept WHERE dept_id BETWEEN 101 AND 109")).isEqualTo("0");
        assertThat(single(ds, "SELECT COUNT(*) FROM sys_job WHERE job_id IN (1,2,3)")).isEqualTo("0");
        assertThat(single(ds, "SELECT COUNT(*) FROM sys_role WHERE role_key = 'nadoumi_super_admin'")).isEqualTo("1");

        // V7 — email-first student identity
        assertThat(single(ds, "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() "
                + "AND table_name = 'sys_user' AND column_name = 'email_verified'")).isEqualTo("1");
        // V8 — the RuoYi initial-password nag is off
        assertThat(single(ds, "SELECT config_value FROM sys_config WHERE config_key = 'sys.account.initPasswordModify'"))
                .isEqualTo("0");
    }

    @Test
    void existingRuoYiDatabase_isBaselinedThenGetsOnlyTheNadoumiMigrations() throws Exception {
        DataSource ds = freshSchema("existing_db");

        // simulate a pre-existing, never-Flyway-managed RuoYi database
        flyway(ds).target(MigrationVersion.fromVersion("1")).load().migrate();
        try (Connection c = ds.getConnection(); Statement s = c.createStatement()) {
            s.execute("DROP TABLE flyway_schema_history");
        }

        int applied = flyway(ds).load().migrate().migrationsExecuted;

        assertThat(applied).isEqualTo(10); // V2..V11
        assertThat(single(ds, "SELECT type FROM flyway_schema_history WHERE version = '1'")).isEqualTo("BASELINE");
        assertThat(tableExists(ds, "nad_applicant")).isTrue();
        assertThat(single(ds, "SELECT COUNT(*) FROM sys_role WHERE role_key IN ('ops_manager','case_officer')"))
                .isEqualTo("2");
        assertThat(single(ds, "SELECT COUNT(*) FROM sys_user WHERE user_name = 'almousleck'")).isEqualTo("1");
    }

    @Test
    void ownerGuard_rejectsASecondActiveOwnerForTheSameApplicant() throws Exception {
        DataSource ds = freshSchema("guard_db");
        flyway(ds).load().migrate();

        try (Connection c = ds.getConnection(); Statement s = c.createStatement()) {
            s.execute("INSERT INTO nad_applicant (given_name, family_name) VALUES ('Test', 'Owner')");
            long applicantId;
            try (ResultSet rs = s.executeQuery("SELECT LAST_INSERT_ID()")) {
                rs.next();
                applicantId = rs.getLong(1);
            }
            s.execute("INSERT INTO nad_user_applicant_access (user_id, applicant_id, access_role, status) "
                    + "VALUES (1, " + applicantId + ", 'OWNER', 'ACTIVE')");

            assertThatThrownBy(() -> s.execute(
                    "INSERT INTO nad_user_applicant_access (user_id, applicant_id, access_role, status) "
                            + "VALUES (2, " + applicantId + ", 'OWNER', 'ACTIVE')"))
                    .isInstanceOf(SQLException.class)
                    .satisfies(e -> assertThat(e).isInstanceOfAny(
                            SQLIntegrityConstraintViolationException.class, SQLException.class));
        }
    }
}
