package com.devalgas.productpage.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProductAggregationControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void bothSuccessEndpointsReturnTheSameBody() throws Exception {
        String completableBody = mockMvc.perform(get("/api/products/keyboard-42/completable-future"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Orchestration-Strategy", "completable-future"))
                .andReturn().getResponse().getContentAsString();

        String structuredBody = mockMvc.perform(get("/api/products/keyboard-42/structured-concurrency"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Orchestration-Strategy", "structured-concurrency"))
                .andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(structuredBody).isEqualTo(completableBody);
    }

    @Test
    void bothFailureEndpointsReturnTheSameProblemContract() throws Exception {
        for (String strategy : new String[]{"completable-future", "structured-concurrency"}) {
            mockMvc.perform(get("/api/products/keyboard-42/" + strategy).param("scenario", "one-failure"))
                    .andExpect(status().isBadGateway())
                    .andExpect(jsonPath("$.code").value("UPSTREAM_FAILURE"));
        }
    }

    @Test
    void bothTimeoutEndpointsReturnGatewayTimeout() throws Exception {
        for (String strategy : new String[]{"completable-future", "structured-concurrency"}) {
            mockMvc.perform(get("/api/products/keyboard-42/" + strategy).param("scenario", "slow-service"))
                    .andExpect(status().isGatewayTimeout())
                    .andExpect(jsonPath("$.code").value("GLOBAL_DEADLINE_EXCEEDED"));
        }
    }

    @Test
    void invalidScenarioReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/products/keyboard-42/completable-future").param("scenario", "unknown"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }
}
