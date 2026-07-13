package io.rosecloud.notification.core;

import io.rosecloud.notification.channel.NotificationChannel;
import io.rosecloud.notification.model.BroadcastResult;
import io.rosecloud.notification.model.ChannelType;
import io.rosecloud.notification.model.Notification;
import io.rosecloud.notification.model.Recipient;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * In-process notification delivery engine. Synchronously delivers a
 * {@link Notification} to a list of {@link Recipient}s across registered
 * channels and returns a structured {@link BroadcastResult}. Instances are
 * thread-safe and may be called concurrently; see the design doc §9.
 */
public interface NotificationEngine extends AutoCloseable {

    static Builder builder() {
        return new Builder();
    }

    /**
     * @param n          non-null notification
     * @param recipients non-null, no null elements; {@code recipientId} must be unique
     *                   within the list. An empty list returns an empty result bound to
     *                   {@code n.notificationId()}.
     */
    BroadcastResult broadcast(Notification n, List<Recipient> recipients);

    @Override
    void close();

    final class Builder {
        private ExecutorService executor;
        private boolean ownsExecutor = false;
        private DeliveryPolicy policy;
        private final Map<ChannelType, NotificationChannel> channels = new LinkedHashMap<>();

        public Builder executor(ExecutorService executor) {
            if (executor == null) {
                throw new IllegalArgumentException("executor must not be null");
            }
            this.executor = executor;
            return this;
        }

        public Builder ownsExecutor(boolean owns) {
            this.ownsExecutor = owns;
            return this;
        }

        public Builder policy(DeliveryPolicy policy) {
            if (policy == null) {
                throw new IllegalArgumentException("policy must not be null");
            }
            this.policy = policy;
            return this;
        }

        public Builder registerChannel(NotificationChannel c) {
            if (c == null) {
                throw new IllegalArgumentException("channel must not be null");
            }
            ChannelType t = c.type();
            if (t == null) {
                throw new IllegalArgumentException("channel type must not be null");
            }
            if (channels.put(t, c) != null) {
                throw new IllegalArgumentException("duplicate channel type: " + t);
            }
            return this;
        }

        public NotificationEngine build() {
            if (policy == null) {
                throw new IllegalArgumentException("policy must be set");
            }
            if (channels.isEmpty()) {
                throw new IllegalArgumentException("at least one channel must be registered");
            }
            Set<ChannelType> registered = channels.keySet();
            Set<ChannelType> seen = new HashSet<>();
            for (ChannelType t : policy.failoverOrder()) {
                if (!seen.add(t)) {
                    throw new IllegalArgumentException("duplicate failoverOrder entry: " + t);
                }
                if (!registered.contains(t)) {
                    throw new IllegalArgumentException("failoverOrder references unregistered channel: " + t);
                }
            }
            ExecutorService ex = executor;
            boolean owns = ownsExecutor;
            if (ex == null) {
                ex = Executors.newVirtualThreadPerTaskExecutor();
                owns = true;
            }
            return new DefaultNotificationEngine(policy, Map.copyOf(channels), ex, owns);
        }
    }
}
