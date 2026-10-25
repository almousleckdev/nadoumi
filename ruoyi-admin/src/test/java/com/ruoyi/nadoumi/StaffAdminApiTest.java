package com.ruoyi.nadoumi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** Staff administration screens: posts (positions) and employees. */
class StaffAdminApiTest extends AbstractNadIntegrationTest {

    private String today() {
        return LocalDate.now(ZoneId.of("Asia/Shanghai")).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    }

    @Test
    void shouldGenerateThePostCodeAndOrder_whenNeitherIsSent() throws Exception {
        createStaff("adm_posts", "nadoumi_super_admin");
        String auth = bearer(staffToken("adm_posts"));

        for (String name : new String[] {"Admissions Officer", "Finance Controller"}) {
            mvc.perform(post("/system/post").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"postName\":\"" + name + "\",\"status\":\"0\"}")).andExpect(status().isOk());
        }

        var codes = jdbc.queryForList("select post_code from sys_post where post_name in ('Admissions Officer','Finance Controller') order by post_id",
                String.class);
        assertThat(codes).hasSize(2);
        assertThat(codes.get(0)).matches("NA" + today() + "\\d{2,}");
        assertThat(codes.get(1)).isNotEqualTo(codes.get(0)).startsWith("NA" + today());
        assertThat(Integer.parseInt(codes.get(1).substring(("NA" + today()).length())))
                .isEqualTo(Integer.parseInt(codes.get(0).substring(("NA" + today()).length())) + 1);
        assertThat(jdbc.queryForObject("select count(*) from sys_post where post_name in ('Admissions Officer','Finance Controller') and post_sort > 0",
                Integer.class)).isEqualTo(2);
    }

    @Test
    void shouldRequireAPosition_whenCreatingAnEmployee() throws Exception {
        createStaff("adm_hr", "nadoumi_super_admin");
        String auth = bearer(staffToken("adm_hr"));
        String body = "{\"userName\":\"new_hire_a\",\"password\":\"Initial-Pass-1!\",\"nickName\":\"New Hire\","
                + "\"employmentType\":\"FULL_TIME\",\"employmentStatus\":\"ACTIVE\",\"startDate\":\"2026-11-01\"%s}";

        mvc.perform(post("/api/staff/employees").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content(body.formatted(""))).andExpect(status().isBadRequest());

        mvc.perform(post("/system/post").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"postName\":\"Case Officer\",\"status\":\"0\"}")).andExpect(status().isOk());
        long postId = jdbc.queryForObject("select post_id from sys_post where post_name = 'Case Officer'", Long.class);

        mvc.perform(post("/api/staff/employees").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content(body.formatted(",\"positionId\":" + postId))).andExpect(status().isCreated());
    }
}
