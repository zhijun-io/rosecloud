package io.rosecloud.system.service;

import io.rosecloud.system.persistence.TenantMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TenantExpirySchedulerTest {

    @Mock
    TenantMapper tenantMapper;

    @Test
    void expireOverdueTenantsUpdatesEnabledTenantsPastExpiry() {
        TenantExpiryScheduler scheduler = new TenantExpiryScheduler(tenantMapper);

        scheduler.expireOverdueTenants();

        verify(tenantMapper).update(any(), any());
    }
}
