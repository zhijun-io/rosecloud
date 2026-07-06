package io.rosecloud.system.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.rosecloud.system.domain.TaskTypes;
import io.rosecloud.system.service.TenantProvisioner;
import org.springframework.stereotype.Component;

/** Provisions a tenant (first admin + enable) as an async task. */
@Component
public class TenantProvisioningTaskHandler implements TaskHandler {

    private final TenantProvisioner provisioner;
    private final ObjectMapper objectMapper;

    public TenantProvisioningTaskHandler(TenantProvisioner provisioner, ObjectMapper objectMapper) {
        this.provisioner = provisioner;
        this.objectMapper = objectMapper;
    }

    @Override
    public String type() {
        return TaskTypes.TENANT_PROVISIONING;
    }

    @Override
    public String execute(String payload) throws Exception {
        TenantProvisioningPayload p = objectMapper.readValue(payload, TenantProvisioningPayload.class);
        provisioner.provision(p.tenantId());
        return "tenant " + p.tenantId() + " provisioned";
    }
}
