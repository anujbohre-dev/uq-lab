package com.anujbohre.uqlab.domain.montecarlo;

/** Thrown when every sample was rejected, so no statistics can be computed. */
public class NoValidSamplesException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public NoValidSamplesException(String message) {
        super(message);
    }
}
