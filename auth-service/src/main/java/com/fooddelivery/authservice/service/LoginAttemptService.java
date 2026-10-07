package com.fooddelivery.authservice.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fooddelivery.authservice.config.LoginProperties;
import com.fooddelivery.common.exception.TooManyAttemptsException;

/**
 * Locks an email address out of login for a while after too many consecutive failures. State is
 * kept in memory, so it is per instance and is cleared by a restart.
 */
@Service
public class LoginAttemptService {

    private static final int MAX_TRACKED_KEYS = 10_000;

    private final LoginProperties properties;
    private final Clock clock;
    private final ConcurrentHashMap<String, Attempts> attempts = new ConcurrentHashMap<>();

    @Autowired
    public LoginAttemptService(LoginProperties properties) {
        this(properties, Clock.systemUTC());
    }

    LoginAttemptService(LoginProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public void assertNotLocked(String email) {
        Attempts current = attempts.get(key(email));
        if (current != null && current.lockedUntil != null && current.lockedUntil.isAfter(clock.instant())) {
            throw new TooManyAttemptsException("Too many failed login attempts. Try again later.");
        }
    }

    public void recordFailure(String email) {
        if (attempts.size() >= MAX_TRACKED_KEYS) {
            purgeExpired();
        }
        Instant now = clock.instant();
        attempts.compute(key(email), (k, existing) -> {
            Attempts next = existing == null || existing.isStale(now, lockout()) ? new Attempts() : existing;
            next.failures++;
            next.lastFailure = now;
            if (next.failures >= properties.maxFailedAttempts()) {
                next.lockedUntil = now.plus(lockout());
            }
            return next;
        });
    }

    public void recordSuccess(String email) {
        attempts.remove(key(email));
    }

    private void purgeExpired() {
        Instant now = clock.instant();
        attempts.entrySet().removeIf(entry -> entry.getValue().isStale(now, lockout()));
    }

    private Duration lockout() {
        return Duration.ofMinutes(properties.lockoutMinutes());
    }

    private static String key(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static final class Attempts {
        int failures;
        Instant lastFailure;
        Instant lockedUntil;

        /** A lock that has expired, or old failures that never reached the limit, no longer count. */
        boolean isStale(Instant now, Duration lockout) {
            if (lockedUntil != null) {
                return !lockedUntil.isAfter(now);
            }
            return lastFailure == null || lastFailure.plus(lockout).isBefore(now);
        }
    }
}
