package io.rosecloud.starter.tenant.core;

/** Tenant data isolation strategy. */
public enum MultiTenantType {

    /** Row-level isolation via a {@code tenant_id} column. */
    COLUMN,

    /** Per-tenant schema isolation. */
    SCHEMA,

    /** Per-tenant datasource isolation. */
    DATASOURCE,

    /** Multi-tenancy disabled. */
    NONE
}
