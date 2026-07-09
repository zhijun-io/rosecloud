package io.rosecloud.common.security.token;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record JwtPair(String accessToken, String refreshToken) implements Serializable {}
