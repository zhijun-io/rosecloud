package io.rosecloud.system.domain;

import com.fasterxml.jackson.databind.JsonNode;
import io.rosecloud.common.core.model.BaseData;
import io.rosecloud.common.core.model.BaseDataWithAdditionalInfo;
import io.rosecloud.common.core.model.HasAdditionalInfo;

import java.util.Objects;

/**
 * Tenant profile entity aligned with ThingsBoard's split between tenant and
 * tenant profile. {@code additionalInfo} carries the structured profile JSON,
 * while {@link #profileData} is the typed view used by callers.
 */
public final class TenantProfile extends BaseDataWithAdditionalInfo implements HasAdditionalInfo {

    private final String id;
    private final String name;
    private final String description;
    private final boolean defaultProfile;
    private transient TenantProfileData profileData;

    public TenantProfile(String id, String name, String description, TenantProfileData profileData) {
        this(id, name, description, false, (JsonNode) null);
        setProfileData(profileData);
    }

    public TenantProfile(String id, String name, String description, JsonNode additionalInfo) {
        this(id, name, description, false, additionalInfo);
    }

    public TenantProfile(String id, String name, String description, boolean defaultProfile, JsonNode additionalInfo) {
        super(additionalInfo == null ? BaseData.mapper.valueToTree(TenantProfileData.defaults()) : additionalInfo);
        this.id = id;
        this.name = name;
        this.description = description;
        this.defaultProfile = defaultProfile;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isDefault() {
        return defaultProfile;
    }

    public TenantProfileData getProfileData() {
        TenantProfileData data = profileData;
        if (data != null) {
            return data;
        }
        JsonNode additionalInfo = getAdditionalInfo();
        if (additionalInfo != null) {
            data = mapper.convertValue(additionalInfo, TenantProfileData.class);
            profileData = data;
            return data;
        }
        return TenantProfileData.defaults();
    }

    public void setProfileData(TenantProfileData profileData) {
        TenantProfileData value = profileData == null ? TenantProfileData.defaults() : profileData;
        this.profileData = value;
        setAdditionalInfo(mapper.valueToTree(value));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TenantProfile that)) {
            return false;
        }
        return Objects.equals(id, that.id)
                && Objects.equals(name, that.name)
                && Objects.equals(description, that.description)
                && defaultProfile == that.defaultProfile
                && Objects.equals(getProfileData(), that.getProfileData())
                && Objects.equals(getAdditionalInfo(), that.getAdditionalInfo());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description, defaultProfile, getProfileData(), getAdditionalInfo());
    }

    @Override
    public String toString() {
        return "TenantProfile[" +
                "id=" + id +
                ", name=" + name +
                ", description=" + description +
                ", defaultProfile=" + defaultProfile +
                ", profileData=" + getProfileData() +
                ", additionalInfo=" + getAdditionalInfo() +
                ']';
    }
}
