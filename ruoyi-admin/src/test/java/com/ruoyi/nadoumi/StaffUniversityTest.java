package com.ruoyi.nadoumi;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Staff university catalog: profile CRUD, children round-trip, unique guard, permission gate, public visibility. */
class StaffUniversityTest extends AbstractNadIntegrationTest {

    private static final String BODY = """
            {"name":"  Tsinghua University  ","nameCn":"清华大学","country":"cn","type":"PUBLIC",
             "city":"Beijing","province":"Beijing","foundedYear":1911,"totalStudents":50000,
             "introduction":"A leading research university.","status":"ACTIVE","publishStatus":"PUBLISHED",
             "rankings":[{"source":"QS","rankPosition":20,"rankYear":2026}],
             "highlights":[{"kind":"HIGHLIGHT","text":"C9 League member"},
                           {"kind":"ADVANTAGE","text":"Strong engineering"}]}""";

    @Test
    void full_profile_crud_with_children() throws Exception {
        createStaff("uni_ops", "nadoumi_super_admin");
        String token = staffToken("uni_ops");

        String created = mvc.perform(post("/api/staff/universities").header("Authorization", bearer(token))
                        .contentType("application/json").content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Tsinghua University"))
                .andExpect(jsonPath("$.country").value("CN"))
                .andExpect(jsonPath("$.nameCn").value("清华大学"))
                .andExpect(jsonPath("$.foundedYear").value(1911))
                .andExpect(jsonPath("$.rankings.length()").value(1))
                .andExpect(jsonPath("$.rankings[0].source").value("QS"))
                .andExpect(jsonPath("$.highlights.length()").value(2))
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(created, "$.id")).longValue();

        // edit: drop a ranking, keep one highlight, flip publish off
        mvc.perform(put("/api/staff/universities/{id}", id).header("Authorization", bearer(token))
                        .contentType("application/json")
                        .content("""
                            {"name":"Tsinghua University","country":"CN","status":"ACTIVE",
                             "publishStatus":"DRAFT","rankings":[],
                             "highlights":[{"kind":"HIGHLIGHT","text":"C9 League member"}]}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publishStatus").value("DRAFT"))
                .andExpect(jsonPath("$.rankings.length()").value(0))
                .andExpect(jsonPath("$.highlights.length()").value(1));

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

    @Test
    void public_endpoint_shows_only_published_active_universities() throws Exception {
        createStaff("uni_pub", "nadoumi_super_admin");
        String token = staffToken("uni_pub");

        // published + active
        String pub = mvc.perform(post("/api/staff/universities").header("Authorization", bearer(token))
                        .contentType("application/json").content(BODY))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long pubId = ((Number) JsonPath.read(pub, "$.id")).longValue();

        // draft
        mvc.perform(post("/api/staff/universities").header("Authorization", bearer(token))
                .contentType("application/json").content("""
                    {"name":"Draft University","country":"CN","status":"ACTIVE","publishStatus":"DRAFT"}"""))
                .andExpect(status().isCreated());

        // anonymous — no auth header
        mvc.perform(get("/api/public/universities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value((int) pubId))
                .andExpect(jsonPath("$.content[0].introduction").value("A leading research university."))
                // no operational fields leak
                .andExpect(jsonPath("$.content[0].status").doesNotExist())
                .andExpect(jsonPath("$.content[0].remark").doesNotExist());

        mvc.perform(get("/api/public/universities/{id}", pubId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.highlights.length()").value(2));
    }
}
