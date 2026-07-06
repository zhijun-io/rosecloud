package io.rosecloud.system.domain;

/** Known task type codes. Each maps to a {@code TaskHandler#type()} registration. */
public final class TaskTypes {

    /** Provisions a tenant's first admin and enables the tenant. Payload: {@code {"tenantId": <id>}}. */
    public static final String TENANT_PROVISIONING = "tenant-provisioning";

    private TaskTypes() {
    }
}
