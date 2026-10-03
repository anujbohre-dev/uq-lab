package com.anujbohre.uqlab.api;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.MvcResult;

/**
 * End-to-end test of the asynchronous API: real Spring context, real thread pool and an in-memory
 * H2 database (see src/test/resources/application.properties).
 */
@SpringBootTest
@AutoConfigureMockMvc
class JobControllerTest {

    private static final double CLOSED_FORM = 151397.8353530265;

    /** Makes a request look as if user "alice" had signed in (the real token path is tested in AuthControllerTest). */
    private static final RequestPostProcessor ALICE = jwt().jwt(token -> token.subject("alice"));

    private static final RequestPostProcessor BOB = jwt().jwt(token -> token.subject("bob"));

    @Autowired
    private MockMvc mvc;

    private static String eulerBody(double diameter) {
        return """
                {
                  "modelId": "euler-column-buckling",
                  "samples": 1000,
                  "seed": 1,
                  "inputs": {
                    "youngsModulus":         { "type": "CONSTANT", "value": 200000000000 },
                    "diameter":              { "type": "CONSTANT", "value": %s },
                    "length":                { "type": "CONSTANT", "value": 2.0 },
                    "effectiveLengthFactor": { "type": "CONSTANT", "value": 1.0 }
                  }
                }
                """.formatted(diameter);
    }

    /** Submits a job and returns the URL from the Location header. */
    private String submit(String body) throws Exception {
        MvcResult result = mvc.perform(post("/api/jobs").with(ALICE).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isAccepted())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").value(notNullValue()))
                .andExpect(jsonPath("$.status").value("QUEUED"))
                .andReturn();
        return result.getResponse().getHeader("Location");
    }

    private void awaitStatus(String location, String expected) {
        await().atMost(Duration.ofSeconds(15)).pollInterval(Duration.ofMillis(50)).untilAsserted(() ->
                mvc.perform(get(location).with(ALICE)).andExpect(jsonPath("$.status").value(expected)));
    }

    @Test
    void submittedJobRunsToCompletionWithTheClosedFormResult() throws Exception {
        String location = submit(eulerBody(0.05));

        awaitStatus(location, "DONE");

        mvc.perform(get(location).with(ALICE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.samplesUsed").value(1000))
                .andExpect(jsonPath("$.result.summary.mean").value(closeTo(CLOSED_FORM, 1e-6)))
                .andExpect(jsonPath("$.request.modelId").value("euler-column-buckling"))
                .andExpect(jsonPath("$.startedAt").value(notNullValue()))
                .andExpect(jsonPath("$.finishedAt").value(notNullValue()));
    }

    @Test
    void jobWithNoValidSamplesEndsFailed() throws Exception {
        String location = submit(eulerBody(-0.05));

        awaitStatus(location, "FAILED");

        mvc.perform(get(location).with(ALICE))
                .andExpect(jsonPath("$.errorMessage").value(notNullValue()))
                .andExpect(jsonPath("$.result").doesNotExist());
    }

    @Test
    void newModelsCanBeRunAsJobs() throws Exception {
        String body = """
                {
                  "modelId": "spring-mass-natural-frequency",
                  "samples": 1000,
                  "seed": 1,
                  "inputs": {
                    "stiffness": { "type": "CONSTANT", "value": 1000 },
                    "mass":      { "type": "CONSTANT", "value": 1 }
                  }
                }
                """;
        String location = submit(body);

        awaitStatus(location, "DONE");

        mvc.perform(get(location).with(ALICE))
                .andExpect(jsonPath("$.result.outputUnit").value("Hz"))
                .andExpect(jsonPath("$.result.summary.mean").value(closeTo(5.032921210448704, 1e-9)));
    }

    @Test
    void unknownJobIdGivesNotFound() throws Exception {
        mvc.perform(get("/api/jobs/does-not-exist").with(ALICE)).andExpect(status().isNotFound());
    }

    @Test
    void invalidBodyIsRejectedImmediately() throws Exception {
        mvc.perform(post("/api/jobs").with(ALICE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"modelId\":\"euler-column-buckling\",\"samples\":5,\"inputs\":{}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.samples").exists());
    }

    @Test
    void unknownModelIsRejectedImmediately() throws Exception {
        mvc.perform(post("/api/jobs").with(ALICE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eulerBody(0.05).replace("euler-column-buckling", "nope")))
                .andExpect(status().isNotFound());
    }

    @Test
    void listingIsPaginatedAndFilterable() throws Exception {
        for (int i = 0; i < 3; i++) {
            awaitStatus(submit(eulerBody(0.05)), "DONE");
        }

        mvc.perform(get("/api/jobs").with(ALICE).param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalItems").value(greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.totalPages").value(greaterThanOrEqualTo(2)));

        mvc.perform(get("/api/jobs").with(ALICE).param("status", "DONE").param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].status").value("DONE"));
    }

    @Test
    void badPagingParametersGiveBadRequest() throws Exception {
        mvc.perform(get("/api/jobs").with(ALICE).param("size", "0")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/jobs").with(ALICE).param("size", "101")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/jobs").with(ALICE).param("page", "-1")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/jobs").with(ALICE).param("status", "BOGUS")).andExpect(status().isBadRequest());
    }

    @Test
    void anotherUserCannotReadMyJob() throws Exception {
        String location = submit(eulerBody(0.05));
        awaitStatus(location, "DONE");

        // 404, not 403: a stranger must not learn that the id exists.
        mvc.perform(get(location).with(BOB)).andExpect(status().isNotFound());
        mvc.perform(get(location).with(ALICE)).andExpect(status().isOk());
    }

    @Test
    void listingShowsOnlyMyOwnJobs() throws Exception {
        awaitStatus(submit(eulerBody(0.05)), "DONE"); // submitted by alice

        mvc.perform(get("/api/jobs").with(BOB).param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(0))
                .andExpect(jsonPath("$.items", hasSize(0)));
    }
}
