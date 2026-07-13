package io.rosecloud.common.core.model;

/**
 * A single sort instruction: an API-facing {@code property} plus a {@link SortDirection}.
 * Replaces Spring Data's {@code Sort.Order} so the request contract stays framework-free
 * and the property can be whitelisted to a real column via {@link SortColumnMapper}.
 */
public record SortField(String property, SortDirection direction) {

    public SortField(String property) {
        this(property, SortDirection.ASC);
    }

    public static SortField of(String property) {
        return new SortField(property);
    }

    public static SortField of(String property, SortDirection direction) {
        return new SortField(property, direction);
    }
}
