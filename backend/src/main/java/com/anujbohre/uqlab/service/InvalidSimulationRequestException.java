package com.anujbohre.uqlab.service;

/** The request is well-formed JSON but its content does not make sense. Mapped to HTTP 400. */
public class InvalidSimulationRequestException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidSimulationRequestException(String message) {
        super(message);
    }

    public InvalidSimulationRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
