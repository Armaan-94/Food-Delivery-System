package com.fooddelivery.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Set;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

class JwtTokenServiceTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";

    private final JwtSecurityAutoConfiguration config = new JwtSecurityAutoConfiguration();
    private JwtProperties properties;
    private JwtEncoder encoder;
    private JwtDecoder decoder;
    private JwtTokenService tokenService;

    @BeforeEach
    void setUp() {
        properties = new JwtProperties();
        properties.setSecret(SECRET);
        SecretKey key = config.jwtSecretKey(properties);
        encoder = config.jwtEncoder(key);
        decoder = config.jwtDecoder(key, properties);
        tokenService = config.jwtTokenService(encoder, properties);
    }

    @Test
    void userTokenCarriesIdentityAndRole() {
        String token = tokenService.issueUserToken(42L, "a@example.com", "Asha", Role.ADMIN);

        Jwt jwt = decoder.decode(token);
        assertThat(jwt.getSubject()).isEqualTo("42");
        assertThat(jwt.getClaimAsString(JwtClaims.EMAIL)).isEqualTo("a@example.com");
        assertThat(jwt.getClaimAsString(JwtClaims.NAME)).isEqualTo("Asha");
        assertThat(jwt.getClaimAsString(JwtClaims.ROLE)).isEqualTo("ADMIN");
        assertThat(jwt.getExpiresAt()).isAfter(Instant.now().plusSeconds(59 * 60));

        AuthenticatedUser user = AuthenticatedUser.from(jwt);
        assertThat(user.id()).isEqualTo(42L);
        assertThat(user.isAdmin()).isTrue();
    }

    @Test
    void serviceTokenHasServiceRoleAndNoUserId() {
        Jwt jwt = decoder.decode(tokenService.issueServiceToken("auth-service"));

        assertThat(jwt.getSubject()).isEqualTo("auth-service");
        AuthenticatedUser caller = AuthenticatedUser.from(jwt);
        assertThat(caller.role()).isEqualTo(Role.SERVICE);
        assertThat(caller.id()).isNull();
        assertThat(jwt.getExpiresAt()).isBefore(Instant.now().plusSeconds(180));
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        JwtProperties other = new JwtProperties();
        other.setSecret("ffffffffffffffffffffffffffffffff");
        JwtTokenService foreign = config.jwtTokenService(config.jwtEncoder(config.jwtSecretKey(other)), other);

        String forged = foreign.issueUserToken(1L, "x@example.com", "X", Role.ADMIN);

        assertThatThrownBy(() -> decoder.decode(forged)).isInstanceOf(BadJwtException.class);
    }

    @Test
    void tamperedPayloadIsRejected() {
        String token = tokenService.issueUserToken(1L, "u@example.com", "U", Role.USER);
        String[] parts = token.split("\\.");
        String adminPayload = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                ("{\"sub\":\"1\",\"role\":\"ADMIN\",\"iss\":\"food-delivery\",\"exp\":9999999999}").getBytes());

        assertThatThrownBy(() -> decoder.decode(parts[0] + "." + adminPayload + "." + parts[2]))
                .isInstanceOf(BadJwtException.class);
    }

    @Test
    void expiredTokenIsRejected() {
        Instant past = Instant.now().minusSeconds(600);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.getIssuer()).subject("1")
                .issuedAt(past.minusSeconds(60)).expiresAt(past)
                .claim(JwtClaims.ROLE, "USER").build();
        String expired = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();

        assertThatThrownBy(() -> decoder.decode(expired)).isInstanceOf(BadJwtException.class);
    }

    @Test
    void tokenFromAnotherIssuerIsRejected() {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("someone-else").subject("1")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300))
                .claim(JwtClaims.ROLE, "USER").build();
        String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(BadJwtException.class);
    }

    @Test
    void shortSecretFailsValidation() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        JwtProperties weak = new JwtProperties();
        weak.setSecret("too-short");

        Set<ConstraintViolation<JwtProperties>> violations = validator.validate(weak);

        assertThat(violations).extracting(v -> v.getPropertyPath().toString()).contains("secret");
        assertThat(validator.validate(properties)).isEmpty();
    }
}
