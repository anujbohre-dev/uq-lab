package com.anujbohre.uqlab.auth;

/** Registration data that the field annotations cannot check on their own. Mapped to HTTP 400. */
public class InvalidRegistrationException extends RuntimeException {

    public InvalidRegistrationException(String message) {
        super(message);
    }
}
