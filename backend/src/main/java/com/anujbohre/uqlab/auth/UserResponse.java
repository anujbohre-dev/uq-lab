package com.anujbohre.uqlab.auth;

/** The signed-in user, as returned by GET /api/auth/me. */
public record UserResponse(String id, String username) {}
