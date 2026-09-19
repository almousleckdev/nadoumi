package com.ruoyi.framework.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.filter.CorsFilter;

/**
 * nadoumi-admin sends every request with withCredentials/cookies (the admin session
 * cookie), which requires Access-Control-Allow-Credentials: true on the response or
 * the browser blocks the preflight — this regressed once already (missing
 * allowCredentials) and broke the deployed admin console.
 */
class ResourcesConfigCorsTest {

    private final CorsFilter corsFilter = new ResourcesConfig().corsFilter();

    @Test
    void allowsCredentials_soCrossOriginRequestsWithCookiesSucceed() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/getInfo");
        request.addHeader("Origin", "https://dashboard.nadoumi.com");
        request.addHeader("Access-Control-Request-Method", "GET");
        MockHttpServletResponse response = new MockHttpServletResponse();

        corsFilter.doFilter(request, response, (req, res) -> { });

        assertThat(response.getHeader("Access-Control-Allow-Credentials")).isEqualTo("true");
        assertThat(response.getHeader("Access-Control-Allow-Origin")).isEqualTo("https://dashboard.nadoumi.com");
    }
}
