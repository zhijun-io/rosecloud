package io.rosecloud.common.core.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.function.Function;

/**
 * Base class for domain data that carries optional JSON additional info.
 * Subclasses expose their own business fields with standard getter methods.
 */
public abstract class BaseDataWithAdditionalInfo extends BaseData implements HasAdditionalInfo {

    private JsonNode additionalInfo;

    protected BaseDataWithAdditionalInfo(JsonNode additionalInfo) {
        this.additionalInfo = additionalInfo;
    }

    protected BaseDataWithAdditionalInfo() {
        super();
    }

    protected BaseDataWithAdditionalInfo(BaseDataWithAdditionalInfo data) {
        super(data);
        this.additionalInfo = data.additionalInfo;
    }

    @Override
    public JsonNode getAdditionalInfo() {
        return additionalInfo;
    }

    public void setAdditionalInfo(JsonNode additionalInfo) {
        this.additionalInfo = additionalInfo;
    }

    public void setAdditionalInfoField(String field, JsonNode value) {
        JsonNode current = additionalInfo;
        if (!(current instanceof ObjectNode)) {
            current = JsonNodeFactory.instance.objectNode();
        }
        ((ObjectNode) current).set(field, value);
        this.additionalInfo = current;
    }

    public <T> T getAdditionalInfoField(String field, Function<JsonNode, T> mapper, T defaultValue) {
        if (additionalInfo != null && additionalInfo.has(field)) {
            return mapper.apply(additionalInfo.get(field));
        }
        return defaultValue;
    }
}
