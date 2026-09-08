package com.nadoumi.identity.service.otp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ruoyi.common.core.redis.RedisCache;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class TicketServiceTest {

    private final RedisCache redis = mock(RedisCache.class);
    private final TicketService tickets = new TicketService(redis);

    @Test
    void mint_stores_the_lower_cased_email_under_a_purpose_scoped_key_with_a_ttl() {
        String id = tickets.mint("Ada@Example.com", OtpPurpose.REGISTER);

        assertThat(id).startsWith("tkt_");
        verify(redis).setCacheObject(eq("nad:ticket:REGISTER:" + id), eq("ada@example.com"),
                eq(TicketService.TTL_SECONDS), eq(TimeUnit.SECONDS));
    }

    @Test
    void consume_atomically_gets_and_deletes_the_key() {
        when(redis.<String>getAndDelete(startsWith("nad:ticket:REGISTER:"))).thenReturn("a@x.com");

        assertThat(tickets.consume("tkt_abc", OtpPurpose.REGISTER)).isEqualTo("a@x.com");
        verify(redis).getAndDelete("nad:ticket:REGISTER:tkt_abc");
    }

    @Test
    void consume_throws_when_the_ticket_is_gone() {
        when(redis.getAndDelete(any())).thenReturn(null);

        assertThatThrownBy(() -> tickets.consume("tkt_x", OtpPurpose.PASSWORD_RESET))
                .isInstanceOf(OtpException.class)
                .hasMessageContaining("ticket");
    }
}
