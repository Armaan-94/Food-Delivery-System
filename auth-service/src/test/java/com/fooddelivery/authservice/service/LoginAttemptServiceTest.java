package com.fooddelivery.authservice.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fooddelivery.authservice.config.LoginProperties;
import com.fooddelivery.common.exception.TooManyAttemptsException;

class LoginAttemptServiceTest {

    private MutableClock clock;
    private LoginAttemptService service;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        service = new LoginAttemptService(new LoginProperties(3, 15), clock);
    }

    @Test
    void failuresBelowTheLimitDoNotLock() {
        service.recordFailure("a@example.com");
        service.recordFailure("a@example.com");

        assertThatCode(() -> service.assertNotLocked("a@example.com")).doesNotThrowAnyException();
    }

    @Test
    void reachingTheLimitLocksTheAccountForTheLockoutPeriod() {
        fail("a@example.com", 3);

        assertThatThrownBy(() -> service.assertNotLocked("a@example.com")).isInstanceOf(TooManyAttemptsException.class);

        clock.advance(Duration.ofMinutes(14));
        assertThatThrownBy(() -> service.assertNotLocked("a@example.com")).isInstanceOf(TooManyAttemptsException.class);

        clock.advance(Duration.ofMinutes(2));
        assertThatCode(() -> service.assertNotLocked("a@example.com")).doesNotThrowAnyException();
    }

    @Test
    void aSuccessfulLoginResetsTheCounter() {
        fail("a@example.com", 2);
        service.recordSuccess("a@example.com");
        fail("a@example.com", 2);

        assertThatCode(() -> service.assertNotLocked("a@example.com")).doesNotThrowAnyException();
    }

    @Test
    void oldFailuresExpireInsteadOfAccumulatingForever() {
        fail("a@example.com", 2);
        clock.advance(Duration.ofMinutes(20));
        fail("a@example.com", 2);

        assertThatCode(() -> service.assertNotLocked("a@example.com")).doesNotThrowAnyException();
    }

    @Test
    void anExpiredLockStartsAFreshCount() {
        fail("a@example.com", 3);
        clock.advance(Duration.ofMinutes(16));

        fail("a@example.com", 2);

        assertThatCode(() -> service.assertNotLocked("a@example.com")).doesNotThrowAnyException();
    }

    @Test
    void emailsAreTrackedIndependentlyAndCaseInsensitively() {
        fail("A@Example.com", 3);

        assertThatThrownBy(() -> service.assertNotLocked("  a@example.COM ")).isInstanceOf(TooManyAttemptsException.class);
        assertThatCode(() -> service.assertNotLocked("b@example.com")).doesNotThrowAnyException();
    }

    private void fail(String email, int times) {
        for (int i = 0; i < times; i++) {
            service.recordFailure(email);
        }
    }

    private static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant start) {
            this.now = start;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
