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
}
