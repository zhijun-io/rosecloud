package io.rosecloud.common.core.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.io.UncheckedIOException;

/**
 * Central, framework-free Jackson helper (modeled on ThingsBoard's {@code JacksonUtil}).
 * A single module-aware {@link ObjectMapper} is shared process-wide so entities and domain
 * objects outside the Spring context can parse/serialize JSON without wiring a Spring bean.
 * {@code findAndAddModules()} picks up classpath modules (e.g. JavaTimeModule); unknown
 * properties are ignored and dates are written as ISO strings.
 */
public final class JacksonUtil {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
            .findAndAddModules()
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private JacksonUtil() {
    }

    public static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }

    public static String toString(Object obj) {
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static <T> T fromString(String str, Class<T> clazz) {
        try {
            return OBJECT_MAPPER.readValue(str, clazz);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static <T> T fromString(String str, com.fasterxml.jackson.core.type.TypeReference<T> type) {
        try {
            return OBJECT_MAPPER.readValue(str, type);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static JsonNode toJsonNode(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readTree(value);
        } catch (Exception e) {
            throw new IllegalStateException("Invalid JSON content: " + value, e);
        }
    }

    public static JsonNode valueToTree(Object value) {
        return OBJECT_MAPPER.valueToTree(value);
    }

    public static <T> T treeToValue(JsonNode node, Class<T> clazz) {
        try {
            return OBJECT_MAPPER.treeToValue(node, clazz);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static <T> T convertValue(Object fromValue, Class<T> clazz) {
        return OBJECT_MAPPER.convertValue(fromValue, clazz);
    }

    public static <T> T convertValue(Object fromValue, com.fasterxml.jackson.core.type.TypeReference<T> type) {
        return OBJECT_MAPPER.convertValue(fromValue, type);
    }

    public static String writeString(JsonNode node) {
        return node == null || node.isNull() ? null : node.toString();
    }
}
