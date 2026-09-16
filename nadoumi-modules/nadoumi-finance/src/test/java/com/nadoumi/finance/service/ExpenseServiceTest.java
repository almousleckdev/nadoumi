package com.nadoumi.finance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nadoumi.finance.domain.Expense;
import com.nadoumi.finance.mapper.ExpenseMapper;
import com.nadoumi.finance.web.response.ExpenseResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ExpenseServiceTest {

    private final ExpenseMapper mapper = mock(ExpenseMapper.class);
    private final ExpenseService service = new ExpenseService(mapper);

    private static Expense stored() {
        Expense e = new Expense();
        e.setId(1L);
        e.setReceiptNo("NAD-EXP-2026-0001");
        e.setTitle("Visa courier");
        e.setAmount(new BigDecimal("120.00"));
        e.setCurrency("CNY");
        e.setSpentOn(LocalDate.of(2026, 1, 10));
        e.setStatus("APPROVED");
        e.setSubmittedBy(7L);
        e.setSubmittedByName("Jane Staff");
        e.setApprovedBy(3L);
        e.setApprovedByName("Ada Manager");
        e.setCreateBy("jane.staff");
        e.setUpdateBy("ada.manager");
        return e;
    }

    @Test
    void get_mapsDisplayNames_butNeverExposesRawActorIdsOrUsernames() {
        when(mapper.findById(1L)).thenReturn(stored());

        ExpenseResponse response = service.get(1L);

        assertThat(response.submittedByName()).isEqualTo("Jane Staff");
        assertThat(response.approvedByName()).isEqualTo("Ada Manager");
        // ExpenseResponse must not carry submittedBy/approvedBy/createBy/updateBy —
        // see the "Expense entity returned directly" finding this DTO replaces.
        assertThat(Arrays.stream(ExpenseResponse.class.getRecordComponents())
                        .map(c -> c.getName())
                        .collect(Collectors.toSet()))
                .doesNotContain("submittedBy", "approvedBy", "createBy", "updateBy");
    }
}
