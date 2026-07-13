package io.rosecloud.notification.model;

import java.time.Duration;
import java.util.List;

/**
 * Aggregated broadcast output. {@code elapsed} is the total wall time of the
 * whole {@code broadcast()} call (distinct from {@link DeliveryResult#elapsed()}
 * which is per recipient-channel task). Results are ordered deterministically:
 * by input recipient order, then by {@code channel.name()} lexicographically.
 */
public record BroadcastResult(String notificationId, List<DeliveryResult> results, Duration elapsed) {

    public BroadcastResult {
        if (notificationId == null || notificationId.isBlank()) {
            throw new IllegalArgumentException("notificationId must not be blank");
        }
        if (elapsed == null) {
            throw new IllegalArgumentException("elapsed must not be null");
        }
        results = List.copyOf(results);
        if (results.stream().anyMatch(r -> !notificationId.equals(r.notificationId()))) {
            throw new IllegalArgumentException("broadcast result notificationId must match every delivery result");
        }
    }

    public long successCount() {
        return results.stream().filter(DeliveryResult::isSuccess).count();
    }

    public long skippedCount() {
        return results.stream().filter(DeliveryResult::isSkipped).count();
    }

    public long attemptedCount() {
        return results.size() - skippedCount();
    }

    public List<DeliveryResult> failures() {
        return results.stream().filter(DeliveryResult::isFailure).toList();
    }
}
