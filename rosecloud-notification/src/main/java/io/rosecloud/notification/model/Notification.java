package io.rosecloud.notification.model;

import java.util.Map;
import java.util.Set;

/**
 * Channel-agnostic notification payload. The engine only cares about a stable
 * id, an opaque payload and the target channels; it does not interpret
 * {@code payload} semantics.
 *
 * <p>{@code notificationId} must be non-blank with no surrounding whitespace;
 * it is preserved case-sensitively (no normalization). Whether it is globally
 * unique across calls is the caller's decision; it only needs to be stable for
 * one broadcast and its result.
 *
 * <p>An empty {@code targetChannels} set means "use all registered channels".
 * All three fields must be non-null; "no payload / no target / no context" is
 * expressed with empty containers, never {@code null}. The containers are
 * defensively copied ({@link Map#copyOf}/{@link Set#copyOf}), so null keys,
 * elements or values are rejected as illegal input.
 */
public record Notification(
        String notificationId,
        Map<String, Object> payload,
        Set<ChannelType> targetChannels) {

    public Notification {
        if (notificationId == null || notificationId.isBlank()) {
            throw new IllegalArgumentException("notificationId must not be blank");
        }
        if (!notificationId.equals(notificationId.trim())) {
            throw new IllegalArgumentException("notificationId must not have surrounding whitespace");
        }
        if (payload == null) {
            throw new IllegalArgumentException("payload must not be null");
        }
        if (targetChannels == null) {
            throw new IllegalArgumentException("targetChannels must not be null");
        }
        payload = Map.copyOf(payload);
        targetChannels = Set.copyOf(targetChannels);
    }
}
