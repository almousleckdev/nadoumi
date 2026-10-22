package com.nadoumi.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nadoumi.payment.config.PaymentSettings;
import com.nadoumi.payment.mapper.PaymentMapper;
import com.ruoyi.common.core.redis.RedisCache;
import org.junit.jupiter.api.Test;

class PaymentSettingsTest {

    private final RedisCache cache = mock(RedisCache.class);
    private final PaymentMapper mapper = mock(PaymentMapper.class);
    private final PaymentSettings settings = new PaymentSettings(cache, mapper);

    private void roles(String value) {
        when(cache.getCacheObject(anyString())).thenReturn(value);
    }

    @Test
    void shouldDefaultToTheSuperAdminRole_whenNothingIsConfigured() {
        roles(null);

        assertThat(settings.notifyRoleKeys()).containsExactly("nadoumi_super_admin");
    }

    @Test
    void shouldAcceptSeveralWellFormedRoleKeys() {
        roles(" nadoumi_super_admin , ops_manager,ops_manager ");

        assertThat(settings.notifyRoleKeys()).containsExactly("nadoumi_super_admin", "ops_manager");
    }

    @Test
    void shouldDropAnythingThatIsNotARoleKey() {
        roles("ops_manager,'; drop table sys_user;--, ADMIN ,x");

        assertThat(settings.notifyRoleKeys()).containsExactly("ops_manager");
    }

    private void fee(String cached, String stored) {
        when(cache.getCacheObject(anyString())).thenReturn(cached);
        when(mapper.findConfigValue(PaymentSettings.FEE_KEY)).thenReturn(stored);
    }

    @Test
    void shouldReadTheFee_fromTheCacheOrTheDatabaseRowOnAColdCache() {
        fee("300", null);
        assertThat(settings.applicationFee()).isEqualByComparingTo("300");

        fee(null, "450.50");
        assertThat(settings.applicationFee()).isEqualByComparingTo("450.50");
    }

    @Test
    void shouldDisableThePaymentStep_onlyForAnExplicitZeroOrNoRowAtAll() {
        fee("0", null);
        assertThat(settings.applicationFee().signum()).isZero();
        fee("-5", null);
        assertThat(settings.applicationFee().signum()).isZero();
        fee(null, null);
        assertThat(settings.applicationFee().signum()).isZero();
    }

    @Test
    void shouldFailClosed_whenTheFeeIsGarbled() {
        fee("12O", null);

        assertThatThrownBy(settings::applicationFee).isInstanceOf(IllegalStateException.class);
    }
}
