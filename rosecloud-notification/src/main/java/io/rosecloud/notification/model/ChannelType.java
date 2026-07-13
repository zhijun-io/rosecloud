package io.rosecloud.notification.model;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Channel type identifier. {@code name} is normalized (trimmed, upper-cased
 * under {@link Locale#ROOT}) and must match {@code [A-Z][A-Z0-9_]*}. It
 * participates in equality, ordering and the engine channel registry, so it
 * is not free-form text. {@code EMAIL} and {@code SMS} are built-in examples;
 * custom types follow the same naming rule (e.g. {@code PUSH}, {@code WEBHOOK}).
 */
public record ChannelType(String name) {

    private static final Pattern VALID = Pattern.compile("[A-Z][A-Z0-9_]*");

    public static final ChannelType EMAIL = new ChannelType("EMAIL");
    public static final ChannelType SMS = new ChannelType("SMS");

    public ChannelType {
        if (name == null) {
            throw new IllegalArgumentException("channel type name must not be null");
        }
        name = name.trim().toUpperCase(Locale.ROOT);
        if (!VALID.matcher(name).matches()) {
            throw new IllegalArgumentException("invalid channel type name: " + name);
        }
    }
}
