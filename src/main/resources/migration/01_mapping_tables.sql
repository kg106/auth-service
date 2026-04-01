-- =============================================================================
-- REVISED MIGRATION MAPPING TABLES
-- =============================================================================

-- =============================================================================
-- 1. USER MAPPING TABLE
DROP TABLE IF EXISTS auth.user_migration_map;
CREATE TABLE auth.user_migration_map (
    email_normalized VARCHAR(255) PRIMARY KEY,
    wallet_old_id   BIGINT,
    kyc_old_id      BIGINT,
    voucher_old_id  BIGINT,
    order_old_id    VARCHAR(50),
    new_uuid        UUID,
    created_at      TIMESTAMP DEFAULT NOW()
);

-- 2. TENANT MAPPING TABLE (Revised for 4 services)
DROP TABLE IF EXISTS auth.tenant_migration_map;
CREATE TABLE auth.tenant_migration_map (
    new_tenant_uuid UUID PRIMARY KEY,
    wallet_old_tenant_id   VARCHAR(50),
    kyc_old_tenant_id      VARCHAR(50),
    voucher_old_tenant_id   VARCHAR(50),
    order_old_tenant_id     VARCHAR(50),
    canonical_id            VARCHAR(50), -- Logical ID like 'T001' or 'default'
    created_at              TIMESTAMP DEFAULT NOW()
);
