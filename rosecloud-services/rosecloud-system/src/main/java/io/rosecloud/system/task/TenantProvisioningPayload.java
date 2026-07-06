package io.rosecloud.system.task;

/** Payload for {@code TaskTypes#TENANT_PROVISIONING}. */
public record TenantProvisioningPayload(Long tenantId) {
}
