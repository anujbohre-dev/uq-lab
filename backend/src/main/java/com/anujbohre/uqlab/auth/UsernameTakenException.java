package com.anujbohre.uqlab.auth;

/** Registration failed because the username already exists. Mapped to HTTP 409. */
public class UsernameTakenException extends RuntimeException {

    public UsernameTakenException(String username) {
        super("The username '" + username + "' is already taken");
    }
}
