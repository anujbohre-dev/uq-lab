package com.anujbohre.uqlab.job;

/** Life cycle of an asynchronous simulation job: QUEUED, then RUNNING, then DONE or FAILED. */
public enum JobStatus {
    QUEUED,
    RUNNING,
    DONE,
    FAILED
}
