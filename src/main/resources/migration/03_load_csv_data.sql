-- =============================================================================
-- PHASE 0: LOAD CSV DATA INTO STAGING TABLES
-- We create a temporary schema and tables to hold the raw CSV data before migration.
-- =============================================================================
CREATE SCHEMA IF NOT EXISTS staging;

-- 1. KYC Staging Tables
CREATE TABLE IF NOT EXISTS staging.kyc_users (
    id BIGINT, created_at TIMESTAMP, updated_at TIMESTAMP, dob DATE, 
    email VARCHAR, is_active BOOLEAN, last_login_at TIMESTAMP, 
    mobile_number VARCHAR, name VARCHAR, password_hash VARCHAR, tenant_id VARCHAR
);
CREATE TABLE IF NOT EXISTS staging.kyc_tenants (
    id BIGINT, tenant_id VARCHAR, name VARCHAR, email VARCHAR, plan VARCHAR, 
    is_active BOOLEAN, max_daily_attempts INTEGER, allowed_document_types VARCHAR, 
    api_key VARCHAR, created_at TIMESTAMP, updated_at TIMESTAMP
);
CREATE TABLE IF NOT EXISTS staging.kyc_roles (id BIGINT, created_at TIMESTAMP, updated_at TIMESTAMP, name VARCHAR);
CREATE TABLE IF NOT EXISTS staging.kyc_user_roles (id BIGINT, created_at TIMESTAMP, updated_at TIMESTAMP, role_id BIGINT, user_id BIGINT);

-- 2. Wallet Staging Tables
CREATE TABLE IF NOT EXISTS staging.wallet_users (
    id BIGINT, username VARCHAR, email VARCHAR, password_hash VARCHAR, 
    created_at VARCHAR, city VARCHAR, phone_number VARCHAR, 
    organization_id BIGINT, role VARCHAR, status VARCHAR
);
CREATE TABLE IF NOT EXISTS staging.wallet_orgs (
    id BIGINT, created_at VARCHAR, name VARCHAR, org_code VARCHAR, status VARCHAR
);

-- 3. Voucher Staging Tables
CREATE TABLE IF NOT EXISTS staging.voucher_users (
    id BIGINT, created_at TIMESTAMP, email VARCHAR, enabled BOOLEAN, 
    first_name VARCHAR, last_name VARCHAR, password_hash VARCHAR, 
    phone_number VARCHAR, tenant_id BIGINT
);
CREATE TABLE IF NOT EXISTS staging.voucher_tenants (
    id BIGINT, tenant_code VARCHAR, tenant_name VARCHAR, tenant_type VARCHAR, 
    active BOOLEAN, created_at TIMESTAMP
);
CREATE TABLE IF NOT EXISTS staging.voucher_roles (id BIGINT, description VARCHAR, name VARCHAR);
CREATE TABLE IF NOT EXISTS staging.voucher_user_roles (user_id BIGINT, role_id BIGINT);

-- 4. Order Service Staging Tables
CREATE TABLE IF NOT EXISTS staging.order_users (
    id VARCHAR, created_by VARCHAR, created_date TIMESTAMP, email VARCHAR, 
    enabled BOOLEAN, is_deleted BOOLEAN, password VARCHAR, updated_by VARCHAR, 
    updated_date TIMESTAMP, role_id INTEGER, organization_id VARCHAR
);
CREATE TABLE IF NOT EXISTS staging.order_orgs (
    id VARCHAR, name VARCHAR, code VARCHAR, is_active BOOLEAN, 
    created_at TIMESTAMP, updated_at TIMESTAMP, created_by VARCHAR, 
    updated_by VARCHAR, description VARCHAR
);
CREATE TABLE IF NOT EXISTS staging.order_roles (
    id INTEGER, created_by VARCHAR, created_date TIMESTAMP, role_name VARCHAR, 
    updated_by VARCHAR, updated_date TIMESTAMP
);
CREATE TABLE IF NOT EXISTS staging.order_customers (
    id VARCHAR, address VARCHAR, phone VARCHAR, created_by VARCHAR, 
    created_date TIMESTAMP, first_name VARCHAR, last_name VARCHAR, 
    updated_by VARCHAR, updated_date TIMESTAMP, app_user_id VARCHAR, 
    organization_id VARCHAR
);

-- =============================================================================
-- DATA IMPORT (Reads from /tmp/auth_migration/ to avoid permission issues)
-- =============================================================================
COPY staging.kyc_users FROM '/tmp/auth_migration/users_202603311505.csv' WITH (FORMAT csv, HEADER true);
COPY staging.kyc_tenants FROM '/tmp/auth_migration/tenants_202603311510.csv' WITH (FORMAT csv, HEADER true);
COPY staging.kyc_roles FROM '/tmp/auth_migration/roles_202603311510.csv' WITH (FORMAT csv, HEADER true);
COPY staging.kyc_user_roles FROM '/tmp/auth_migration/user_roles_202603311508.csv' WITH (FORMAT csv, HEADER true);

COPY staging.wallet_users FROM '/tmp/auth_migration/users_202603311518.csv' WITH (FORMAT csv, HEADER true);
COPY staging.wallet_orgs FROM '/tmp/auth_migration/organizations_202603311517.csv' WITH (FORMAT csv, HEADER true);

COPY staging.voucher_users FROM '/tmp/auth_migration/users.csv' WITH (FORMAT csv, HEADER true);
COPY staging.voucher_tenants FROM '/tmp/auth_migration/tenants.csv' WITH (FORMAT csv, HEADER true);
COPY staging.voucher_roles FROM '/tmp/auth_migration/role.csv' WITH (FORMAT csv, HEADER true);
COPY staging.voucher_user_roles FROM '/tmp/auth_migration/users_role.csv' WITH (FORMAT csv, HEADER true);

COPY staging.order_users FROM '/tmp/auth_migration/app_user' WITH (FORMAT csv, HEADER false);
COPY staging.order_orgs FROM '/tmp/auth_migration/organization' WITH (FORMAT csv, HEADER false);
COPY staging.order_roles FROM '/tmp/auth_migration/user_role' WITH (FORMAT csv, HEADER false);
COPY staging.order_customers FROM '/tmp/auth_migration/customer' WITH (FORMAT csv, HEADER false);
