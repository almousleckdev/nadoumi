package com.ruoyi.nadoumi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** Personal details, family, financial supporter and emergency contact: saved once, owned by the applicant. */
class ApplicantFamilyTest extends AbstractStudentIntegrationTest {

    private static final String FATHER = "{\"relation\":\"FATHER\",\"fullName\":\"Ibrahima Diallo\",\"nationality\":\"Guinea\"}";
    private static final String MOTHER_DECEASED =
            "{\"relation\":\"MOTHER\",\"fullName\":\"Fatoumata Diallo\",\"deceased\":true,\"note\":\"Passed away in 2020\"}";
    private static final String PERSON =
            "{\"fullName\":\"Ibrahima Diallo\",\"relationship\":\"Father\",\"gender\":\"MALE\",\"phone\":\"+224611111111\"}";

    private String url(Student s, String suffix) {
        return s.applicantUrl() + suffix;
    }

    private org.springframework.test.web.servlet.ResultActions send(Student s, String method, String suffix, String body) throws Exception {
        var builder = switch (method) {
            case "POST" -> post(url(s, suffix));
            case "PUT" -> put(url(s, suffix));
            case "DELETE" -> delete(url(s, suffix));
            default -> get(url(s, suffix));
        };
        builder.header("Authorization", bearer(s.token()));
        if (body != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return mvc.perform(builder);
    }

    @Test
    void shouldSaveEachSection_andReportWhatTheFormStillLacks() throws Exception {
        Student s = register("fam", "all");
        send(s, "GET", "/form-readiness", null).andExpect(status().isOk())
                .andExpect(jsonPath("$.missing.length()").value(5));

        send(s, "PUT", "/personal-details", "{\"maritalStatus\":\"SINGLE\",\"religion\":\"Muslim\",\"placeOfBirth\":\"Conakry\","
                + "\"officialLanguage\":\"French\",\"permanentAddress\":\"12 Rue du Port\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.maritalStatus").value("SINGLE"));
        send(s, "GET", "/personal-details", null).andExpect(status().isOk()).andExpect(jsonPath("$.religion").value("Muslim"))
                .andExpect(jsonPath("$.mailingChoice").value("CURRENT"));
        // the delivery address is kept only when it is a separate one
        send(s, "PUT", "/personal-details", "{\"maritalStatus\":\"SINGLE\",\"religion\":\"Muslim\",\"placeOfBirth\":\"Conakry\",\"officialLanguage\":\"French\",\"permanentAddress\":\"12 Rue du Port\","
                + "\"mailingChoice\":\"OTHER\",\"mailingAddress\":\"5 Avenue Centrale\","
                + "\"mailingPostcode\":\"3000\",\"currentPostcode\":\"2000\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.mailingAddress").value("5 Avenue Centrale"))
                .andExpect(jsonPath("$.currentPostcode").value("2000"));
        send(s, "PUT", "/personal-details", "{\"maritalStatus\":\"SINGLE\",\"religion\":\"Muslim\",\"placeOfBirth\":\"Conakry\",\"officialLanguage\":\"French\",\"permanentAddress\":\"12 Rue du Port\","
                + "\"mailingChoice\":\"PERMANENT\",\"mailingAddress\":\"ignored\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.mailingAddress").doesNotExist());
        send(s, "PUT", "/people/supporter", PERSON).andExpect(status().isOk()).andExpect(jsonPath("$.role").value("SUPPORTER"));
        send(s, "PUT", "/people/china-emergency", PERSON).andExpect(status().isOk());
        send(s, "POST", "/family", FATHER).andExpect(status().isCreated());
        send(s, "GET", "/form-readiness", null).andExpect(jsonPath("$.missing[0]").value("FAMILY"))
                .andExpect(jsonPath("$.missing[1]").value("SIGNATURE"))
                .andExpect(jsonPath("$.missing.length()").value(2));

        send(s, "POST", "/family", MOTHER_DECEASED).andExpect(status().isCreated());
        // a parent has died, so the form also needs two siblings or relatives
        send(s, "GET", "/form-readiness", null).andExpect(jsonPath("$.missing[0]").value("FAMILY"));
        for (int i = 0; i < 2; i++) {
            send(s, "POST", "/family", "{\"relation\":\"SIBLING\",\"fullName\":\"Sibling " + i + "\"}").andExpect(status().isCreated());
        }
        send(s, "GET", "/form-readiness", null).andExpect(jsonPath("$.missing.length()").value(1)); // only the signature
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart(url(s, "/signature"))
                        .file(new org.springframework.mock.web.MockMultipartFile("file", "s.png", "image/png",
                                ApplicationPaymentFlowTest.signaturePng()))
                        .header("Authorization", bearer(s.token())))
                .andExpect(status().isOk());
        send(s, "GET", "/form-readiness", null).andExpect(jsonPath("$.missing.length()").value(0));
    }

    @Test
    void shouldRefuseABadFamilyMember_withAStructuredError() throws Exception {
        Student s = register("fam", "bad");
        send(s, "POST", "/family", FATHER).andExpect(status().isCreated());

        send(s, "POST", "/family", FATHER).andExpect(status().isBadRequest()); // a second father
        send(s, "POST", "/family", "{\"relation\":\"MOTHER\",\"fullName\":\"X\",\"deceased\":true}")
                .andExpect(status().isBadRequest()); // died with no note
        send(s, "POST", "/family", "{\"relation\":\"SIBLING\",\"fullName\":\"X\",\"dateOfBirth\":\"2999-01-01\"}")
                .andExpect(status().isBadRequest());
        send(s, "PUT", "/people/nobody", PERSON).andExpect(status().isNotFound());
    }

    @Test
    void shouldUpdateAndDeleteAMember() throws Exception {
        Student s = register("fam", "edit");
        String created = send(s, "POST", "/family", FATHER).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(created, "$.id")).longValue();

        send(s, "PUT", "/family/" + id, "{\"relation\":\"FATHER\",\"fullName\":\"Ibrahima B. Diallo\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.fullName").value("Ibrahima B. Diallo"));
        send(s, "DELETE", "/family/" + id, null).andExpect(status().isNoContent());
        send(s, "GET", "/family", null).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldKeepAnotherStudentOutOfTheFamilyData() throws Exception {
        Student owner = register("fam", "owner");
        Student intruder = register("fam", "intruder");
        send(owner, "POST", "/family", FATHER).andExpect(status().isCreated());

        for (String suffix : new String[] {"/family", "/personal-details", "/people/supporter", "/form-readiness"}) {
            mvc.perform(get(url(owner, suffix)).header("Authorization", bearer(intruder.token()))).andExpect(status().isForbidden());
        }
        mvc.perform(post(url(owner, "/family")).header("Authorization", bearer(intruder.token()))
                .contentType(MediaType.APPLICATION_JSON).content(FATHER)).andExpect(status().isForbidden());
        mvc.perform(put(url(owner, "/people/supporter")).header("Authorization", bearer(intruder.token()))
                .contentType(MediaType.APPLICATION_JSON).content(PERSON)).andExpect(status().isForbidden());
    }

    @Test
    void shouldRemoveTheFamilyData_whenStaffErasesTheApplicant() throws Exception {
        createStaff("fam_admin", "nadoumi_super_admin");
        String staff = bearer(staffToken("fam_admin"));
        Student s = register("fam", "erase");
        send(s, "PUT", "/personal-details", "{\"religion\":\"Muslim\"}").andExpect(status().isOk());
        send(s, "POST", "/family", FATHER).andExpect(status().isCreated());
        send(s, "PUT", "/people/supporter", PERSON).andExpect(status().isOk());

        mvc.perform(delete("/api/staff/applicants/" + pid(s.applicantId())).header("Authorization", staff))
                .andExpect(status().isNoContent());

        for (String table : new String[] {"nad_applicant_personal", "nad_applicant_family_member", "nad_applicant_person"}) {
            assertThat(jdbc.queryForObject("select count(*) from " + table + " where applicant_id = ?", Integer.class,
                    s.applicantId())).isZero();
        }
    }
}
