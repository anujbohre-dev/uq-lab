package com.anujbohre.uqlab.service;

/** The requested model id does not exist. Mapped to HTTP 404. */
public class ModelNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ModelNotFoundException(String modelId) {
        super("Unknown model '" + modelId + "'");
    }
}
