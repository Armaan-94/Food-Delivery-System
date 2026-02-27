package com.fooddelivery.orderservice.config;

import java.io.IOException;

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
public class OrderServiceFilter extends OncePerRequestFilter {

    @Autowired
    @Qualifier("configJdbcTemplate")
    private JdbcTemplate configJdbcTemplate;

    private String getSharedSecretFromDB() {
        try {
            String sql = "SELECT config_value FROM service_config WHERE config_key = ?";
            return configJdbcTemplate.queryForObject(sql, String.class, "apigateway.shared.secret.orderservice");
        } catch (EmptyResultDataAccessException e) {
            throw new RuntimeException("Missing config key 'apigateway.shared.secret.orderservice' in database", e);
        }
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String requestSecret = request.getHeader("X-API-GATEWAY-SECRET");
        
        try {
            String dbSecret = getSharedSecretFromDB();
            if (requestSecret == null || !dbSecret.equals(requestSecret)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or missing API Gateway secret");
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