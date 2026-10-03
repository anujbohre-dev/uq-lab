package com.anujbohre.uqlab.api;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Starts the whole application context and calls the API through MockMvc (no real network). */
@SpringBootTest
@AutoConfigureMockMvc
class SimulationControllerTest {

    private static final double CLOSED_FORM = 151397.8353530265;

    /** Makes a request look as if user "alice" had signed in (the real token path is tested in AuthControllerTest). */
    private static final RequestPostProcessor ALICE = jwt().jwt(token -> token.subject("alice"));

    @Autowired
    private MockMvc mvc;

    private static final String CERTAIN_INPUTS = """
            {
              "modelId": "euler-column-buckling",
              "samples": 1000,
              "seed": 1,
              "inputs": {
                "youngsModulus":         { "type": "CONSTANT", "value": 200000000000 },
                "diameter":              { "type": "CONSTANT", "value": 0.05 },
                "length":                { "type": "CONSTANT", "value": 2.0 },
                "effectiveLengthFactor": { "type": "CONSTANT", "value": 1.0 }
              }
            }
            """;

    @Test
    void listsTheThreeModels() throws Exception {
        mvc.perform(get("/api/models").with(ALICE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[?(@.id == 'euler-column-buckling')]", hasSize(1)))
                .andExpect(jsonPath("$[?(@.id == 'cantilever-tip-deflection')]", hasSize(1)))
                .andExpect(jsonPath("$[?(@.id == 'spring-mass-natural-frequency')]", hasSize(1)));
    }

    @Test
    void zeroSpreadRunReturnsTheClosedFormValue() throws Exception {
        mvc.perform(post("/api/simulations").with(ALICE).contentType(MediaType.APPLICATION_JSON).content(CERTAIN_INPUTS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.samplesUsed").value(1000))
                .andExpect(jsonPath("$.seed").value(1))
                .andExpect(jsonPath("$.summary.mean").value(closeTo(CLOSED_FORM, 1e-6)))
                .andExpect(jsonPath("$.summary.stdDev").value(closeTo(0.0, 1e-9)))
                .andExpect(jsonPath("$.percentiles.p50").value(closeTo(CLOSED_FORM, 1e-6)));
    }

    @Test
    void uncertainRunReturnsStatisticsAndHistogram() throws Exception {
        String body = """
                {
                  "modelId": "euler-column-buckling",
                  "samples": 5000,
                  "seed": 42,
                  "bins": 10,
                  "inputs": {
                    "youngsModulus":         { "type": "LOGNORMAL", "mean": 200000000000, "stdDev": 10000000000 },
                    "diameter":              { "type": "NORMAL", "mean": 0.05, "stdDev": 0.0005 },
                    "length":                { "type": "NORMAL", "mean": 2.0, "stdDev": 0.01 },
                    "effectiveLengthFactor": { "type": "UNIFORM", "min": 0.9, "max": 1.1 }
                  }
                }
                """;
        mvc.perform(post("/api/simulations").with(ALICE).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.mean").value(closeTo(CLOSED_FORM, 6000.0)))
                .andExpect(jsonPath("$.histogram.counts", hasSize(10)))
                .andExpect(jsonPath("$.histogram.binEdges", hasSize(11)));
    }

    @Test
    void omittedSeedIsChosenAndReturned() throws Exception {
        String body = CERTAIN_INPUTS.replace("\"seed\": 1,", "");
        mvc.perform(post("/api/simulations").with(ALICE).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seed").isNumber());
    }

    @Test
    void tooFewSamplesIsRejectedWithFieldError() throws Exception {
        String body = CERTAIN_INPUTS.replace("\"samples\": 1000", "\"samples\": 5");
        mvc.perform(post("/api/simulations").with(ALICE).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.samples").exists());
    }

    @Test
    void unknownModelGivesNotFound() throws Exception {
        String body = CERTAIN_INPUTS.replace("euler-column-buckling", "no-such-model");
        mvc.perform(post("/api/simulations").with(ALICE).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void missingDistributionParameterIsABadRequest() throws Exception {
        String body = CERTAIN_INPUTS.replace(
                "{ \"type\": \"CONSTANT\", \"value\": 0.05 }", "{ \"type\": \"NORMAL\", \"mean\": 0.05 }");
        mvc.perform(post("/api/simulations").with(ALICE).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingInputIsABadRequest() throws Exception {
        String body = CERTAIN_INPUTS.replace(
                "\"diameter\":              { \"type\": \"CONSTANT\", \"value\": 0.05 },", "");
        mvc.perform(post("/api/simulations").with(ALICE).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void malformedJsonIsABadRequest() throws Exception {
        mvc.perform(post("/api/simulations").with(ALICE).contentType(MediaType.APPLICATION_JSON).content("{ not json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownDistributionTypeIsABadRequest() throws Exception {
        String body = CERTAIN_INPUTS.replace("\"type\": \"CONSTANT\", \"value\": 2.0", "\"type\": \"WEIBULL\", \"value\": 2.0");
        mvc.perform(post("/api/simulations").with(ALICE).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void allSamplesNonPhysicalGivesUnprocessableEntity() throws Exception {
        String body = CERTAIN_INPUTS.replace("\"value\": 2.0", "\"value\": -2.0");
        mvc.perform(post("/api/simulations").with(ALICE).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnprocessableEntity());
    }
}
