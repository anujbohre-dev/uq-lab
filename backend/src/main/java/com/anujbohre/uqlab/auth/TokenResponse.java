package com.anujbohre.uqlab.auth;

/**
 * Returned by register and login. The client sends {@code accessToken} on every later request in
 * the header {@code Authorization: Bearer <accessToken>}.
 */
public record TokenResponse(String accessToken, String tokenType, long expiresInSeconds, String username) {}
