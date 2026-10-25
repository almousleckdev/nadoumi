package com.ruoyi.nadoumi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** Finance: the overview, the ledger and its CSV export. */
class FinanceApiTest extends AbstractNadIntegrationTest {

    private String today() {
        return LocalDate.now(ZoneId.of("Asia/Shanghai")).toString();
    }

    @Test
    void shouldShowTodaysRevenueInTheOverviewAndTheLedger_andExportItAsCsv() throws Exception {
        createStaff("fin_boss", "nadoumi_super_admin");
        String auth = bearer(staffToken("fin_boss"));
        mvc.perform(post("/api/staff/revenue").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"source\":\"SERVICE_FEE\",\"title\":\"Visa support, Ada\",\"amount\":1200,\"currency\":\"CNY\",\"receivedOn\":\"" + today() + "\"}"))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/staff/finance/overview").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("CNY"))
                .andExpect(jsonPath("$.kpis.revenueToday").value(1200))
                .andExpect(jsonPath("$.kpis.revenueMonth").value(1200))
                .andExpect(jsonPath("$.months.length()").value(12))
                .andExpect(jsonPath("$.revenueBySource[0].label").value("SERVICE_FEE"));

        mvc.perform(get("/api/staff/finance/ledger").param("kind", "REVENUE").param("q", "Visa").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Visa support, Ada"))
                .andExpect(jsonPath("$.content[0].kind").value("REVENUE"));

        String csv = mvc.perform(get("/api/staff/finance/ledger/export").param("q", "Visa").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andReturn().getResponse().getContentAsString();
        assertThat(csv).startsWith("Date,Type,Category,Title,Reference,Currency,Amount");
        assertThat(csv).contains("\"Visa support, Ada\"").contains(",CNY,1200");
    }

    @Test
    void shouldKeepFinanceFromStudentsAndFromStaffWithoutThePermission() throws Exception {
        createStudent("fin_student");
        mvc.perform(get("/api/staff/finance/overview").header("Authorization", bearer(studentToken("fin_student"))))
                .andExpect(status().is4xxClientError());
        createStaff("fin_none", "case_officer");
        mvc.perform(get("/api/staff/finance/ledger").header("Authorization", bearer(staffToken("fin_none"))))
                .andExpect(status().isForbidden());
    }
}
