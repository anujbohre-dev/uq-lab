package com.anujbohre.uqlab.job;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A persisted simulation job. The request and the result are stored as JSON text, which keeps the
 * table simple and independent of the simulation model.
 */
@Entity
@Table(name = "simulation_job")
public class SimulationJob {

    static final int MAX_ERROR_LENGTH = 1000;

    @Id
    @Column(length = 36)
    private String id;

    /**
     * Id of the user who submitted the job. Nullable only because jobs created before Phase 4 have
     * no owner; those jobs are not visible to anyone.
     */
    @Column(length = 36)
    private String ownerId;

    @Column(nullable = false, length = 100)
    private String modelId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobStatus status;

    @Column(nullable = false, length = 4000)
    private String requestJson;

    @Column(length = 20000)
    private String resultJson;

    @Column(length = MAX_ERROR_LENGTH)
    private String errorMessage;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant startedAt;

    private Instant finishedAt;

    /** Required by JPA. */
    protected SimulationJob() {}

    public SimulationJob(String id, String ownerId, String modelId, String requestJson, Instant createdAt) {
        this.id = id;
        this.ownerId = ownerId;
        this.modelId = modelId;
        this.requestJson = requestJson;
        this.createdAt = createdAt;
        this.status = JobStatus.QUEUED;
    }

    public void markRunning(Instant now) {
        this.status = JobStatus.RUNNING;
        this.startedAt = now;
    }

    public void markDone(Instant now, String resultJson) {
        this.status = JobStatus.DONE;
        this.resultJson = resultJson;
        this.finishedAt = now;
    }

    public void markFailed(Instant now, String message) {
        this.status = JobStatus.FAILED;
        this.errorMessage = truncate(message);
        this.finishedAt = now;
    }

    private static String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() <= MAX_ERROR_LENGTH ? message : message.substring(0, MAX_ERROR_LENGTH);
    }

    public String getId() {
        return id;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public String getModelId() {
        return modelId;
    }

    public JobStatus getStatus() {
        return status;
    }

    public String getRequestJson() {
        return requestJson;
    }

    public String getResultJson() {
        return resultJson;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }
}
