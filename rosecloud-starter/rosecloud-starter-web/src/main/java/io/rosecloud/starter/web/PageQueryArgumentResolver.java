package io.rosecloud.starter.web;

import io.rosecloud.common.core.model.PageQuery;
import io.rosecloud.common.core.model.SortDirection;
import io.rosecloud.common.core.model.SortField;
import io.rosecloud.common.core.model.TimePageQuery;
import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Binds {@code page}/{@code size}/{@code keyword}/{@code sort}/{@code startTime}/{@code endTime}
 * request parameters into a {@link PageQuery} (or {@link TimePageQuery}). Treats {@code page} as
 * 1-based and applies a default size of {@value io.rosecloud.common.core.model.PageQuery#DEFAULT_SIZE}
 * (minimum 1); the max page size is
 * enforced by the MyBatis-Plus pagination interceptor.
 *
 * <p>{@code sort} accepts repeated values, each {@code property} or {@code property:DIRECTION}
 * (DIRECTION defaults to ASC). Unknown sort properties are left for the data layer's
 * {@link io.rosecloud.common.core.model.SortColumnMapper} to reject.
 */
public class PageQueryArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return PageQuery.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
            @Nullable ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            @Nullable WebDataBinderFactory binderFactory) {
        Map<String, String[]> params = webRequest.getParameterMap();
        int page = parseInt(params, "page", PageQuery.DEFAULT_PAGE);
        int size = Math.max(1, parseInt(params, "size", PageQuery.DEFAULT_SIZE));
        String keyword = first(params, "keyword");
        List<SortField> sorts = parseSorts(params.get("sort"));

        if (TimePageQuery.class.isAssignableFrom(parameter.getParameterType())) {
            return new TimePageQuery(page, size, keyword, sorts,
                    parseTime(first(params, "startTime")), parseTime(first(params, "endTime")));
        }
        return new PageQuery(page, size, keyword, sorts);
    }

    private static int parseInt(Map<String, String[]> params, String name, int fallback) {
        String[] values = params.get(name);
        if (values == null || values.length == 0 || values[0].isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(values[0].trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static String first(Map<String, String[]> params, String name) {
        String[] values = params.get(name);
        return (values != null && values.length > 0 && !values[0].isBlank()) ? values[0].trim() : null;
    }

    private static List<SortField> parseSorts(@Nullable String[] raw) {
        List<SortField> sorts = new ArrayList<>();
        if (raw == null) {
            return sorts;
        }
        for (String token : raw) {
            if (token == null || token.isBlank()) {
                continue;
            }
            int sep = token.lastIndexOf(':');
            if (sep < 0) {
                sorts.add(SortField.of(token.trim()));
            } else {
                String property = token.substring(0, sep).trim();
                SortDirection direction = parseDirection(token.substring(sep + 1).trim());
                sorts.add(SortField.of(property, direction));
            }
        }
        return sorts;
    }

    private static SortDirection parseDirection(String value) {
        try {
            return SortDirection.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            return SortDirection.ASC;
        }
    }

    private static LocalDateTime parseTime(@Nullable String value) {
        if (value == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(value, DateTimeFormatter.ISO_DATE);
            } catch (Exception ex) {
                return null;
            }
        }
    }
}
