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
    void shouldFormatTheReferenceAsNadYearAndThreeDigitYearlyNumber() {
        assertThat(ApplicationReferenceService.format(LocalDate.of(2027, 1, 1), 1)).isEqualTo("NAD2027001");
        assertThat(ApplicationReferenceService.format(LocalDate.of(2027, 6, 9), 42)).isEqualTo("NAD2027042");
        assertThat(ApplicationReferenceService.format(LocalDate.of(2027, 12, 31), 1000)).isEqualTo("NAD20271000");
    }

    @Test
    void shouldCountWithinTheYearInChinaTime_andStartAgainWhenTheNewYearBeginsThere() {
        ApplicationReferenceMapper mapper = mock(ApplicationReferenceMapper.class);
        when(mapper.lastTaken()).thenReturn(1);
        // 17:00 UTC on 31 December 2026 is already 01:00 on 1 January 2027 in China
        Clock clock = Clock.fixed(Instant.parse("2026-12-31T17:00:00Z"), ZoneOffset.UTC);

        String reference = new ApplicationReferenceService(mapper, clock).next();

        assertThat(reference).isEqualTo("NAD2027001");
        verify(mapper).take(LocalDate.of(2027, 1, 1));
    }

    @Test
    void shouldKeepOneCounterForTheWholeYear() {
        ApplicationReferenceMapper mapper = mock(ApplicationReferenceMapper.class);
        when(mapper.lastTaken()).thenReturn(8);
        Clock clock = Clock.fixed(Instant.parse("2027-03-05T10:00:00Z"), ZoneOffset.UTC);

        assertThat(new ApplicationReferenceService(mapper, clock).next()).isEqualTo("NAD2027008");
        verify(mapper).take(LocalDate.of(2027, 1, 1));
    }
}
