package com.anujbohre.uqlab.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/**
 * Creates signed login tokens (JWTs). A JWT is three Base64 parts: a header, a set of claims and a
 * signature made with the server's secret. Anyone can read the claims, but only the server can
 * produce a valid signature, so a token that verifies must have been issued by this server.
 */
@Service
public class TokenService {

    /** Who issued the token. The decoder rejects tokens that name a different issuer. */
    public static final String ISSUER = "uq-lab";

    private final JwtEncoder encoder;
    private final Clock clock;
    private final Duration lifetime;

    public TokenService(
            JwtEncoder encoder, Clock clock, @Value("${uqlab.security.token-ttl-minutes:60}") long ttlMinutes) {
        this.encoder = encoder;
        this.clock = clock;
        this.lifetime = Duration.ofMinutes(ttlMinutes);
    }

    /** Issues a token whose subject is the user's id and which expires after the configured time. */
    public TokenResponse issue(AppUser user) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .issuedAt(now)
                .expiresAt(now.plus(lifetime))
                .subject(user.getId())
                .claim("username", user.getUsername())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenResponse(token, "Bearer", lifetime.toSeconds(), user.getUsername());
    }
}
