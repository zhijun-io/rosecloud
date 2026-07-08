package io.rosecloud.starter.tenant.mybatis;

import io.rosecloud.starter.tenant.core.TenantContext;
import io.rosecloud.starter.tenant.core.TenantProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoseCloudTenantLineHandlerTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void ignoresGlobalTablesWhenTenantIsBound() {
        TenantProperties properties = new TenantProperties();
        properties.setIgnoreTables(List.of(
                "sys_role",
                "sys_user_role",
                "sys_menu",
                "sys_role_menu",
                "sys_audit_log",
                "sys_login_log",
                "sys_dict_type",
                "sys_dict_data",
                "sys_dept",
                "sys_login_session",
                "sys_setting_key",
                "sys_system_setting",
                "sys_user_setting"
        ));

        TenantContext.setTenantId("tenant-100");
        RoseCloudTenantLineHandler handler = new RoseCloudTenantLineHandler(properties);

        assertTrue(handler.ignoreTable("sys_role"));
        assertTrue(handler.ignoreTable("sys_login_log"));
        assertEquals("'tenant-100'", handler.getTenantId().toString());
    }

    @Test
    void ignoresEverythingWhenNoTenantIsBound() {
        TenantProperties properties = new TenantProperties();
        properties.setIgnoreTables(List.of("sys_role"));
        RoseCloudTenantLineHandler handler = new RoseCloudTenantLineHandler(properties);

        assertTrue(handler.ignoreTable("sys_user"));
        assertNull(handler.getTenantId());
    }
}
