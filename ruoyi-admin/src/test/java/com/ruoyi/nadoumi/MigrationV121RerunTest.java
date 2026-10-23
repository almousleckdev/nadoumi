package com.ruoyi.nadoumi;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/**
 * V121 must survive a rerun: MySQL DDL is not transactional, so a start that died half way leaves its column behind. The
 * script is executed again here, statement by statement, over the database Flyway has already migrated.
 */
class MigrationV121RerunTest extends AbstractNadIntegrationTest {

    private static List<String> statements() throws Exception {
        String sql = new String(new ClassPathResource("db/migration/V121__nad_payment_document_verify_code.sql")
                .getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String line : sql.split("\n")) {
            if (line.strip().startsWith("--")) {
                continue;
            }
            current.append(line).append('\n');
            if (line.strip().endsWith(";")) {
                result.add(current.toString().strip().replaceAll(";$", ""));
                current.setLength(0);
            }
        }
        return result;
    }

    private int columns() {
        return jdbc.queryForObject("select count(*) from information_schema.columns where table_schema = database() "
                + "and table_name = 'nad_payment_document' and column_name = 'verify_code'", Integer.class);
    }

    private int indexes() {
        return jdbc.queryForObject("select count(*) from information_schema.statistics where table_schema = database() "
                + "and table_name = 'nad_payment_document' and index_name = 'idx_payment_document_number'", Integer.class);
    }

    @Test
    void shouldRunAgainWithoutFailing_andLeaveOneColumnAndOneIndex() throws Exception {
        assertThat(columns()).isEqualTo(1);
        for (String statement : statements()) {
            jdbc.execute(statement);
        }

        assertThat(columns()).isEqualTo(1);
        assertThat(indexes()).isEqualTo(1);
    }

    @Test
    void shouldFinishAHalfDoneRun_whenOnlyTheColumnExists() throws Exception {
        jdbc.execute("alter table nad_payment_document drop index idx_payment_document_number");
        assertThat(indexes()).isZero();

        for (String statement : statements()) {
            jdbc.execute(statement);
        }

        assertThat(columns()).isEqualTo(1);
        assertThat(indexes()).isEqualTo(1);
    }
}
