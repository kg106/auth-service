-- =============================================================================
-- CENTRAL AUTH SERVICE - AUTHORITATIVE SCHEMA (RESTART READY)
-- =============================================================================

DROP SCHEMA IF EXISTS auth CASCADE;
CREATE SCHEMA auth;
SET search_path TO auth;

-- =============================================================================
-- 1) ROLES TABLE
-- Lookup table for roles
-- =============================================================================
CREATE TABLE IF NOT EXISTS auth.roles (
    id BIGINT PRIMARY KEY,
    name CHARACTER VARYING(100) UNIQUE NOT NULL,
    created_at TIMESTAMP(6) WITHOUT TIME ZONE DEFAULT NOW()
);

-- =============================================================================
-- 1.5) STATUSES TABLE
-- =============================================================================
CREATE TABLE IF NOT EXISTS auth.statuses (
    id SERIAL PRIMARY KEY,
    name CHARACTER VARYING(20) UNIQUE NOT NULL
);

INSERT INTO auth.statuses (name) VALUES 
('ACTIVE'), ('INACTIVE'), ('SUSPENDED'), ('DELETED')
ON CONFLICT (name) DO NOTHING;

-- =============================================================================
-- 2) TENANTS TABLE (Organizations)
-- MIGRATION STRATEGY: Primary Key MUST be UUID to guarantee global uniqueness
-- =============================================================================
CREATE TABLE IF NOT EXISTS auth.tenants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id CHARACTER VARYING(100) UNIQUE NOT NULL, -- Logical String ID (e.g., 'T001')
    name CHARACTER VARYING(100) UNIQUE NOT NULL,      -- Enforced name uniqueness
    email CHARACTER VARYING(100) NOT NULL,
    plan CHARACTER VARYING(20) DEFAULT 'BASIC',
    status CHARACTER VARYING(20) DEFAULT 'ACTIVE' NOT NULL, -- ACTIVE, INACTIVE, SUSPENDED
    is_active BOOLEAN DEFAULT true NOT NULL,
    max_daily_attempts INTEGER DEFAULT 5 NOT NULL,
    allowed_document_types CHARACTER VARYING(200) DEFAULT 'PAN,AADHAAR',
    api_key CHARACTER VARYING(255),
    created_at TIMESTAMP(6) WITHOUT TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP(6) WITHOUT TIME ZONE DEFAULT NOW()
);

-- =============================================================================
-- 3) USERS TABLE (Centralized Identity)
-- MIGRATION STRATEGY: 
-- 1. Primary Key MUST be UUID
-- 2. `tenant_id` is now a Foreign Key UUID mapped directly to `tenants.id`
-- 3. `role_id` is now a direct Foreign Key on the user
-- =============================================================================
CREATE TABLE IF NOT EXISTS auth.users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES auth.tenants(id) ON DELETE CASCADE,
    email CHARACTER VARYING(100) NOT NULL,
    mobile_number CHARACTER VARYING(15) NOT NULL,
    name CHARACTER VARYING(100) NOT NULL,
    tenant_name CHARACTER VARYING(100),                -- Denormalized for convenience
    role_id BIGINT NOT NULL REFERENCES auth.roles(id), -- Flat Role Mapping
    password_hash CHARACTER VARYING(255) NOT NULL,
    dob DATE,
    status CHARACTER VARYING(20) DEFAULT 'ACTIVE' NOT NULL, -- ACTIVE, BANNED, PENDING
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_login_at TIMESTAMP(6) WITHOUT TIME ZONE,
    created_at TIMESTAMP(6) WITHOUT TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP(6) WITHOUT TIME ZONE DEFAULT NOW(),

    CONSTRAINT uq_user_email_tenant UNIQUE (email, tenant_id)
);

-- =============================================================================
-- 5) SEED DEFAULT ROLES
-- =============================================================================
INSERT INTO auth.roles (id, name) VALUES 
    (1, 'USER'),
    (2, 'ADMIN'),
    (3, 'TENANT_ADMIN'),
    (4, 'SUPER_ADMIN')
ON CONFLICT (id) DO NOTHING;

-- =============================================================================
-- 6) INDEXES FOR PERFORMANCE
-- =============================================================================
CREATE INDEX idx_auth_users_email ON auth.users(email);
CREATE INDEX idx_auth_users_tenant_id ON auth.users(tenant_id);
