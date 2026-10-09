package com.ruoyi.nadoumi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Connection;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.datasource.init.ScriptUtils;

/**
 * V103 clears students that were deleted the old way, and only those: staff (live or deleted), the break-glass
 * administrator, live students and the catalog must come out exactly as they went in.
 */
class SoftDeletedStudentCleanupMigrationTest extends AbstractStudentIntegrationTest {

    private void runMigration() throws Exception {
        try (Connection connection = jdbc.getDataSource().getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V103__nad_soft_deleted_student_accounts.sql"));
        }
    }

    private long count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Long.class, args);
    }

    private void chat(String staffToken, long studentUserId) throws Exception {
        mvc.perform(post("/api/staff/conversations/direct").header("Authorization", bearer(staffToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"userId\":" + studentUserId + "}"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRemoveOnlySoftDeletedStudents_andLeaveStaffTheAdminAndTheCatalogAlone() throws Exception {
        createStaff("v103_staff", "ops_manager");
        long deletedStaff = createStaff("v103_gone_staff", "ops_manager");
        jdbc.update("update sys_user set del_flag = '2' where user_id = ?", deletedStaff);
        long liveStudent = createStudent("v103_live");
        long oldStudent = createStudent("v103_old");
        String staff = staffToken("v103_staff");
        chat(staff, liveStudent);
        chat(staff, oldStudent);
        jdbc.update("update sys_user set del_flag = '2' where user_id = ?", oldStudent); // RuoYi soft delete: email kept

        jdbc.update("insert into nad_university (name, slug, country) values ('Keep U', 'keep-u', 'CN')");
        long university = jdbc.queryForObject("select max(id) from nad_university", Long.class);
        jdbc.update("insert into nad_program (university_id, name, slug, program_type) values (?, 'Keep MBA', 'keep-mba', 'MASTER')",
                university);
        long catalogBefore = count("select count(*) from nad_university") + count("select count(*) from nad_program");
        long staffBefore = count("select count(*) from sys_user where user_type = '00'");
        long adminBefore = count("select count(*) from sys_user where user_id = 1");

        runMigration();

        assertThat(count("select count(*) from sys_user where user_id = ?", oldStudent)).as("soft-deleted student").isZero();
        assertThat(count("select count(*) from nad_conversation_participant where user_id = ?", oldStudent))
                .as("their chat is gone").isZero();

        assertThat(count("select count(*) from sys_user where user_id = ?", liveStudent)).as("live student").isEqualTo(1);
        assertThat(count("select count(*) from nad_conversation_participant where user_id = ?", liveStudent))
                .as("the live student's chat").isEqualTo(1);
        assertThat(count("select count(*) from sys_user where user_type = '00'")).as("staff, live and deleted").isEqualTo(staffBefore);
        assertThat(count("select count(*) from sys_user where user_id = ?", deletedStaff)).as("deleted staff row is not this migration's job")
                .isEqualTo(1);
        assertThat(count("select count(*) from sys_user where user_id = 1")).as("break-glass admin").isEqualTo(adminBefore);
        assertThat(count("select count(*) from nad_university") + count("select count(*) from nad_program"))
                .as("universities and programmes").isEqualTo(catalogBefore);
    }

    @Test
    void shouldBeSafeToRunWhenThereIsNothingToClean() throws Exception {
        createStudent("v103_only_live");
        long before = count("select count(*) from sys_user");

        runMigration();

        assertThat(count("select count(*) from sys_user")).isEqualTo(before);
    }
}
