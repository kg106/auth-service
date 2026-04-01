-- =============================================================================
-- FLATTENED RESTART: SEED CENTRAL AUTH SERVICE
-- Logic: Roles on User table | Tenants unique by Name
-- =============================================================================

-- -----------------------------------------------------------------------------
-- STEP 1: CLEANUP AND RECREATE MAPPING TABLES
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS auth.user_roles;
DROP TABLE IF EXISTS auth.user_migration_map;
DROP TABLE IF EXISTS auth.tenant_migration_map;

CREATE TABLE auth.user_migration_map (
    email_normalized VARCHAR(255) PRIMARY KEY,
    wallet_old_id   BIGINT,
    kyc_old_id      BIGINT,
    voucher_old_id  BIGINT,
    order_old_id    VARCHAR(50),
    new_uuid        UUID,
    created_at      TIMESTAMP DEFAULT NOW()
);

CREATE TABLE auth.tenant_migration_map (
    new_tenant_uuid UUID PRIMARY KEY,
    wallet_old_tenant_id   VARCHAR(50),
    kyc_old_tenant_id      VARCHAR(50),
    voucher_old_tenant_id   VARCHAR(50),
    order_old_tenant_id     VARCHAR(50),
    canonical_id            VARCHAR(50), 
    created_at              TIMESTAMP DEFAULT NOW()
);

TRUNCATE TABLE auth.users CASCADE;
TRUNCATE TABLE auth.tenants CASCADE;

-- Ensure Roles exist
INSERT INTO auth.roles (id, name) VALUES (1, 'ROLE_USER'), (2, 'ROLE_ORG_ADMIN'), (3, 'ROLE_SUPER_ADMIN') ON CONFLICT DO NOTHING;

-- -----------------------------------------------------------------------------
-- STEP 2: DEDUPLICATE TENANTS BY NAME
-- -----------------------------------------------------------------------------
CREATE TEMP TABLE tenants_by_name AS
SELECT 
    INITCAP(TRIM(name)) as t_name, 
    MAX(tenant_id) as canonical_id, 
    MAX(email) as t_email 
FROM (
    SELECT name, tenant_id, email FROM staging.kyc_tenants
    UNION ALL
    SELECT name, org_code as tenant_id, 'unknown@wallet.com' as email FROM staging.wallet_orgs
    UNION ALL
    SELECT tenant_name as name, tenant_code as tenant_id, 'unknown@voucher.com' as email FROM staging.voucher_tenants
    UNION ALL
    SELECT name, code as tenant_id, 'unknown@order.com' as email FROM staging.order_orgs
) all_raw
GROUP BY INITCAP(TRIM(name));

INSERT INTO auth.tenants (tenant_id, name, email)
SELECT canonical_id, t_name, t_email FROM tenants_by_name
ON CONFLICT (name) DO NOTHING;

-- Map across all old IDs
INSERT INTO auth.tenant_migration_map (new_tenant_uuid, canonical_id)
SELECT id, tenant_id FROM auth.tenants;

UPDATE auth.tenant_migration_map m SET kyc_old_tenant_id = CAST(k.id AS VARCHAR) FROM staging.kyc_tenants k JOIN auth.tenants t ON t.tenant_id = k.tenant_id WHERE m.new_tenant_uuid = t.id;
UPDATE auth.tenant_migration_map m SET wallet_old_tenant_id = CAST(w.id AS VARCHAR) FROM staging.wallet_orgs w JOIN auth.tenants t ON t.tenant_id = w.org_code WHERE m.new_tenant_uuid = t.id;
UPDATE auth.tenant_migration_map m SET voucher_old_tenant_id = CAST(v.id AS VARCHAR) FROM staging.voucher_tenants v JOIN auth.tenants t ON t.tenant_id = v.tenant_code WHERE m.new_tenant_uuid = t.id;
UPDATE auth.tenant_migration_map m SET order_old_tenant_id = CAST(o.id AS VARCHAR) FROM staging.order_orgs o JOIN auth.tenants t ON t.tenant_id = o.code WHERE m.new_tenant_uuid = t.id;

-- -----------------------------------------------------------------------------
-- STEP 3: CONSOLIDATE USERS AND ROLES
-- -----------------------------------------------------------------------------

CREATE TEMP TABLE users_with_mapped_roles AS
SELECT 
    u.email_norm, 
    u.pwd, 
    u.u_name, 
    u.mobile AS raw_mobile, 
    tm.new_tenant_uuid, 
    t.name as tenant_disp_name,
    u.mapped_role_id,
    u.original_user_id,
    u.source_service,
    u.dob,
    u.last_login_at
