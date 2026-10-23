package com.nadoumi.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nadoumi.application.mapper.ApplicationReferenceMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class ApplicationReferenceServiceTest {

    @Test
    void shouldFormatTheReferenceAsNadDateAndThreeDigitDailyNumber() {
        assertThat(ApplicationReferenceService.format(LocalDate.of(2026, 10, 23), 1)).isEqualTo("NAD20261023001");
        assertThat(ApplicationReferenceService.format(LocalDate.of(2026, 10, 23), 42)).isEqualTo("NAD20261023042");
        assertThat(ApplicationReferenceService.format(LocalDate.of(2026, 1, 5), 1000)).isEqualTo("NAD202601051000");
    }

    @Test
    void shouldUseTheChinaDate_whenItIsAlreadyTomorrowThere() {
        ApplicationReferenceMapper mapper = mock(ApplicationReferenceMapper.class);
        when(mapper.lastTaken()).thenReturn(7);
        // 20:30 UTC on 22 October is 04:30 on 23 October in China
        Clock clock = Clock.fixed(Instant.parse("2026-10-22T20:30:00Z"), ZoneOffset.UTC);

        String reference = new ApplicationReferenceService(mapper, clock).next();

        assertThat(reference).isEqualTo("NAD20261023007");
        verify(mapper).take(LocalDate.of(2026, 10, 23));
    }
}
