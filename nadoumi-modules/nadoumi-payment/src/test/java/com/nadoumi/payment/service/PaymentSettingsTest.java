package com.nadoumi.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nadoumi.payment.config.PaymentSettings;
import com.ruoyi.common.core.redis.RedisCache;
import org.junit.jupiter.api.Test;

class PaymentSettingsTest {

    private final RedisCache cache = mock(RedisCache.class);
    private final PaymentSettings settings = new PaymentSettings(cache);

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
}
