package com.anujbohre.uqlab.auth;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Registration, login and token checking, using the real context, a real database and real tokens
 * (no shortcuts). Each test makes its own user, so tests do not depend on each other.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    private static final String PASSWORD = "correct horse battery";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository users;

    @Autowired
    private JwtEncoder encoder;

    private static String newUsername() {
        return "u" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private static String json(String username, String password) {
        return "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
    }

    private MvcResult register(String username, String password) throws Exception {
        return mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(json(username, password)))
                .andReturn();
    }

    private String tokenFrom(MvcResult result) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    // ------------------------------------------------------------------ registration

    @Test
    void registrationCreatesTheAccountAndSignsItIn() throws Exception {
        String name = newUsername();

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(json(name, PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInSeconds").value(3600))
                .andExpect(jsonPath("$.username").value(name));
    }

    @Test
    void usernamesAreNotCaseSensitive() throws Exception {
        String name = newUsername();
        assertEquals(201, register(name, PASSWORD).getResponse().getStatus());

        MvcResult again = register(name.toUpperCase(), PASSWORD);

        assertEquals(409, again.getResponse().getStatus());
    }

    @Test
    void duplicateUsernameIsRefusedWithConflict() throws Exception {
        String name = newUsername();
        register(name, PASSWORD);

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(json(name, PASSWORD)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Username already taken"));
    }

    @Test
    void thePasswordIsStoredOnlyAsABcryptHash() throws Exception {
        String name = newUsername();
        register(name, PASSWORD);

        String stored = users.findByUsername(name).orElseThrow().getPasswordHash();

        assertNotEquals(PASSWORD, stored);
        assertTrue(stored.startsWith("$2"), "BCrypt hashes start with $2");
        assertEquals(60, stored.length());
    }

    @Test
    void badRegistrationDataGivesBadRequestWithFieldMessages() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(json("ab", "short")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.username").exists())
                .andExpect(jsonPath("$.errors.password").exists());

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("bad name!", PASSWORD)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.username").exists());
    }

    @Test
    void passwordLongerThan72BytesIsRefused() throws Exception {
        // 40 characters but 80 bytes in UTF-8: passes the character-count check, fails the byte check.
        String longInBytes = "é".repeat(40);

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(newUsername(), longInBytes)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid registration"));
    }

    // ------------------------------------------------------------------ login

    @Test
    void loginReturnsATokenThatOpensProtectedEndpoints() throws Exception {
        String name = newUsername();
        register(name, PASSWORD);

        MvcResult login = mvc.perform(
                        post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(json(name, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(name))
                .andReturn();

        // The token travels in the Authorization header, exactly as the React page will send it.
        mvc.perform(get("/api/models").header("Authorization", "Bearer " + tokenFrom(login)))
                .andExpect(status().isOk());
    }

    @Test
    void loginIgnoresCaseAndSurroundingSpacesInTheUsername() throws Exception {
        String name = newUsername();
        register(name, PASSWORD);

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("  " + name.toUpperCase() + " ", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(name));
    }

    @Test
    void wrongPasswordAndUnknownUserGiveTheSameAnswer() throws Exception {
        String name = newUsername();
        register(name, PASSWORD);

        String wrongPassword = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(name, "not the password")))
                .andExpect(status().isUnauthorized())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(newUsername(), PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value(equalTo("Invalid username or password")));

        assertTrue(wrongPassword.contains("Invalid username or password"));
    }

    @Test
    void loginWithMissingFieldsIsABadRequest() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.username").exists())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void meReturnsTheSignedInUser() throws Exception {
        String name = newUsername();
        MvcResult registered = register(name, PASSWORD);

        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + tokenFrom(registered)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(name))
                .andExpect(jsonPath("$.id").value(users.findByUsername(name).orElseThrow().getId()));
    }

    // ------------------------------------------------------------------ token checking

    @Test
    void aTamperedTokenIsRejected() throws Exception {
        String token = tokenFrom(register(newUsername(), PASSWORD));
        // Change one character of the signature (the last part of the token).
        char last = token.charAt(token.length() - 2);
        String tampered = token.substring(0, token.length() - 2) + (last == 'A' ? 'B' : 'A') + token.charAt(token.length() - 1);

        mvc.perform(get("/api/models").header("Authorization", "Bearer " + tampered))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anExpiredTokenIsRejected() throws Exception {
        Instant now = Instant.now();
        String expired = sign(encoder, "someone", TokenService.ISSUER, now.minus(Duration.ofHours(2)), now.minus(Duration.ofHours(1)));

        mvc.perform(get("/api/models").header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void aTokenFromAnotherIssuerIsRejected() throws Exception {
        Instant now = Instant.now();
        String foreign = sign(encoder, "someone", "somebody-else", now, now.plus(Duration.ofHours(1)));

        mvc.perform(get("/api/models").header("Authorization", "Bearer " + foreign))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void aTokenSignedWithAnotherKeyIsRejected() throws Exception {
        byte[] otherKey = "a-completely-different-secret-key-0123456789".getBytes(StandardCharsets.UTF_8);
        JwtEncoder otherEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(new SecretKeySpec(otherKey, "HmacSHA256")));
        Instant now = Instant.now();
        String forged = sign(otherEncoder, "someone", TokenService.ISSUER, now, now.plus(Duration.ofHours(1)));

        mvc.perform(get("/api/models").header("Authorization", "Bearer " + forged))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void noTokenAndGarbageTokenAreBothRejected() throws Exception {
        mvc.perform(get("/api/models")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/models").header("Authorization", "Bearer not.a.token"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/models").header("Authorization", "Basic dXNlcjpwYXNz"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void theTokenNeverContainsThePassword() throws Exception {
        String token = tokenFrom(register(newUsername(), PASSWORD));

        // A JWT's parts are only Base64-encoded, so anyone can read them; the password must not be there.
        String payload = new String(java.util.Base64.getUrlDecoder().decode(token.split("\\.")[1]), StandardCharsets.UTF_8);
        org.hamcrest.MatcherAssert.assertThat(payload, not(containsString(PASSWORD)));
        org.hamcrest.MatcherAssert.assertThat(payload, containsString("\"iss\":\"uq-lab\""));
        org.hamcrest.MatcherAssert.assertThat(token, startsWith("eyJ")); // Base64 of {"alg":...
    }

    private static String sign(JwtEncoder with, String subject, String issuer, Instant issuedAt, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(subject)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();
        return with.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }
}
