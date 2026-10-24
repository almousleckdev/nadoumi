package com.nadoumi.application.service;

import com.nadoumi.application.mapper.ApplicationReferenceMapper;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hands out application references: {@code NAD} + the year + the year's running number, for example
 * {@code NAD2027001}. The year is China time, where Nadoumi works, so the count starts again at 001 when the new year
 * begins there. Past 999 the number simply grows (NAD20271000).
 */
@Service
public class ApplicationReferenceService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter YEAR = DateTimeFormatter.ofPattern("yyyy");
    private static final String PREFIX = "NAD";

    private final ApplicationReferenceMapper mapper;
    private final Clock clock;

    @Autowired
    public ApplicationReferenceService(ApplicationReferenceMapper mapper) {
        this(mapper, Clock.systemUTC());
    }

    ApplicationReferenceService(ApplicationReferenceMapper mapper, Clock clock) {
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional(rollbackFor = Exception.class)
    public String next() {
        // the counter table is keyed by a date: the first day of the year stands for the whole year
        LocalDate yearStart = LocalDate.now(clock.withZone(BUSINESS_ZONE)).withDayOfYear(1);
        mapper.take(yearStart);
        return format(yearStart, mapper.lastTaken());
    }

    static String format(LocalDate day, int sequence) {
        return PREFIX + YEAR.format(day) + String.format("%03d", sequence);
    }
}
