package com.nadoumi.identity.money;

import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.core.redis.RedisCache;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

/**
 * The one editable CNY-to-USD display rate. Staff enter catalog money in RMB
 * (CNY); the USD figure shown beside it is {@code amountCny * rate}, computed
 * here at read time and never stored per record.
 *
 * <p>The rate is the {@code sys_config} key {@code nadoumi.fx.cny_usd} (seeded by
 * V40, editable in System &gt; Configuration). RuoYi keeps {@code sys_config}
 * warm in Redis under {@code sys_config:<key>}; a missing or unparseable value
 * falls back to {@link #DEFAULT_CNY_USD}.</p>
 */
@Component
public class FxRates {

    /** {@code sys_config} key; RuoYi caches it under {@code sys_config:<key>}. */
    public static final String CNY_USD_KEY = "nadoumi.fx.cny_usd";

    /** Used when the config row is absent or blank. */
    public static final BigDecimal DEFAULT_CNY_USD = new BigDecimal("0.1381");

    private final RedisCache redisCache;

    public FxRates(RedisCache redisCache) {
        this.redisCache = redisCache;
    }

    /** Current CNY&rarr;USD multiplier, always positive. */
    public BigDecimal cnyToUsd() {
        Object cached = redisCache.getCacheObject(CacheConstants.SYS_CONFIG_KEY + CNY_USD_KEY);
        if (cached != null) {
            try {
                BigDecimal parsed = new BigDecimal(cached.toString().trim());
                if (parsed.signum() > 0) {
                    return parsed;
                }
            }
            catch (NumberFormatException ignored) {
                // fall through to the default
            }
        }
        return DEFAULT_CNY_USD;
    }

    /**
     * Convert a CNY amount to USD for display, or {@code null} when {@code amountCny}
     * is null or {@code currency} is not CNY / RMB. Rounded to cents.
     */
    public BigDecimal toUsd(BigDecimal amountCny, String currency) {
        if (amountCny == null || !isCny(currency)) {
            return null;
        }
        return amountCny.multiply(cnyToUsd()).setScale(2, RoundingMode.HALF_UP);
    }

    /** The catalog default currency is CNY, so a null / blank code counts as CNY. */
    public static boolean isCny(String currency) {
        if (currency == null) {
            return true;
        }
        String c = currency.trim().toUpperCase();
        return c.isEmpty() || c.equals("CNY") || c.equals("RMB");
    }
}
