package com.anujbohre.uqlab.job;

import com.anujbohre.uqlab.api.dto.JobResponse;
import com.anujbohre.uqlab.api.dto.SimulationRequest;
import com.anujbohre.uqlab.api.dto.SimulationResponse;
import com.anujbohre.uqlab.service.InvalidSimulationRequestException;
import com.anujbohre.uqlab.service.SimulationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Runs simulations in the background. {@link #submit} validates and stores the job, queues it on
 * the executor and returns at once; a worker thread then moves it QUEUED, RUNNING, DONE or FAILED.
 */
@Service
public class JobService {

    private static final Logger log = LoggerFactory.getLogger(JobService.class);

    static final int MAX_PAGE_SIZE = 100;

    private final JobStore store;
    private final SimulationService simulationService;
    private final ObjectMapper objectMapper;
    private final ExecutorService executor;
    private final Clock clock;

    public JobService(
            JobStore store,
            SimulationService simulationService,
            ObjectMapper objectMapper,
            @Qualifier("jobExecutor") ExecutorService executor,
            Clock clock) {
        this.store = store;
        this.simulationService = simulationService;
        this.objectMapper = objectMapper;
        this.executor = executor;
        this.clock = clock;
    }

    /**
     * Validates the request, stores a QUEUED job owned by {@code ownerId} and schedules it.
     * Returns before the run starts.
     */
    public JobResponse submit(String ownerId, SimulationRequest request) {
        // Bad requests fail now with 400/404 instead of becoming FAILED jobs later.
        simulationService.validate(request);

        SimulationJob job = new SimulationJob(
                UUID.randomUUID().toString(), ownerId, request.modelId(), toJson(request), clock.instant());
        store.save(job);
        JobResponse queued = toResponse(job); // built before the worker can change anything

        try {
            executor.execute(() -> execute(job.getId(), request));
        } catch (RejectedExecutionException e) {
            job.markFailed(clock.instant(), "Rejected: the job queue was full");
            store.save(job);
            throw new JobQueueFullException();
        }
        return queued;
    }

    /**
     * Returns one of the caller's jobs. Someone else's job gives the same "not found" as a job that
     * does not exist, so the response never reveals which ids are in use.
     */
    public JobResponse get(String ownerId, String id) {
        return store.findByIdAndOwner(id, ownerId)
                .map(this::toResponse)
                .orElseThrow(() -> new JobNotFoundException(id));
    }

    /** Lists the caller's jobs, newest first. Pages are numbered from 0; size is 1 to 100. */
    public PageResult<JobResponse> list(String ownerId, JobStatus status, int page, int size) {
        if (page < 0) {
            throw new InvalidSimulationRequestException("page must be 0 or greater");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidSimulationRequestException("size must be between 1 and " + MAX_PAGE_SIZE);
        }
        return store.findPage(ownerId, status, page, size).map(this::toResponse);
    }

    /** Runs on a worker thread. Never throws: any failure is recorded on the job. */
    void execute(String id, SimulationRequest request) {
        SimulationJob job = store.findById(id).orElse(null);
        if (job == null) {
            log.warn("Job {} vanished before it could run", id);
            return;
        }
        job.markRunning(clock.instant());
        store.save(job);
        try {
            SimulationResponse result = simulationService.run(request);
            job.markDone(clock.instant(), toJson(result));
        } catch (Exception e) {
            // Deliberately broad: a worker thread must record every failure, not die silently.
            log.warn("Job {} failed: {}", id, e.getMessage());
            job.markFailed(clock.instant(), e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        }
        store.save(job);
    }

    /**
     * Jobs that were QUEUED or RUNNING when the application last stopped will never finish, because
     * the queue lives in memory. Mark them FAILED at start-up so clients are not left waiting.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void failInterruptedJobs() {
        List<SimulationJob> stale = store.findByStatusIn(List.of(JobStatus.QUEUED, JobStatus.RUNNING));
        for (SimulationJob job : stale) {
            job.markFailed(clock.instant(), "Interrupted by an application restart");
            store.save(job);
        }
        if (!stale.isEmpty()) {
            log.info("Marked {} interrupted job(s) as FAILED", stale.size());
        }
    }

    private JobResponse toResponse(SimulationJob job) {
        SimulationRequest request = fromJson(job.getRequestJson(), SimulationRequest.class);
        SimulationResponse result =
                job.getResultJson() == null ? null : fromJson(job.getResultJson(), SimulationResponse.class);
        return new JobResponse(
                job.getId(),
                job.getModelId(),
                job.getStatus(),
                job.getCreatedAt(),
                job.getStartedAt(),
                job.getFinishedAt(),
                job.getErrorMessage(),
                request,
                result);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialise " + value.getClass().getSimpleName(), e);
        }
    }

    private <T> T fromJson(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Stored JSON cannot be read as " + type.getSimpleName(), e);
        }
    }
}
