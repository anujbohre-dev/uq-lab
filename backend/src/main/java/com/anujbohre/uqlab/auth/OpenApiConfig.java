package com.anujbohre.uqlab.auth;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Describes the token login to the Swagger UI, which then shows an "Authorize" button. Log in with
 * POST /api/auth/login, paste the accessToken into that button, and the other calls will work.
 */
@Configuration
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {}
