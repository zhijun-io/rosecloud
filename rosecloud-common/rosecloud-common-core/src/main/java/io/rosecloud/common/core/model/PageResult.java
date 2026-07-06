package io.rosecloud.common.core.model;

import java.util.Collections;
import java.util.List;

/**
 * Paginated payload, intended to be wrapped in an {@link ApiResponse}.
 */
public record PageResult<T>(List<T> records, long total, long current, long size) {

    public PageResult {
        records = records == null ? Collections.emptyList() : List.copyOf(records);
    }

    public static <T> PageResult<T> of(List<T> records, long total, long current, long size) {
        return new PageResult<>(records, total, current, size);
    }

    public static <T> PageResult<T> empty(long current, long size) {
        return new PageResult<>(Collections.emptyList(), 0L, current, size);
    }
}
