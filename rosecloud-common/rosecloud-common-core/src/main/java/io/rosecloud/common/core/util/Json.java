package io.rosecloud.common.core.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

/**
 * Minimal JSON bridge for entity &rarr; domain conversion. Entities parse stored
 * JSON columns into {@link JsonNode} here so they stay free of a Spring-wired
 * {@link ObjectMapper}; {@code findAndAddModules()} keeps Java/time and other
 * registered serializers in sync with the application mapper.
 */
public final class Json {

    private static final ObjectMapper MAPPER = JsonMapper.builder().findAndAddModules().build();

    private Json() {
    }

    public static JsonNode readTree(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readTree(value);
        } catch (Exception e) {
            throw new IllegalStateException("Invalid JSON content: " + value, e);
        }
    }

    public static JsonNode toTree(Object value) {
        return MAPPER.valueToTree(value);
    }

    public static String writeString(JsonNode node) {
        return node == null || node.isNull() ? null : node.toString();
    }
}
