package com.ruoyi.nadoumi;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Staff university catalog: CRUD, the unique (name, country) guard, and the permission gate. */
class StaffUniversityTest extends AbstractNadIntegrationTest {

    private static final String BODY = """
            {"name":"  Tsinghua University  ","country":"cn","city":"Beijing","status":"ACTIVE"}""";

    @Test
    void full_crud_lifecycle() throws Exception {
        createStaff("uni_ops", "nadoumi_super_admin");
        String token = staffToken("uni_ops");

        String created = mvc.perform(post("/api/staff/universities").header("Authorization", bearer(token))
                        .contentType("application/json").content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Tsinghua University"))
                .andExpect(jsonPath("$.country").value("CN"))
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(created, "$.id")).longValue();

        mvc.perform(get("/api/staff/universities/{id}", id).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city").value("Beijing"));

        mvc.perform(get("/api/staff/universities").param("q", "Tsinghua").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value((int) id));

        mvc.perform(put("/api/staff/universities/{id}", id).header("Authorization", bearer(token))
                        .contentType("application/json")
                        .content("{\"name\":\"Tsinghua University\",\"country\":\"CN\",\"city\":\"Haidian\",\"status\":\"INACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city").value("Haidian"))
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        mvc.perform(delete("/api/staff/universities/{id}", id).header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/staff/universities/{id}", id).header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void a_duplicate_name_in_the_same_country_is_rejected() throws Exception {
        createStaff("uni_dup", "nadoumi_super_admin");
        String token = staffToken("uni_dup");

        mvc.perform(post("/api/staff/universities").header("Authorization", bearer(token))
                .contentType("application/json").content(BODY)).andExpect(status().isCreated());

        mvc.perform(post("/api/staff/universities").header("Authorization", bearer(token))
                        .contentType("application/json").content(BODY))
                .andExpect(status().isBadRequest());
    }

    @Test
    void read_only_staff_can_list_but_not_create() throws Exception {
        createStaff("uni_ro", "case_officer");
        String token = staffToken("uni_ro");

        mvc.perform(get("/api/staff/universities").header("Authorization", bearer(token)))
                .andExpect(status().isOk());

        mvc.perform(post("/api/staff/universities").header("Authorization", bearer(token))
                        .contentType("application/json").content(BODY))
                .andExpect(status().isForbidden());
    }
}
