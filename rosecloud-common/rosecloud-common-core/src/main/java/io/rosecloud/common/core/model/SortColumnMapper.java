package io.rosecloud.common.core.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Whitelist mapping API sort {@code property} -> real database column. Any property not
 * present is rejected, which prevents order-by injection. Mirrors the {@code columnMap}
 * argument of ThingsBoard's {@code PageLink.toSort(...)}.
 */
public class SortColumnMapper {

    private static final SortField DEFAULT_SORT = SortField.of("id");

    private final Map<String, String> propertyToColumn;
    private final List<SortField> defaultSorts;

    public SortColumnMapper(Map<String, String> propertyToColumn) {
        this(propertyToColumn, List.of(DEFAULT_SORT));
    }

    public SortColumnMapper(Map<String, String> propertyToColumn, SortField... defaultSorts) {
        this(propertyToColumn, List.of(defaultSorts));
    }

    public SortColumnMapper(Map<String, String> propertyToColumn, List<SortField> defaultSorts) {
        this.propertyToColumn = Map.copyOf(propertyToColumn);
        this.defaultSorts = defaultSorts;
    }

    public String toColumn(String property) {
        String column = propertyToColumn.get(property);
        if (column == null) {
            throw new IllegalArgumentException("Unsupported sort property: " + property);
        }
        return column;
    }

    /**
     * Resolve requested sort fields to the effective ordering (with default fallback). The
     * property-to-column mapping is applied later by the data layer via {@link #toColumn(String)}.
     */
    public List<SortField> resolve(List<SortField> requested) {
        return (requested == null || requested.isEmpty())
                ? defaultSortFields()
                : requested;
    }

    private List<SortField> defaultSortFields() {
        List<SortField> resolved = new ArrayList<>(defaultSorts.size());
        for (SortField field : defaultSorts) {
            if (propertyToColumn.containsKey(field.property())) {
                resolved.add(field);
            }
        }
        return resolved;
    }
}
