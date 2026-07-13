package io.rosecloud.common.core.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

/**
 * {@link PageQuery} extended with an optional time window, the first-class citizen for
 * log / audit / telemetry style queries.
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class TimePageQuery extends PageQuery {

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public TimePageQuery() {
        super();
    }

    public TimePageQuery(int page, int size, String keyword, List<SortField> sorts,
            LocalDateTime startTime, LocalDateTime endTime) {
        super(page, size, keyword, sorts);
        this.startTime = startTime;
        this.endTime = endTime;
    }
}
