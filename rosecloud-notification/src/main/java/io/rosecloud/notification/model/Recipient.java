package io.rosecloud.notification.model;

import java.util.Map;

/**
 * Recipient view. Addresses are pre-resolved by the caller; the engine does
 * not resolve users, tenants or roles. v1 supports at most one resolved string
 * address per channel, hence {@code Map<ChannelType, String>}.
 *
 * <p>{@code recipientId} is a business label for mapping results back to the
 * caller's recipient records. It must be non-blank with no surrounding
 * whitespace, preserved case-sensitively. It must be unique within a single
 * broadcast (enforced at {@code broadcast()} entry, not here, since uniqueness
 * is a cross-list property).
 *
 * <p>{@code context} is a recipient-scoped pass-through; the engine does not
 * interpret it. Both {@code addresses} and {@code context} must be non-null
 * (use empty maps) and are defensively copied.
 */
public record Recipient(
        String recipientId,
        Map<ChannelType, String> addresses,
        Map<String, Object> context) {

    public Recipient {
        if (recipientId == null || recipientId.isBlank()) {
            throw new IllegalArgumentException("recipientId must not be blank");
        }
        if (!recipientId.equals(recipientId.trim())) {
            throw new IllegalArgumentException("recipientId must not have surrounding whitespace");
        }
        if (addresses == null) {
            throw new IllegalArgumentException("addresses must not be null");
        }
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }
        addresses = Map.copyOf(addresses);
        context = Map.copyOf(context);
    }
}
