package com.fooddelivery.common.security;

import org.springframework.security.oauth2.jwt.Jwt;

/** The caller as described by a validated JWT. The id is null for service tokens. */
public record AuthenticatedUser(Long id, String email, Role role) {

    public static AuthenticatedUser from(Jwt jwt) {
        Role role = Role.valueOf(jwt.getClaimAsString(JwtClaims.ROLE));
        Long id = role == Role.SERVICE ? null : Long.valueOf(jwt.getSubject());
        return new AuthenticatedUser(id, jwt.getClaimAsString(JwtClaims.EMAIL), role);
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}
