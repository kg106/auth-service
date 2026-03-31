# Data Migration Plan: Centralized Stateless Identity

This document outlines the step-by-step strategy for migrating user and organization data from decentralized microservice databases to the **Centralized Auth Service**.

---

## 🔷 Phase 0 — Preparation (Data Safety + Validation)

### 1. Take Full Backups (MANDATORY)
```sql
SELECT id, email, username, created_at FROM users ORDER BY id;
```
✔ **Export as CSV from:**
*   Wallet DB
*   KYC DB
*   Voucher DB

### 2. Normalize & Clean Data (VERY IMPORTANT)
Apply `LOWER(TRIM(email))` to all email fields.

✔ **Detect duplicates:**
```sql
SELECT LOWER(TRIM(email)) AS email, COUNT(*)
FROM merged_users
GROUP BY email
HAVING COUNT(*) > 1;
```
> [!IMPORTANT]
> Resolve conflicts manually before proceeding.

### 3. Create Migration Mapping Tables

#### User Mapping
```sql
CREATE TABLE user_migration_map (
    email_normalized VARCHAR(255) PRIMARY KEY,
    wallet_old_id   BIGINT,
    kyc_old_id      BIGINT,
    voucher_old_id  BIGINT,
    new_uuid        VARCHAR(36)
);
```

#### Tenant Mapping
```sql
CREATE TABLE tenant_migration_map (
    old_tenant_id   BIGINT PRIMARY KEY,
    new_tenant_uuid VARCHAR(36)
);
```

---

## 🔷 Phase 1 — Seed Auth Service & Build Mapping

### 1. Merge & Insert Users into Auth Service
```sql
INSERT INTO auth_users (id, email, username, created_at, status)
VALUES (UUID(), 'user@example.com', 'username', NOW(), 'ACTIVE');
```
✔ Ensure: **No duplicates** and **All users covered**.

### 2. Build Mapping Table
```sql
INSERT INTO user_migration_map (email_normalized, new_uuid, wallet_old_id)
SELECT LOWER(TRIM(a.email)), a.id, w.id
FROM auth_users a
JOIN wallet_db.users w 
ON LOWER(TRIM(a.email)) = LOWER(TRIM(w.email));
```

### 3. Fill Remaining Services
```sql
UPDATE user_migration_map m
JOIN kyc_db.users k 
ON m.email_normalized = LOWER(TRIM(k.email))
SET m.kyc_old_id = k.id;
```

### 4. Detect Unmapped Users (CRITICAL CHECK)
```sql
SELECT w.id, w.email
FROM wallet_db.users w
LEFT JOIN user_migration_map m 
ON LOWER(TRIM(w.email)) = m.email_normalized
WHERE m.new_uuid IS NULL;
```
> [!IMPORTANT]
> This query **MUST** return 0 rows.

---

## 🔷 Phase 2 — Schema Changes (Non-Breaking)

### Add UUID Columns
```sql
ALTER TABLE wallet ADD COLUMN user_uuid VARCHAR(36) NULL;
ALTER TABLE wallet ADD COLUMN tenant_uuid VARCHAR(36) NULL;

ALTER TABLE kyc_records ADD COLUMN user_uuid VARCHAR(36) NULL;
ALTER TABLE vouchers ADD COLUMN user_uuid VARCHAR(36) NULL;
```

### Add Indexes
```sql
CREATE INDEX idx_wallet_user_uuid ON wallet(user_uuid);
CREATE INDEX idx_kyc_user_uuid ON kyc_records(user_uuid);
```

---

## 🔷 Phase 3 — Backfill UUIDs

### Wallet
```sql
UPDATE wallet w
JOIN user_migration_map m 
ON w.user_id = m.wallet_old_id
SET w.user_uuid = m.new_uuid;
```

### KYC
```sql
UPDATE kyc_records k
JOIN user_migration_map m 
ON k.user_id = m.kyc_old_id
SET k.user_uuid = m.new_uuid;
```

### Validation (STRICT)
```sql
SELECT COUNT(*) FROM wallet WHERE user_uuid IS NULL;
SELECT COUNT(*) FROM kyc_records WHERE user_uuid IS NULL;
```
> [!IMPORTANT]
> Results **MUST** be 0.

### Referential Integrity Check
```sql
SELECT COUNT(*)
FROM wallet w
LEFT JOIN auth_users a ON w.user_uuid = a.id
WHERE a.id IS NULL;
```

---

## 🔷 Phase 3.5 — Dual Write Phase (CRITICAL 🔥)

Deploy intermediate version of services to write to **BOTH** fields:
*   `wallet.setUserId(oldId);`
*   `wallet.setUserUuid(newUuid);`

✔ **Monitor for consistency and Ensure no NULL UUIDs appear.**

---

## 🔷 Phase 4 — Code Cutover (Read Switch)

### Update Code
*   **Entities:** Use `private String userId; // UUID`
*   **Repositories:** `findByUserId(String userId)`

### Add Temporary Fallback (Safety Net)
```java
if (user_uuid != null) {
    return use user_uuid;
} else {
    return fallback to old user_id;
}
```

---

## 🔷 Phase 4.5 — Production Validation
Monitor production for 1–2 days minimum:
*   `SELECT COUNT(*) FROM wallet WHERE user_uuid IS NULL;`
*   Check logs, errors, and cross-service flows.

---

## 🔷 Phase 5 — Cleanup (Final Step)

### Remove Old Columns
```sql
ALTER TABLE wallet DROP FOREIGN KEY fk_wallet_user;
ALTER TABLE wallet DROP COLUMN user_id;

ALTER TABLE wallet RENAME COLUMN user_uuid TO user_id;
ALTER TABLE wallet MODIFY COLUMN user_id VARCHAR(36) NOT NULL;
```

### Drop Old Tables
```sql
DROP TABLE wallet_db.users;
DROP TABLE wallet_db.organizations;
```

### Remove Fallback Logic from Code

---

## 🔁 Rollback Strategy

### Before Phase 5:
✔ Switch code back to use old `user_id`. Data is still intact.

### After Phase 5 (Hard Rollback):
👉 Restore from backup taken in Phase 0.

---

## 🔥 Key Safety Principles
✅ **Never trust raw email** → normalize + deduplicate.
✅ **Always verify 0 NULLs** before moving ahead.
✅ **Dual write** prevents live data inconsistency.
✅ **Keep rollback path** until final cleanup.
✅ **Validate referential integrity** before dropping FKs.
