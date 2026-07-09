package io.rosecloud.common.security.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class UserPrincipal implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Type type;
    private final String value;

    @JsonCreator
    public UserPrincipal(@JsonProperty("type") Type type,
                         @JsonProperty("value") String value) {
        this.type = type;
        this.value = value;
    }

    public Type getType() { return type; }
    public String getValue() { return value; }

    public enum Type {
        USER_NAME,
        PUBLIC_ID
    }
}
