package com.fooddelivery.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class DeliveryServiceFilter extends OncePerRequestFilter {

    @Autowired
    @Qualifier("configJdbcTemplate")
    private JdbcTemplate configJdbcTemplate;

    private String getDbUsername() {
        try {
            String sql = "SELECT config_value FROM service_config WHERE config_key = ?";
            return configJdbcTemplate.queryForObject(sql, String.class, "security.user.name.delivery-service");
        } catch (EmptyResultDataAccessException e) {
            throw new RuntimeException("Missing config key 'security.user.name.delivery-service' in database", e);
        }
    }
    
    private String getDbPassword() {
        try {
            String sql = "SELECT config_value FROM service_config WHERE config_key = ?";
            String passWithNoop = configJdbcTemplate.queryForObject(sql, String.class, "security.user.password.delivery-service");
            if (passWithNoop == null) throw new RuntimeException("Missing config key 'security.user.password.delivery-service' in database");
            return passWithNoop.replace("{noop}", "");
        } catch (EmptyResultDataAccessException e) {
            throw new RuntimeException("Missing config key 'security.user.password.delivery-service' in database", e);
        }
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Unauthorized: Missing Service Authorization Header");
            return;
        }

        try {
            String base64Credentials = authHeader.substring("Basic ".length()).trim();
            byte[] decodedBytes = Base64.getDecoder().decode(base64Credentials);
            String decodedString = new String(decodedBytes, StandardCharsets.UTF_8);
            String[] parts = decodedString.split(":", 2);
            String username = parts[0];
            String password = parts.length > 1 ? parts[1] : "";

            String dbUsername = getDbUsername();
            String dbPassword = getDbPassword();

            if (!dbUsername.equals(username) || !dbPassword.equals(password)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Unauthorized: Invalid Service Credentials");
                return;
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Error during service authentication: " + e.getMessage());
            return;
        }

        filterChain.doFilter(request, response);
    }
}