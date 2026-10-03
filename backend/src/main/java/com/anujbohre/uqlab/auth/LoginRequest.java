package com.anujbohre.uqlab.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body of POST /api/auth/login. The size limits only stop absurdly large inputs. */
public record LoginRequest(
        @NotBlank(message = "username is required") @Size(max = 100, message = "username is too long") String username,
        @NotBlank(message = "password is required") @Size(max = 200, message = "password is too long")
                String password) {

    /** Keeps the password out of logs and error messages. */
    @Override
    public String toString() {
        return "LoginRequest[username=" + username + ", password=***]";
    }
}
