package com.ruoyi.nadoumi;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

/**
 * List endpoints must clamp {@code size} (PageSupport.MAX_SIZE = 100) and floor
 * {@code page} at 0, so {@code ?size=100000} cannot make PageHelper issue an
 * unbounded {@code LIMIT}. All nadoumi list services share the same clamp, so one
 * endpoint is enough to prove the mechanism.
 */
class PaginationClampTest extends AbstractNadIntegrationTest {

    @Test
    void oversizePageSizeIsClamped_andNegativePageFloored() throws Exception {
        mvc.perform(get("/api/public/universities").param("size", "100000").param("page", "-3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100))
                .andExpect(jsonPath("$.page").value(0));
    }
}
