package com.anujbohre.uqlab.auth;

/**
 * Login failed. The message is deliberately the same for an unknown user and a wrong password, so
 * that an attacker cannot learn which usernames exist. Mapped to HTTP 401.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid username or password");
    }
}
