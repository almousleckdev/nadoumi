package com.nadoumi.scholarship.config;

import com.nadoumi.identity.money.FxRates;
import com.nadoumi.scholarship.web.response.PublicScholarshipResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Seeds {@link PublicScholarshipResponse}'s display FX rate from the editable
 * {@code nadoumi.fx.cny_usd} setting once the context is up. Scholarship money
 * factories are static (records), so the rate is a class field set here rather
 * than injected; a rate change takes effect on the next restart.
 */
@Component
public class ScholarshipFxInitializer {

    private final FxRates fxRates;

    public ScholarshipFxInitializer(FxRates fxRates) {
        this.fxRates = fxRates;
    }

    @EventListener(ApplicationReadyEvent.class)
    void applyRate() {
        BigDecimal cnyToUsd = fxRates.cnyToUsd();
        if (cnyToUsd != null && cnyToUsd.signum() > 0) {
            // stored rate is USD-per-CNY; the scholarship helpers want CNY-per-USD
            PublicScholarshipResponse.setRmbPerUsd(
                    BigDecimal.ONE.divide(cnyToUsd, 6, RoundingMode.HALF_UP));
        }
    }
}
