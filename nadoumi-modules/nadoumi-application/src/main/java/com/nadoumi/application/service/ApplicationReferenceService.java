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
 * Hands out application references: {@code NAD} + the start date + that day's running number, for example
 * {@code NAD20261023001}. The date is China time, where Nadoumi works, so the day rolls over at midnight there.
 */
@Service
public class ApplicationReferenceService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");
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
        LocalDate day = LocalDate.now(clock.withZone(BUSINESS_ZONE));
        mapper.take(day);
        return format(day, mapper.lastTaken());
    }

    static String format(LocalDate day, int sequence) {
        return PREFIX + DAY.format(day) + String.format("%03d", sequence);
    }
}
