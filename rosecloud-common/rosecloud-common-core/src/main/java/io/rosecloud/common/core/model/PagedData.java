package io.rosecloud.common.core.model;
import lombok.RequiredArgsConstructor;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Page result envelope, modeled on ThingsBoard's {@code PageData<T>}. Uses {@code hasNext}
 * so large tables can avoid a {@code COUNT(*)} when a cursor-style walk is enough.
 */
@RequiredArgsConstructor
@ToString
@EqualsAndHashCode
public class PagedData<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final PagedData<Object> EMPTY = new PagedData<>(Collections.emptyList(), 0, 0L, false);

    private final List<T> data;
    private final int totalPages;
    private final long totalElements;
    private final boolean hasNext;
    @SuppressWarnings("unchecked")
    public static <T> PagedData<T> empty() {
        return (PagedData<T>) EMPTY;
    }

    public List<T> getData() {
        return data;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public boolean isHasNext() {
        return hasNext;
    }
}
