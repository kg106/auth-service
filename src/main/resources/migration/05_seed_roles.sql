-- =============================================================================
-- PHASE 1.5: MAP USER ROLES (RESTART VERSION)
-- Maps legacy roles from all four services to central Auth Roles.
-- =============================================================================

-- Ensure standard roles exist in auth.roles
INSERT INTO auth.roles (id, name) VALUES 
(1, 'ROLE_USER'), 
(2, 'ROLE_ORG_ADMIN'), 
(3, 'ROLE_SUPER_ADMIN')
ON CONFLICT (id) DO NOTHING;

-- Map across all services using normalized_users_pool (if still available) or migration map
-- Here we use the user_migration_map to find the correct central UUID.

-- 1. KYC Roles
INSERT INTO auth.user_roles (user_id, role_id)
SELECT m.new_uuid, 
       CASE 
         WHEN r.name = 'ROLE_SUPER_ADMIN' THEN 3
         WHEN r.name LIKE '%ADMIN%' THEN 2 
         ELSE 1 
       END
FROM staging.kyc_user_roles ur
JOIN staging.kyc_roles r ON r.id = ur.role_id
JOIN auth.user_migration_map m ON m.kyc_old_id = ur.user_id
ON CONFLICT DO NOTHING;

-- 2. Wallet Roles
INSERT INTO auth.user_roles (user_id, role_id)
SELECT m.new_uuid, 
       CASE 
         WHEN w.role = 'ROLE_SUPER_ADMIN' THEN 3
         WHEN w.role = 'ROLE_ORG_ADMIN' THEN 2 
         ELSE 1 
     END
FROM staging.wallet_users w
JOIN auth.user_migration_map m ON m.wallet_old_id = w.id
ON CONFLICT DO NOTHING;

-- 3. Voucher Roles
INSERT INTO auth.user_roles (user_id, role_id)
SELECT m.new_uuid, 
       CASE 
         WHEN r.name LIKE '%ADMIN%' THEN 2 
         ELSE 1 
       END
FROM staging.voucher_user_roles ur
JOIN staging.voucher_roles r ON r.id = ur.role_id
JOIN auth.user_migration_map m ON m.voucher_old_id = ur.user_id
ON CONFLICT DO NOTHING;

-- 4. Order Roles
INSERT INTO auth.user_roles (user_id, role_id)
SELECT m.new_uuid, 
       CASE 
         WHEN r.role_name LIKE '%ADMIN%' THEN 2 
         ELSE 1 
       END
FROM staging.order_users o
JOIN staging.order_roles r ON r.id = o.role_id
JOIN auth.user_migration_map m ON m.order_old_id = CAST(o.id AS VARCHAR)
ON CONFLICT DO NOTHING;
