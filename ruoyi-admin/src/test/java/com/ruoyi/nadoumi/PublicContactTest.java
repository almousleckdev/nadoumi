package com.ruoyi.nadoumi;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

/** Anonymous public "Contact us" endpoint: persists, validates, drops bot submissions. */
class PublicContactTest extends AbstractNadIntegrationTest {

    private int inquiryCount() {
        return jdbc.queryForObject("select count(*) from nad_contact_inquiry", Integer.class);
    }

    @Test
    void an_anonymous_visitor_can_submit_a_message_and_it_is_stored() throws Exception {
        mvc.perform(post("/api/public/contact").contentType("application/json").content("""
                {"firstName":"Dana","lastName":"Ali","email":"dana@example.com","phone":"+212600000000",
                 "category":"SCHOLARSHIPS","subject":"Programmes",
                 "message":"Do you support the September intake?","locale":"en"}"""))
                .andExpect(status().isAccepted());

        assertThat(inquiryCount()).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "select concat(first_name,'|',last_name,'|',name,'|',phone,'|',status) "
                        + "from nad_contact_inquiry order by id desc limit 1", String.class))
                .isEqualTo("Dana|Ali|Dana Ali|+212600000000|NEW");
    }

    @Test
    void a_missing_message_is_rejected_without_persisting() throws Exception {
        mvc.perform(post("/api/public/contact").contentType("application/json").content("""
                {"firstName":"Dana","lastName":"Ali","email":"dana@example.com"}"""))
                .andExpect(status().isBadRequest());

        assertThat(inquiryCount()).isZero();
    }

    @Test
    void a_bot_submission_with_a_filled_honeypot_is_accepted_but_not_stored() throws Exception {
        mvc.perform(post("/api/public/contact").contentType("application/json").content("""
                {"firstName":"Bot","lastName":"Net","email":"bot@example.com","message":"buy now",
                 "website":"http://spam.example"}"""))
                .andExpect(status().isAccepted());

        assertThat(inquiryCount()).isZero();
    }
}
