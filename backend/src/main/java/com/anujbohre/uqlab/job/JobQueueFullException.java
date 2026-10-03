package com.anujbohre.uqlab.job;

/** Thrown when the executor cannot accept another job (HTTP 503). */
public class JobQueueFullException extends RuntimeException {
    public JobQueueFullException() {
        super("The job queue is full; try again shortly");
    }
}
