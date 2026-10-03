package com.anujbohre.uqlab.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Body of POST /api/auth/register. */
public record RegisterRequest(
        @NotBlank(message = "username is required")
                @Size(min = 3, max = 30, message = "username must be 3 to 30 characters")
                @Pattern(
                        regexp = "[A-Za-z0-9_.-]+",
                        message = "username may contain only letters, digits, dot, underscore and hyphen")
                String username,
        @NotBlank(message = "password is required")
                @Size(min = 8, max = 72, message = "password must be 8 to 72 characters")
                String password) {

    /** Keeps the password out of logs and error messages. */
    @Override
    public String toString() {
        return "RegisterRequest[username=" + username + ", password=***]";
    }
}
