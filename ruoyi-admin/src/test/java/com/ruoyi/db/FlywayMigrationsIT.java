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

        assertThat(applied).isEqualTo(50);
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
        // V18 — university gallery
        assertThat(tableExists(ds, "nad_university_gallery")).isTrue();
        // V11 — public contact inbox; V14 — split-name + phone + category
        assertThat(tableExists(ds, "nad_contact_inquiry")).isTrue();
        assertThat(single(ds, "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() "
                + "AND table_name = 'nad_contact_inquiry' AND column_name IN ('first_name','phone','category')"))
                .isEqualTo("3");
        assertThat(single(ds, "SELECT extra FROM information_schema.columns WHERE table_schema = DATABASE() "
                + "AND table_name = 'nad_user_applicant_access' AND column_name = 'owner_guard'"))
                .isEqualTo("STORED GENERATED");
        // V16 — scholarship aggregate + the student-safe view + the confidential table
        assertThat(tableExists(ds, "nad_scholarship")).isTrue();
        assertThat(tableExists(ds, "nad_scholarship_internal")).isTrue();
        assertThat(tableExists(ds, "nad_scholarship_fee")).isTrue();
        assertThat(single(ds, "SELECT COUNT(*) FROM information_schema.views WHERE table_schema = DATABASE() "
                + "AND table_name = 'v_scholarship_student'")).isEqualTo("1");
        // the view must not project any confidential column
        assertThat(single(ds, "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() "
                + "AND table_name = 'v_scholarship_student' "
                + "AND column_name IN ('university_id','partnership_id','operational_notes',"
                + "'confidential_terms','commission_model_json','internal_status')")).isEqualTo("0");
        // V17 — category reference + menu/permission seed
        assertThat(single(ds, "SELECT COUNT(*) FROM nad_scholarship_category")).isEqualTo("12");
        assertThat(single(ds, "SELECT COUNT(*) FROM sys_menu WHERE perms LIKE 'nad:scholarship:%'")).isEqualTo("9");
        assertThat(single(ds, "SELECT COUNT(*) FROM sys_role_menu rm JOIN sys_menu m ON m.menu_id = rm.menu_id "
                + "WHERE rm.role_id = 3 AND m.perms LIKE 'nad:scholarship:%'")).isEqualTo("9");
        // V19 — programme catalog; V20 — its menu/permission seed
        assertThat(tableExists(ds, "nad_program")).isTrue();
        assertThat(tableExists(ds, "nad_program_major")).isTrue();
        assertThat(tableExists(ds, "nad_program_intake")).isTrue();
        assertThat(single(ds, "SELECT COUNT(*) FROM sys_menu WHERE perms LIKE 'nad:program:%'")).isEqualTo("5");
        assertThat(single(ds, "SELECT COUNT(*) FROM sys_role_menu rm JOIN sys_menu m ON m.menu_id = rm.menu_id "
                + "WHERE rm.role_id = 3 AND m.perms LIKE 'nad:program:%'")).isEqualTo("5");
        // V21 — catalog imagery (URL columns), student view exposes hero/cover
        assertThat(single(ds, "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() "
                + "AND table_name = 'nad_university' AND column_name IN ('logo_image_url','cover_image_url')"))
                .isEqualTo("2");
        assertThat(single(ds, "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() "
                + "AND table_name = 'v_scholarship_student' AND column_name IN ('hero_image_url','cover_image_url')"))
                .isEqualTo("2");
        // V22 — stipend per level + accommodation room types + non-degree duration
        assertThat(tableExists(ds, "nad_scholarship_level_stipend")).isTrue();
        assertThat(tableExists(ds, "nad_scholarship_accommodation")).isTrue();
        assertThat(tableExists(ds, "nad_scholarship_stipend")).isFalse();
        assertThat(single(ds, "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() "
                + "AND table_name = 'nad_scholarship' AND column_name = 'non_degree_duration'")).isEqualTo("1");
        // V23 — coverage list + study duration + application channel + renewal + requirement flags
        assertThat(tableExists(ds, "nad_scholarship_coverage")).isTrue();
        assertThat(single(ds, "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() "
                + "AND table_name = 'nad_scholarship' AND column_name IN ('study_duration_months',"
                + "'application_channel','agency_number','renewal_conditions','requires_financial_proof',"
                + "'requires_foundation_year')")).isEqualTo("6");
        // V24 — public slug identifiers for universities + programmes (unique)
        assertThat(single(ds, "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() "
                + "AND table_name = 'nad_university' AND column_name = 'slug'")).isEqualTo("1");
        assertThat(single(ds, "SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() "
                + "AND table_name = 'nad_program' AND index_name = 'uk_program_slug'")).isEqualTo("1");
        // V25 — scholarship reference code (unique)
        assertThat(single(ds, "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() "
                + "AND table_name = 'nad_scholarship' AND column_name = 'reference_code'")).isEqualTo("1");
        assertThat(single(ds, "SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() "
                + "AND table_name = 'nad_scholarship' AND index_name = 'uk_scholarship_reference'")).isEqualTo("1");
        // V26 — media asset registry + append-only access log
        assertThat(tableExists(ds, "nad_media_asset")).isTrue();
        assertThat(tableExists(ds, "nad_media_access_log")).isTrue();
        assertThat(single(ds, "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() "
                + "AND table_name = 'nad_media_asset' AND column_name IN ('access_class','public_id')")).isEqualTo("2");
        assertThat(single(ds, "SELECT COUNT(DISTINCT index_name) FROM information_schema.statistics WHERE table_schema = DATABASE() "
                + "AND table_name = 'nad_media_asset' AND index_name = 'uk_media_provider_public_id'")).isEqualTo("1");
        // V27 — catalog *_media_id FKs + the student view exposes hero/cover media ids
        assertThat(single(ds, "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() "
                + "AND table_name = 'nad_university' AND column_name IN ('logo_media_id','banner_media_id')")).isEqualTo("2");
        assertThat(single(ds, "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() "
                + "AND table_name = 'nad_university_gallery' AND column_name = 'media_id'")).isEqualTo("1");
        assertThat(single(ds, "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() "
                + "AND table_name = 'nad_scholarship' AND column_name IN ('hero_media_id','cover_media_id')")).isEqualTo("2");
        assertThat(single(ds, "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() "
                + "AND table_name = 'nad_program' AND column_name = 'image_media_id'")).isEqualTo("1");
        assertThat(single(ds, "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() "
                + "AND table_name = 'v_scholarship_student' AND column_name = 'hero_media_id'")).isEqualTo("1");
        // the student view still must not leak the confidential university association
        assertThat(single(ds, "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() "
                + "AND table_name = 'v_scholarship_student' AND column_name = 'university_id'")).isEqualTo("0");
        // V28 — applicant profile photo media id
        assertThat(single(ds, "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() "
                + "AND table_name = 'nad_applicant' AND column_name = 'photo_media_id'")).isEqualTo("1");
        // V29 — media reconciliation Quartz job, seeded paused
        assertThat(single(ds, "SELECT COUNT(*) FROM sys_job "
                + "WHERE invoke_target = 'mediaReconciliationJob.run()'")).isEqualTo("1");
        assertThat(single(ds, "SELECT status FROM sys_job "
                + "WHERE invoke_target = 'mediaReconciliationJob.run()'")).isEqualTo("1");
        // V30 — transactional outbox
        assertThat(tableExists(ds, "nad_outbox_event")).isTrue();
        assertThat(single(ds, "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() "
                + "AND table_name = 'nad_outbox_event' AND column_name IN ('type','payload_json','next_attempt_at')"))
                .isEqualTo("3");
        assertThat(single(ds, "SELECT COUNT(DISTINCT index_name) FROM information_schema.statistics "
                + "WHERE table_schema = DATABASE() AND table_name = 'nad_outbox_event' "
                + "AND index_name = 'idx_outbox_ready'")).isEqualTo("1");
        // V31 — outbox poller Quartz job, seeded active
        assertThat(single(ds, "SELECT COUNT(*) FROM sys_job "
                + "WHERE invoke_target = 'outboxPollerJob.run()'")).isEqualTo("1");
        assertThat(single(ds, "SELECT status FROM sys_job "
                + "WHERE invoke_target = 'outboxPollerJob.run()'")).isEqualTo("0");
        // V32 — notification model
        assertThat(tableExists(ds, "nad_notification")).isTrue();
        assertThat(tableExists(ds, "nad_notification_delivery")).isTrue();
        assertThat(tableExists(ds, "nad_notification_preference")).isTrue();
        assertThat(tableExists(ds, "nad_notification_template")).isTrue();
        assertThat(single(ds, "SELECT COUNT(DISTINCT index_name) FROM information_schema.statistics "
                + "WHERE table_schema = DATABASE() AND table_name = 'nad_notification_delivery' "
                + "AND index_name = 'uk_notif_delivery_channel'")).isEqualTo("1");
        assertThat(single(ds, "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() "
                + "AND table_name = 'nad_notification' AND column_name = 'source_ref'")).isEqualTo("1");
        assertThat(single(ds, "SELECT COUNT(DISTINCT index_name) FROM information_schema.statistics "
                + "WHERE table_schema = DATABASE() AND table_name = 'nad_notification' "
                + "AND index_name = 'uk_notif_source'")).isEqualTo("1");
        // V33 — notification menu/permissions + default en templates
        // one C-menu (perms nad:notification:list) + three F-menus
        assertThat(single(ds, "SELECT COUNT(*) FROM sys_menu WHERE perms LIKE 'nad:notification:%'")).isEqualTo("4");
        assertThat(single(ds, "SELECT COUNT(*) FROM sys_role_menu rm JOIN sys_menu m ON m.menu_id = rm.menu_id "
                + "WHERE rm.role_id = 3 AND m.perms LIKE 'nad:notification:%'")).isEqualTo("4");
        // 4 from V33 + 2 from V37 (TASK_PROGRESS) + 4 from V51 (UNIVERSITY/PROGRAM published)
        assertThat(single(ds, "SELECT COUNT(*) FROM nad_notification_template WHERE locale = 'en'")).isEqualTo("12");
        // V34 — notification dispatch Quartz job, seeded active
        assertThat(single(ds, "SELECT COUNT(*) FROM sys_job "
                + "WHERE invoke_target = 'notificationDispatchJob.run()'")).isEqualTo("1");
        assertThat(single(ds, "SELECT status FROM sys_job "
                + "WHERE invoke_target = 'notificationDispatchJob.run()'")).isEqualTo("0");
        // V35 — employee (HR) records
        assertThat(tableExists(ds, "nad_employee")).isTrue();
        assertThat(single(ds, "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() "
                + "AND table_name = 'nad_employee' AND column_name IN ('salary_amount','start_date','position_id')"))
                .isEqualTo("3");
        assertThat(single(ds, "SELECT COUNT(DISTINCT index_name) FROM information_schema.statistics "
                + "WHERE table_schema = DATABASE() AND table_name = 'nad_employee' "
                + "AND index_name = 'uk_employee_user'")).isEqualTo("1");
        // V36 — tasks + append-only event log
        assertThat(tableExists(ds, "nad_task")).isTrue();
        assertThat(tableExists(ds, "nad_task_event")).isTrue();
        // V37 — HR menu/permissions + TASK_PROGRESS templates
        assertThat(single(ds, "SELECT COUNT(*) FROM sys_menu WHERE perms LIKE 'nad:employee:%'")).isEqualTo("6");
        // 6 from V37 + nad:task:progress from V49
        assertThat(single(ds, "SELECT COUNT(*) FROM sys_menu WHERE perms LIKE 'nad:task:%'")).isEqualTo("7");
        assertThat(single(ds, "SELECT COUNT(*) FROM sys_role_menu rm JOIN sys_menu m ON m.menu_id = rm.menu_id "
                + "WHERE rm.role_id = 3 AND (m.perms LIKE 'nad:employee:%' OR m.perms LIKE 'nad:task:%')")).isEqualTo("13");
        assertThat(single(ds, "SELECT COUNT(*) FROM nad_notification_template WHERE type = 'TASK_PROGRESS'")).isEqualTo("2");
        // V38 — finance
        assertThat(tableExists(ds, "nad_expense")).isTrue();
        assertThat(tableExists(ds, "nad_revenue")).isTrue();
        assertThat(tableExists(ds, "nad_expense_category")).isTrue();
        // V39 — categories + finance menu/permissions
        assertThat(single(ds, "SELECT COUNT(*) FROM nad_expense_category")).isEqualTo("9");
        assertThat(single(ds, "SELECT COUNT(*) FROM sys_menu WHERE perms LIKE 'nad:expense:%'")).isEqualTo("6");
        assertThat(single(ds, "SELECT COUNT(*) FROM sys_menu WHERE perms LIKE 'nad:revenue:%'")).isEqualTo("5");
        assertThat(single(ds, "SELECT COUNT(*) FROM sys_role_menu rm JOIN sys_menu m ON m.menu_id = rm.menu_id "
                + "WHERE rm.role_id = 3 AND (m.perms LIKE 'nad:finance:%' OR m.perms LIKE 'nad:expense:%' "
                + "OR m.perms LIKE 'nad:revenue:%')")).isEqualTo("12");

        // V40 — editable CNY->USD display rate
        assertThat(single(ds, "SELECT config_value FROM sys_config WHERE config_key = 'nadoumi.fx.cny_usd'"))
                .isEqualTo("0.1381");
        // V41 — confidential scholarship -> programme link
        assertThat(single(ds, "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() "
                + "AND table_name = 'nad_scholarship_internal' AND column_name = 'program_id'")).isEqualTo("1");
        // V42 — baseline "staff" role for rank-and-file employees (re-scoped by V49)
        assertThat(single(ds, "SELECT COUNT(*) FROM sys_role WHERE role_key = 'staff'")).isEqualTo("1");
        // V49 revoked staff's system:post:* / system:role:list / system:dept:* / nad:task:add|edit
        assertThat(single(ds, "SELECT COUNT(*) FROM sys_role_menu rm JOIN sys_menu m ON m.menu_id = rm.menu_id "
                + "JOIN sys_role r ON r.role_id = rm.role_id "
                + "WHERE r.role_key = 'staff' AND m.perms LIKE 'system:post:%'")).isEqualTo("0");
        // V43 — academic departments + programme term length
        assertThat(tableExists(ds, "nad_department")).isTrue();
        assertThat(single(ds, "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() "
                + "AND table_name = 'nad_program_major' AND column_name = 'department_id'")).isEqualTo("1");
        assertThat(single(ds, "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() "
                + "AND table_name = 'nad_program' AND column_name = 'term_length'")).isEqualTo("1");
        // V44 — nad:department:* perms under the Universities menu
        assertThat(single(ds, "SELECT COUNT(*) FROM sys_menu WHERE perms LIKE 'nad:department:%'")).isEqualTo("4");
        assertThat(single(ds, "SELECT COUNT(*) FROM sys_role_menu rm JOIN sys_menu m ON m.menu_id = rm.menu_id "
                + "WHERE rm.role_id = 3 AND m.perms LIKE 'nad:department:%'")).isEqualTo("4");
        // V45 — read-only payroll menu + perm
        assertThat(single(ds, "SELECT COUNT(*) FROM sys_menu WHERE perms = 'nad:payroll:view'")).isEqualTo("1");
        assertThat(single(ds, "SELECT COUNT(*) FROM sys_role_menu rm JOIN sys_menu m ON m.menu_id = rm.menu_id "
                + "WHERE rm.role_id = 3 AND m.perms = 'nad:payroll:view'")).isEqualTo("1");
        // V46 — university reference code + internal partner flag
        assertThat(single(ds, "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() "
                + "AND table_name = 'nad_university' AND column_name IN ('reference_code','partner_status')"))
                .isEqualTo("2");
        // V47 — structured employee emergency contact
        assertThat(single(ds, "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() "
                + "AND table_name = 'nad_employee' AND column_name IN "
                + "('emergency_contact_relationship','emergency_contact_phone','emergency_contact_email')")).isEqualTo("3");
        // V48 — multi-level programmes
        assertThat(tableExists(ds, "nad_program_level")).isTrue();
        assertThat(single(ds, "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() "
                + "AND table_name = 'nad_program_major' AND column_name = 'level'")).isEqualTo("1");
        // V49 — staff role re-scoped: new nad:task:progress perm; staff keeps only task view + progress
        assertThat(single(ds, "SELECT COUNT(*) FROM sys_menu WHERE perms = 'nad:task:progress'")).isEqualTo("1");
        // still denied after V49/V50: role admin, adding depts/posts, editing/creating tasks
        assertThat(single(ds, "SELECT COUNT(*) FROM sys_role_menu rm JOIN sys_role r ON r.role_id = rm.role_id "
                + "JOIN sys_menu m ON m.menu_id = rm.menu_id "
                + "WHERE r.role_key = 'staff' AND m.perms IN "
                + "('system:role:list','system:dept:add','system:dept:edit','system:dept:remove','system:post:list',"
                + "'nad:task:add','nad:task:edit','nad:university:remove','nad:program:remove','nad:scholarship:remove',"
                + "'nad:applicant:edit','nad:applicant:archive','nad:scholarship:publish')")).isEqualTo("0");
        // V50 — staff can now read the catalog and add / edit (not delete) universities / programmes / scholarships
        assertThat(single(ds, "SELECT COUNT(*) FROM sys_role_menu rm JOIN sys_role r ON r.role_id = rm.role_id "
                + "JOIN sys_menu m ON m.menu_id = rm.menu_id "
                + "WHERE r.role_key = 'staff' AND m.perms IN "
                + "('nad:applicant:list','nad:university:list','nad:university:create','nad:university:edit',"
                + "'nad:program:create','nad:scholarship:create','system:user:list','system:dept:list')")).isEqualTo("8");
        // V51 — catalog-published notification templates
        assertThat(single(ds, "SELECT COUNT(*) FROM nad_notification_template "
                + "WHERE type IN ('UNIVERSITY_PUBLISHED','PROGRAM_PUBLISHED')")).isEqualTo("4");
        // V52 — public Partners showcase flag (separate from the confidential partner_status)
        assertThat(single(ds, "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() "
                + "AND table_name = 'nad_university' AND column_name = 'public_partner'")).isEqualTo("1");
        // V53 — scholarship deadline reminders (table + 2 templates + daily Quartz job)
        assertThat(tableExists(ds, "nad_scholarship_deadline_reminder")).isTrue();
        assertThat(single(ds, "SELECT COUNT(*) FROM nad_notification_template "
                + "WHERE type = 'SCHOLARSHIP_DEADLINE_REMINDER'")).isEqualTo("2");
        assertThat(single(ds, "SELECT status FROM sys_job "
                + "WHERE invoke_target = 'scholarshipDeadlineReminderJob.run()'")).isEqualTo("0");

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

        assertThat(applied).isEqualTo(49); // V2..V53
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
