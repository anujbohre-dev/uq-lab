package com.anujbohre.uqlab.auth;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/** The signing secret is checked when the application starts. No Spring context needed. */
class SecurityConfigTest {

    @Test
    void aShortSecretStopsTheApplicationFromStarting() {
        assertThrows(IllegalStateException.class, () -> new SecurityConfig("too-short"));
    }

    @Test
    void aSecretOf32OrMoreCharactersIsAccepted() {
        SecurityConfig config = assertDoesNotThrow(() -> new SecurityConfig("0123456789abcdef0123456789abcdef"));
        assertNotNull(config.jwtEncoder());
        assertNotNull(config.jwtDecoder());
    }

    @Test
    void anEmptySecretFallsBackToARandomKey() {
        SecurityConfig config = assertDoesNotThrow(() -> new SecurityConfig(""));
        assertNotNull(config.jwtEncoder());
    }
}
