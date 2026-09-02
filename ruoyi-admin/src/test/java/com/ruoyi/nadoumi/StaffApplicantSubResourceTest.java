package com.ruoyi.nadoumi;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Staff CRUD on applicant sub-resources (education / test scores / contacts):
 * the new PUT + DELETE verbs, the cross-applicant guard, and the edit-permission
 * gate.
 */
class StaffApplicantSubResourceTest extends AbstractNadIntegrationTest {

    private long newApplicant(String token) throws Exception {
        String body = mvc.perform(post("/api/staff/applicants").header("Authorization", bearer(token))
                        .contentType("application/json")
                        .content("{\"givenName\":\"Su\",\"familyName\":\"Res\",\"invitedEmail\":\"su.res@example.test\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    @Test
    void education_can_be_added_updated_and_deleted_by_staff() throws Exception {
        createStaff("subres_edu", "ops_manager");
        String token = staffToken("subres_edu");
        long id = newApplicant(token);

        String created = mvc.perform(post("/api/staff/applicants/{id}/education", id)
                        .header("Authorization", bearer(token)).contentType("application/json")
                        .content("{\"institution\":\"Old School\",\"level\":\"BSC\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long eduId = ((Number) JsonPath.read(created, "$.id")).longValue();

        mvc.perform(put("/api/staff/applicants/{id}/education/{e}", id, eduId)
                        .header("Authorization", bearer(token)).contentType("application/json")
                        .content("{\"institution\":\"New School\",\"level\":\"MSC\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.institution").value("New School"))
                .andExpect(jsonPath("$.level").value("MSC"));

        mvc.perform(delete("/api/staff/applicants/{id}/education/{e}", id, eduId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/staff/applicants/{id}/education", id).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void test_scores_and_contacts_support_put_and_delete() throws Exception {
        createStaff("subres_sc", "ops_manager");
        String token = staffToken("subres_sc");
        long id = newApplicant(token);

        String score = mvc.perform(post("/api/staff/applicants/{id}/test-scores", id)
                        .header("Authorization", bearer(token)).contentType("application/json")
                        .content("{\"testType\":\"IELTS\",\"score\":\"6.5\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long scoreId = ((Number) JsonPath.read(score, "$.id")).longValue();

        mvc.perform(put("/api/staff/applicants/{id}/test-scores/{s}", id, scoreId)
                        .header("Authorization", bearer(token)).contentType("application/json")
                        .content("{\"testType\":\"IELTS\",\"score\":\"7.5\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value("7.5"));
        mvc.perform(delete("/api/staff/applicants/{id}/test-scores/{s}", id, scoreId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());

        String contact = mvc.perform(post("/api/staff/applicants/{id}/contacts", id)
                        .header("Authorization", bearer(token)).contentType("application/json")
                        .content("{\"relation\":\"GUARDIAN\",\"name\":\"Pat\",\"email\":\"pat@example.test\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long contactId = ((Number) JsonPath.read(contact, "$.id")).longValue();

        mvc.perform(put("/api/staff/applicants/{id}/contacts/{c}", id, contactId)
                        .header("Authorization", bearer(token)).contentType("application/json")
                        .content("{\"relation\":\"EMERGENCY\",\"name\":\"Sam\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.relation").value("EMERGENCY"))
                .andExpect(jsonPath("$.name").value("Sam"));
        mvc.perform(delete("/api/staff/applicants/{id}/contacts/{c}", id, contactId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
    }

    @Test
    void a_sub_resource_of_another_applicant_is_not_found() throws Exception {
        createStaff("subres_x", "ops_manager");
        String token = staffToken("subres_x");
        long a = newApplicant(token);
        long b = newApplicant(token);

        String created = mvc.perform(post("/api/staff/applicants/{id}/test-scores", a)
                        .header("Authorization", bearer(token)).contentType("application/json")
                        .content("{\"testType\":\"TOEFL\",\"score\":\"100\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long scoreOnA = ((Number) JsonPath.read(created, "$.id")).longValue();

        mvc.perform(put("/api/staff/applicants/{id}/test-scores/{s}", b, scoreOnA)
                        .header("Authorization", bearer(token)).contentType("application/json")
                        .content("{\"testType\":\"TOEFL\",\"score\":\"110\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void staff_without_edit_permission_cannot_update_a_sub_resource() throws Exception {
        // bespoke role: nad:applicant:view only (survives baseSetup, which clears users not roles)
        jdbc.update("insert into sys_role (role_name, role_key, role_sort, data_scope, status, "
                + "create_by, create_time) values ('View only','sub_view_only',98,'1','0','test',now()) "
                + "on duplicate key update role_key = role_key");
        Long roleId = jdbc.queryForObject("select role_id from sys_role where role_key = 'sub_view_only'", Long.class);
        Long viewMenu = jdbc.queryForObject(
                "select menu_id from sys_menu where perms = 'nad:applicant:view'", Long.class);
        jdbc.update("delete from sys_role_menu where role_id = ?", roleId);
        jdbc.update("insert into sys_role_menu (role_id, menu_id) values (?, ?)", roleId, viewMenu);

        String editor = staffTokenFor("subres_owner", "ops_manager");
        long id = newApplicant(editor);
        String created = mvc.perform(post("/api/staff/applicants/{id}/education", id)
                        .header("Authorization", bearer(editor)).contentType("application/json")
                        .content("{\"institution\":\"Uni\",\"level\":\"BSC\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long eduId = ((Number) JsonPath.read(created, "$.id")).longValue();

        String viewer = staffTokenFor("subres_viewer", "sub_view_only");
        mvc.perform(put("/api/staff/applicants/{id}/education/{e}", id, eduId)
                        .header("Authorization", bearer(viewer)).contentType("application/json")
                        .content("{\"institution\":\"Hacked\",\"level\":\"PHD\"}"))
                .andExpect(status().isForbidden());
        // ...but the same viewer can read it
        mvc.perform(get("/api/staff/applicants/{id}/education", id).header("Authorization", bearer(viewer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].institution").value("Uni"));
    }

    private String staffTokenFor(String username, String roleKey) throws Exception {
        createStaff(username, roleKey);
        return staffToken(username);
    }
}
