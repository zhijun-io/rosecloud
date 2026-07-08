package io.rosecloud.common.core.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Base class for domain data that carries optional JSON additional info.
 * Subclasses expose their own business fields with standard getter methods.
 */
public abstract class BaseDataWithAdditionalInfo extends BaseData implements HasAdditionalInfo {

    private transient JsonNode additionalInfo;
    @JsonIgnore
    private byte[] additionalInfoBytes;

    protected BaseDataWithAdditionalInfo(JsonNode additionalInfo) {
        setAdditionalInfo(additionalInfo);
    }

    protected BaseDataWithAdditionalInfo() {
        super();
    }

    protected BaseDataWithAdditionalInfo(BaseDataWithAdditionalInfo data) {
        super(data);
        setAdditionalInfo(data.getAdditionalInfo());
    }

    @Override
    public JsonNode getAdditionalInfo() {
        return getJson(() -> additionalInfo, () -> additionalInfoBytes);
    }

    public void setAdditionalInfo(JsonNode additionalInfo) {
        setJson(additionalInfo, json -> this.additionalInfo = json, bytes -> this.additionalInfoBytes = bytes);
    }

    public void setAdditionalInfoField(String field, JsonNode value) {
        JsonNode additionalInfo = getAdditionalInfo();
        if (!(additionalInfo instanceof ObjectNode)) {
            additionalInfo = mapper.createObjectNode();
        }
        ((ObjectNode) additionalInfo).set(field, value);
        setAdditionalInfo(additionalInfo);
    }

    public <T> T getAdditionalInfoField(String field, Function<JsonNode, T> mapper, T defaultValue) {
        JsonNode additionalInfo = getAdditionalInfo();
        if (additionalInfo != null && additionalInfo.has(field)) {
            return mapper.apply(additionalInfo.get(field));
        }
        return defaultValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        BaseDataWithAdditionalInfo that = (BaseDataWithAdditionalInfo) o;
        return Arrays.equals(additionalInfoBytes, that.additionalInfoBytes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), Arrays.hashCode(additionalInfoBytes));
    }

    public static JsonNode getJson(Supplier<JsonNode> jsonData, Supplier<byte[]> binaryData) {
        JsonNode json = jsonData.get();
        if (json != null) {
            return json;
        }
        byte[] data = binaryData.get();
        if (data != null) {
            try {
                return mapper.readTree(new ByteArrayInputStream(data));
            } catch (IOException e) {
                return null;
            }
        }
        return null;
    }

    public static void setJson(JsonNode json, Consumer<JsonNode> jsonConsumer, Consumer<byte[]> bytesConsumer) {
        jsonConsumer.accept(json);
        try {
            bytesConsumer.accept(json == null ? null : mapper.writeValueAsBytes(json));
        } catch (JsonProcessingException e) {
            bytesConsumer.accept(null);
        }
    }
}
