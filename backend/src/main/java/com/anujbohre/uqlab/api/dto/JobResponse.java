package com.anujbohre.uqlab.api.dto;

import com.anujbohre.uqlab.job.JobStatus;
import java.time.Instant;

/**
 * Description of a job. {@code result} is present only when the status is DONE and
 * {@code errorMessage} only when it is FAILED.
 */
public record JobResponse(
        String id,
        String modelId,
        JobStatus status,
        Instant createdAt,
        Instant startedAt,
        Instant finishedAt,
        String errorMessage,
        SimulationRequest request,
        SimulationResponse result) {}
