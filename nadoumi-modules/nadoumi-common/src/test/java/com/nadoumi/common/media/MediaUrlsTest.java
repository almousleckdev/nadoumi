package com.nadoumi.common.media;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Proxy;
import java.util.function.LongFunction;
import org.junit.jupiter.api.Test;

class MediaUrlsTest {

    private static MediaGateway gateway(LongFunction<String> publicUrl) {
        return (MediaGateway) Proxy.newProxyInstance(
                MediaGateway.class.getClassLoader(),
                new Class<?>[] {MediaGateway.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("publicUrl")) {
                        return publicUrl.apply((Long) args[0]);
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    @Test
    void shouldReturnLegacy_whenMediaIdIsNull() {
        MediaGateway media = gateway(id -> {
            throw new AssertionError("gateway must not be called");
        });

        assertThat(MediaUrls.resolve(media, null, "/legacy.png")).isEqualTo("/legacy.png");
    }

    @Test
    void shouldReturnNull_whenMediaIdAndLegacyAreNull() {
        assertThat(MediaUrls.resolve(gateway(id -> "x"), null, null)).isNull();
    }

    @Test
    void shouldReturnPublicUrl_whenMediaIdResolves() {
        MediaGateway media = gateway(id -> "https://cdn.example/" + id + ".jpg");

        assertThat(MediaUrls.resolve(media, 71L, "/legacy.png")).isEqualTo("https://cdn.example/71.jpg");
    }

    @Test
    void shouldFallBackToLegacy_whenGatewayFails() {
        MediaGateway media = gateway(id -> {
            throw new IllegalStateException("not public");
        });

        assertThat(MediaUrls.resolve(media, 71L, "/legacy.png")).isEqualTo("/legacy.png");
    }

    @Test
    void shouldReturnNull_whenGatewayFailsAndNoLegacy() {
        MediaGateway media = gateway(id -> {
            throw new IllegalStateException("not public");
        });

        assertThat(MediaUrls.resolve(media, 71L, null)).isNull();
    }
}