FROM (
    -- KYC
    SELECT LOWER(TRIM(ky.email)) as email_norm, ky.password_hash as pwd, ky.name as u_name, ky.mobile_number as mobile, ky.tenant_id as old_t_id, CAST(ky.id AS VARCHAR) as original_user_id, 'kyc' as source_service,
           CASE WHEN kr.name = 'ROLE_SUPER_ADMIN' THEN 3 WHEN kr.name LIKE '%ADMIN%' THEN 2 ELSE 1 END as mapped_role_id,
           ky.dob, ky.last_login_at
    FROM staging.kyc_users ky
    LEFT JOIN staging.kyc_user_roles kur ON kur.user_id = ky.id
    LEFT JOIN staging.kyc_roles kr ON kr.id = kur.role_id
    UNION ALL
    -- Wallet
    SELECT LOWER(TRIM(w.email)), w.password_hash, w.username, w.phone_number, CAST(w.organization_id AS VARCHAR), CAST(w.id AS VARCHAR), 'wallet',
           CASE WHEN w.role = 'ROLE_SUPER_ADMIN' THEN 3 WHEN w.role = 'ROLE_ORG_ADMIN' THEN 2 ELSE 1 END,
           NULL, NULL
    FROM staging.wallet_users w
    UNION ALL
    -- Voucher
    SELECT LOWER(TRIM(v.email)), v.password_hash, v.first_name || ' ' || v.last_name, v.phone_number, CAST(v.tenant_id AS VARCHAR), CAST(v.id AS VARCHAR), 'voucher',
           CASE WHEN vr.name LIKE '%ADMIN%' THEN 2 ELSE 1 END,
           NULL, NULL
    FROM staging.voucher_users v
    LEFT JOIN staging.voucher_user_roles vur ON vur.user_id = v.id
    LEFT JOIN staging.voucher_roles vr ON vr.id = vur.role_id
    UNION ALL
    -- Order
    SELECT LOWER(TRIM(o.email)), o.password, split_part(o.email, '@', 1), NULL, o.organization_id, CAST(o.id AS VARCHAR), 'order',
           CASE WHEN orr.role_name LIKE '%ADMIN%' THEN 2 ELSE 1 END,
           NULL, NULL
    FROM staging.order_users o
    LEFT JOIN staging.order_roles orr ON orr.id = o.role_id
) u
LEFT JOIN auth.tenant_migration_map tm ON (
    (u.source_service = 'kyc' AND u.old_t_id = tm.kyc_old_tenant_id) OR
    (u.source_service = 'wallet' AND u.old_t_id = tm.wallet_old_tenant_id) OR
    (u.source_service = 'voucher' AND u.old_t_id = tm.voucher_old_tenant_id) OR
    (u.source_service = 'order' AND u.old_t_id = tm.order_old_tenant_id)
)
LEFT JOIN auth.tenants t ON t.id = tm.new_tenant_uuid;

-- Final User Insertion (Deduplicated by Email + Tenant)
INSERT INTO auth.users (tenant_id, email, mobile_number, name, tenant_name, role_id, password_hash, dob, last_login_at, status)
SELECT 
    COALESCE(new_tenant_uuid, (SELECT id FROM auth.tenants LIMIT 1)), 
    email_norm, 
    CASE 
        WHEN COUNT(*) OVER (PARTITION BY COALESCE(MAX(raw_mobile), '0000000000')) > 1 
        THEN COALESCE(MAX(raw_mobile), '0000000000') || '-' || ROW_NUMBER() OVER (PARTITION BY COALESCE(MAX(raw_mobile), '0000000000') ORDER BY email_norm)
        ELSE COALESCE(MAX(raw_mobile), '0000000000')
    END, 
    MAX(u_name), 
    COALESCE(MAX(tenant_disp_name), 'Default'),
    MAX(mapped_role_id), 
    MAX(pwd), 
    MAX(dob), 
    MAX(last_login_at), 
    'ACTIVE'
FROM users_with_mapped_roles
GROUP BY email_norm, new_tenant_uuid;

-- Populate User Map
INSERT INTO auth.user_migration_map (email_normalized, new_uuid)
SELECT email, id FROM auth.users
ON CONFLICT (email_normalized) DO NOTHING;

UPDATE auth.user_migration_map m SET kyc_old_id = CAST(up.original_user_id AS BIGINT) FROM users_with_mapped_roles up WHERE m.email_normalized = up.email_norm AND up.source_service = 'kyc';
UPDATE auth.user_migration_map m SET wallet_old_id = CAST(up.original_user_id AS BIGINT) FROM users_with_mapped_roles up WHERE m.email_normalized = up.email_norm AND up.source_service = 'wallet';
UPDATE auth.user_migration_map m SET voucher_old_id = CAST(up.original_user_id AS BIGINT) FROM users_with_mapped_roles up WHERE m.email_normalized = up.email_norm AND up.source_service = 'voucher';
UPDATE auth.user_migration_map m SET order_old_id = up.original_user_id FROM users_with_mapped_roles up WHERE m.email_normalized = up.email_norm AND up.source_service = 'order';
