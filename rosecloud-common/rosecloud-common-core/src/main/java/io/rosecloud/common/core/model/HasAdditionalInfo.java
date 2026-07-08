package io.rosecloud.common.core.model;

import com.fasterxml.jackson.databind.JsonNode;

/** Marker-style contract for domain objects that carry optional JSON additional info. */
public interface HasAdditionalInfo {

    JsonNode getAdditionalInfo();
}
