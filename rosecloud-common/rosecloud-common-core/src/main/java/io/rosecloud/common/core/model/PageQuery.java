package io.rosecloud.common.core.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

/**
 * Unified pagination + search + sort request, replacing Spring {@code Pageable} and the
 * scattered {@code keyword}/{@code current}/{@code size} service parameters.
 *
 * <p>{@code page} is 1-based to match MyBatis-Plus {@code Page} directly, so controllers no
 * longer need the {@code pageNumber + 1} conversion. The upper bound on {@code size} is enforced
 * by the MyBatis-Plus pagination interceptor's max limit, never trusted from the client.
 */
@Data
public class PageQuery {

    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_SIZE = 10;

    private int page = DEFAULT_PAGE;
    private int size = DEFAULT_SIZE;
    private String keyword;
    private List<SortField> sorts = new ArrayList<>();

    public PageQuery() {
    }

    public PageQuery(int page, int size) {
        this.page = page;
        this.size = size;
    }

    public PageQuery(int page, int size, String keyword, List<SortField> sorts) {
        this.page = page;
        this.size = size;
        this.keyword = keyword;
        if (sorts != null) {
            this.sorts = new ArrayList<>(sorts);
        }
    }
}
