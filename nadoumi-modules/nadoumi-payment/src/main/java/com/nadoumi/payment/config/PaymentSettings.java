package com.nadoumi.payment.config;

import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.core.redis.RedisCache;
import java.math.BigDecimal;
import java.time.Duration;
import org.springframework.stereotype.Component;

/**
 * The editable payment settings. Both values are {@code sys_config} rows (seeded by V111, editable in System &gt;
 * Configuration); RuoYi keeps them warm in Redis, and a missing or unparseable value falls back to a safe default.
 */
@Component
public class PaymentSettings {

    public static final String FEE_KEY = "nadoumi.application.fee";
    public static final String PAYEE_KEY = "nadoumi.payment.payee";
    public static final String NOTIFY_ROLES_KEY = "nadoumi.application.notify-roles";

    /** How long a student has to pay and report the payment once the QR is shown. */
    public static final Duration PAYMENT_WINDOW = Duration.ofMinutes(15);

    /** Catalog and fee money is entered in RMB. */
    public static final String CURRENCY = "CNY";

    private static final String DEFAULT_NOTIFY_ROLE = "nadoumi_super_admin";
    private static final java.util.regex.Pattern ROLE_KEY = java.util.regex.Pattern.compile("[a-z][a-z0-9_]{1,63}");
    private static final String DEFAULT_PAYEE = "Sichuan Nadoumi Education Consulting Co., Ltd.";

    private final RedisCache redisCache;

    public PaymentSettings(RedisCache redisCache) {
        this.redisCache = redisCache;
    }

    /** The Nadoumi application fee in RMB; zero (or unset) means no payment step. */
    public BigDecimal applicationFee() {
        Object cached = redisCache.getCacheObject(CacheConstants.SYS_CONFIG_KEY + FEE_KEY);
        if (cached == null) {
            return BigDecimal.ZERO;
        }
        try {
            BigDecimal fee = new BigDecimal(cached.toString().trim());
            return fee.signum() > 0 ? fee : BigDecimal.ZERO;
        }
        catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * Role keys whose active users get the submitted application form by email. Only well-formed keys are
     * accepted, so a stray value in the setting can never become an unexpected recipient query.
     */
    public java.util.List<String> notifyRoleKeys() {
        Object cached = redisCache.getCacheObject(CacheConstants.SYS_CONFIG_KEY + NOTIFY_ROLES_KEY);
        String raw = cached == null || cached.toString().isBlank() ? DEFAULT_NOTIFY_ROLE : cached.toString();
        return java.util.Arrays.stream(raw.split(","))
                .map(String::strip)
                .filter(key -> ROLE_KEY.matcher(key).matches())
                .distinct()
                .toList();
    }

    public String payee() {
        Object cached = redisCache.getCacheObject(CacheConstants.SYS_CONFIG_KEY + PAYEE_KEY);
        return cached == null || cached.toString().isBlank() ? DEFAULT_PAYEE : cached.toString().trim();
    }
}
