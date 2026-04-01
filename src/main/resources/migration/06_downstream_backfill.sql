-- =============================================================================
-- PHASE 2 & 3: DOWNSTREAM SERVICE BACKFILL
-- Run these scripts against your local database to permanently link 
-- legacy records to the new Centralized Auth UUIDs.
-- =============================================================================

-- =============================================================================
-- 1. WALLET SERVICE
-- =============================================================================
-- Step 2: Add the new column
ALTER TABLE public.wallet ADD COLUMN IF NOT EXISTS user_uuid UUID;

-- Step 3: Backfill from mapping table
UPDATE public.wallet w
SET user_uuid = m.new_uuid
FROM auth.user_migration_map m
WHERE w.user_id = m.wallet_old_id;

-- =============================================================================
-- 2. KYC SERVICE
-- =============================================================================
-- Step 2: Add the new column (assuming table is named kyc_records)
ALTER TABLE public.kyc_records ADD COLUMN IF NOT EXISTS user_uuid UUID;

-- Step 3: Backfill from mapping table
UPDATE public.kyc_records k
SET user_uuid = m.new_uuid
FROM auth.user_migration_map m
WHERE k.user_id = m.kyc_old_id;

-- =============================================================================
-- 3. VOUCHER SERVICE
-- =============================================================================
-- Step 2: Add the new column (assuming table is named vouchers)
ALTER TABLE public.vouchers ADD COLUMN IF NOT EXISTS user_uuid UUID;

-- Step 3: Backfill from mapping table
UPDATE public.vouchers v
SET user_uuid = m.new_uuid
FROM auth.user_migration_map m
WHERE v.user_id = m.voucher_old_id;

-- =============================================================================
-- 4. ORDER SERVICE
-- =============================================================================
-- Step 2: Add the new column (assuming table is named app_user or orders)
ALTER TABLE public.orders ADD COLUMN IF NOT EXISTS user_uuid UUID;

-- Step 3: Backfill from mapping table
UPDATE public.orders o
SET user_uuid = m.new_uuid
FROM auth.user_migration_map m
WHERE o.app_user_id = CAST(m.order_old_id AS UUID);


-- =============================================================================
-- VERIFICATION (Must return 0 for all)
-- =============================================================================
-- SELECT count(*) FROM public.wallet WHERE user_uuid IS NULL;
-- SELECT count(*) FROM public.kyc_records WHERE user_uuid IS NULL;
