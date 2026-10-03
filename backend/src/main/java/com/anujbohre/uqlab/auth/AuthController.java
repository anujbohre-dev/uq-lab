package com.anujbohre.uqlab.auth;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Register, log in, and ask who you are. The first two are open; /me needs a token. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final TokenService tokenService;

    public AuthController(UserService userService, TokenService tokenService) {
        this.userService = userService;
        this.tokenService = tokenService;
    }

    /** Creates an account and signs it in at once. 201 with a token; 409 if the name is taken. */
    @PostMapping("/register")
    public ResponseEntity<TokenResponse> register(@Valid @RequestBody RegisterRequest request) {
        AppUser user = userService.register(request.username(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(tokenService.issue(user));
    }

    /** Checks the password and returns a token. 401 for a wrong username or password. */
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        AppUser user = userService.authenticate(request.username(), request.password());
        return tokenService.issue(user);
    }

    /** The signed-in user, read from the token itself (no database lookup). */
    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return new UserResponse(jwt.getSubject(), jwt.getClaimAsString("username"));
    }
}
