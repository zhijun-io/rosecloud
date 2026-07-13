package io.rosecloud.notification.core;

import io.rosecloud.notification.model.ChannelType;

import java.time.Duration;
import java.util.List;

/**
 * Fixed delivery policy. Construct via {@link #fanOut()} / {@link #failover(List)}
 * builders (named setters) to avoid positional mix-ups of {@code Duration}/{@code int}
 * arguments; the canonical constructor is not intended for direct use.
 *
 * <p>{@code FAN_OUT}: all deliverable channels are attempted concurrently.
 * {@code FAILOVER}: candidate channels are tried serially in {@code failoverOrder};
 * the first success short-circuits the rest.
 */
public record DeliveryPolicy(
        Mode mode,
        List<ChannelType> failoverOrder,
        Duration deadline,
        Duration acquireTimeout,
        int maxAttempts,
        Duration baseBackoff,
        int maxConcurrency) {

    public DeliveryPolicy {
        if (mode == null) {
            throw new IllegalArgumentException("mode must not be null");
        }
        if (failoverOrder == null) {
            throw new IllegalArgumentException("failoverOrder must not be null");
        }
        if (deadline == null) {
            throw new IllegalArgumentException("deadline must not be null");
        }
        if (acquireTimeout == null) {
            throw new IllegalArgumentException("acquireTimeout must not be null");
        }
        if (baseBackoff == null) {
            throw new IllegalArgumentException("baseBackoff must not be null");
        }
        failoverOrder = List.copyOf(failoverOrder);
        if (mode == Mode.FAILOVER && failoverOrder.isEmpty()) {
            throw new IllegalArgumentException("failoverOrder must not be empty in FAILOVER mode");
        }
        if (maxAttempts <= 0) {
            throw new IllegalArgumentException("maxAttempts must be positive");
        }
        if (maxConcurrency <= 0) {
            throw new IllegalArgumentException("maxConcurrency must be positive");
        }
        if (deadline.isNegative()) {
            throw new IllegalArgumentException("deadline must not be negative");
        }
        if (acquireTimeout.isNegative()) {
            throw new IllegalArgumentException("acquireTimeout must not be negative");
        }
        if (baseBackoff.isNegative()) {
            throw new IllegalArgumentException("baseBackoff must not be negative");
        }
    }

    /** fan-out: all deliverable channels attempted concurrently. */
    public static Builder fanOut() {
        return new Builder(Mode.FAN_OUT, List.of());
    }

    /** failover: try {@code failoverOrder} serially, stop at first success. */
    public static Builder failover(List<ChannelType> failoverOrder) {
        return new Builder(Mode.FAILOVER, List.copyOf(failoverOrder));
    }

    public enum Mode {
        FAN_OUT,
        FAILOVER
    }

    public static final class Builder {
        private final Mode mode;
        private final List<ChannelType> failoverOrder;
        private Duration deadline;
        private Duration acquireTimeout;
        private int maxAttempts;
        private Duration baseBackoff;
        private int maxConcurrency;

        private Builder(Mode mode, List<ChannelType> failoverOrder) {
            this.mode = mode;
            this.failoverOrder = failoverOrder;
        }

        public Builder deadline(Duration deadline) { this.deadline = deadline; return this; }
        public Builder acquireTimeout(Duration acquireTimeout) { this.acquireTimeout = acquireTimeout; return this; }
        public Builder maxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; return this; }
        public Builder baseBackoff(Duration baseBackoff) { this.baseBackoff = baseBackoff; return this; }
        public Builder maxConcurrency(int maxConcurrency) { this.maxConcurrency = maxConcurrency; return this; }

        public DeliveryPolicy build() {
            return new DeliveryPolicy(mode, failoverOrder, deadline, acquireTimeout,
                    maxAttempts, baseBackoff, maxConcurrency);
        }
    }
}
