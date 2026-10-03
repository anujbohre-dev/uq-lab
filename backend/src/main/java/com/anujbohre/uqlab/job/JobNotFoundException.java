package com.anujbohre.uqlab.job;

/** Thrown when no job has the requested id (HTTP 404). */
public class JobNotFoundException extends RuntimeException {
    public JobNotFoundException(String id) {
        super("No job with id '" + id + "'");
    }
}
