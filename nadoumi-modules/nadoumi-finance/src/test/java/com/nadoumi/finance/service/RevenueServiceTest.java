package com.nadoumi.finance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nadoumi.finance.domain.Revenue;
import com.nadoumi.finance.mapper.RevenueMapper;
import com.nadoumi.finance.web.response.RevenueResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class RevenueServiceTest {

    private final RevenueMapper mapper = mock(RevenueMapper.class);
    private final RevenueService service = new RevenueService(mapper);

    private static Revenue stored() {
        Revenue r = new Revenue();
        r.setId(1L);
        r.setSource("TUITION_COMMISSION");
        r.setTitle("Q1 commission");
        r.setAmount(new BigDecimal("500.00"));
        r.setCurrency("USD");
        r.setReceivedOn(LocalDate.of(2026, 1, 15));
        r.setRecordedBy(3L);
        r.setRecordedByName("Ada Manager");
        r.setCreateBy("ada.manager");
        r.setUpdateBy("ada.manager");
        return r;
    }

    @Test
    void get_mapsDisplayName_butNeverExposesRawActorIdOrUsernames() {
        when(mapper.findById(1L)).thenReturn(stored());

        RevenueResponse response = service.get(1L);

        assertThat(response.recordedByName()).isEqualTo("Ada Manager");
        // RevenueResponse must not carry recordedBy/createBy/updateBy — see the
        // "Revenue entity returned directly" finding this DTO replaces.
        assertThat(Arrays.stream(RevenueResponse.class.getRecordComponents())
                        .map(c -> c.getName())
                        .collect(Collectors.toSet()))
                .doesNotContain("recordedBy", "createBy", "updateBy");
    }

    @Test
    void recordApplicationFee_booksTheFeeAsApplicationRevenueLinkedToTheApplication() {
        service.recordApplicationFee(new BigDecimal("300.00"), "cny", LocalDate.of(2026, 10, 22), "NAD-PAY-7K3Q9F", 41L, 9L);

        org.mockito.ArgumentCaptor<Revenue> saved = org.mockito.ArgumentCaptor.forClass(Revenue.class);
        org.mockito.Mockito.verify(mapper).insert(saved.capture());
        Revenue r = saved.getValue();
        assertThat(r.getSource()).isEqualTo("APPLICATION_FEE");
        assertThat(r.getAmount()).isEqualByComparingTo("300.00");
        assertThat(r.getCurrency()).isEqualTo("CNY");
        assertThat(r.getReference()).isEqualTo("NAD-PAY-7K3Q9F");
        assertThat(r.getRelatedType()).isEqualTo("application");
        assertThat(r.getRelatedId()).isEqualTo(41L);
        assertThat(r.getRecordedBy()).isEqualTo(9L);
    }

    @Test
    void recordApplicationFee_isIdempotent_soARepeatedConfirmationNeverBooksTheFeeTwice() {
        when(mapper.countByRelated("application", 41L, "NAD-PAY-7K3Q9F")).thenReturn(1);

        service.recordApplicationFee(new BigDecimal("300.00"), "CNY", LocalDate.of(2026, 10, 22), "NAD-PAY-7K3Q9F", 41L, 9L);

        org.mockito.Mockito.verify(mapper, org.mockito.Mockito.never()).insert(org.mockito.ArgumentMatchers.any());
    }
}

