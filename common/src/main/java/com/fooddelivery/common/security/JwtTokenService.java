package com.fooddelivery.common.security;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

/**
 * Issues the signed tokens that every service validates. Only auth-service issues user tokens;
 * service tokens are short-lived and used for internal calls.
 */
public class JwtTokenService {

    private final JwtEncoder encoder;
    private final JwtProperties properties;

    public JwtTokenService(JwtEncoder encoder, JwtProperties properties) {
        this.encoder = encoder;
        this.properties = properties;
    }

    public String issueUserToken(Long userId, String email, String name, Role role) {
        JwtClaimsSet claims = baseClaims(String.valueOf(userId), Duration.ofMinutes(properties.getAccessTokenMinutes()))
                .claim(JwtClaims.EMAIL, email)
                .claim(JwtClaims.NAME, name)
                .claim(JwtClaims.ROLE, role.name())
                .build();
        return encode(claims);
    }

    public String issueServiceToken(String serviceName) {
        JwtClaimsSet claims = baseClaims(serviceName, Duration.ofSeconds(properties.getServiceTokenSeconds()))
                .claim(JwtClaims.ROLE, Role.SERVICE.name())
                .build();
        return encode(claims);
    }

    public long accessTokenTtlSeconds() {
        return Duration.ofMinutes(properties.getAccessTokenMinutes()).toSeconds();
    }

    private JwtClaimsSet.Builder baseClaims(String subject, Duration ttl) {
        Instant now = Instant.now();
        return JwtClaimsSet.builder()
                .issuer(properties.getIssuer())
                .subject(subject)
                .id(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiresAt(now.plus(ttl));
    }

    private String encode(JwtClaimsSet claims) {
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
