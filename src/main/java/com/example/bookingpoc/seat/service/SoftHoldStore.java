package com.example.bookingpoc.seat.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * In-memory soft-hold store. In production this would be Redis SETNX with TTL —
 * Caffeine asMap().putIfAbsent gives us the same atomic check-and-set semantics
 * locally so the POC stays single-process but the logic is identical.
 */
@Component
public class SoftHoldStore {

    public record SoftHold(Long seatId, String customerId, String token, Instant expiresAt) {}

    private final Cache<Long, SoftHold> cache;
    private final Duration ttl;

    public SoftHoldStore(@Value("${booking.seat.soft-hold-ttl-seconds:300}") long ttlSeconds) {
        this.ttl = Duration.ofSeconds(ttlSeconds);
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(ttl)
                .maximumSize(100_000)
                .build();
    }

    public Optional<SoftHold> tryAcquire(Long seatId, String customerId) {
        String token = UUID.randomUUID().toString();
        SoftHold candidate = new SoftHold(seatId, customerId, token, Instant.now().plus(ttl));
        SoftHold existing = cache.asMap().putIfAbsent(seatId, candidate);
        return existing == null ? Optional.of(candidate) : Optional.empty();
    }

    public Optional<SoftHold> peek(Long seatId) {
        return Optional.ofNullable(cache.getIfPresent(seatId));
    }

    public boolean consume(Long seatId, String token) {
        AtomicBoolean removed = new AtomicBoolean(false);
        cache.asMap().computeIfPresent(seatId, (k, hold) -> {
            if (hold.token().equals(token)) {
                removed.set(true);
                return null;
            }
            return hold;
        });
        return removed.get();
    }

    public Duration getTtl() {
        return ttl;
    }
}
