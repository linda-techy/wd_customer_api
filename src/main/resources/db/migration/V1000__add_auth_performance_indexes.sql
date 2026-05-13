-- ========================================================================
-- Migration: Add Performance Indexes for Auth and Core Tables
-- Purpose: Prevent full table scans on token lookups, user emails, and
--          project queries that run on every authenticated request.
-- Date: 2026-03-09
--
-- 2026-05-13 fix: original revision referenced `refresh_tokens` and
-- `password_reset_tokens` but the actual customer-api tables are prefixed
-- with `customer_` (see AppConfig.REFRESH_TOKEN_TABLE). Also adds the
-- `customer_refresh_tokens` CREATE TABLE — previously it was auto-created
-- by Hibernate under ddl-auto=update; now ddl-auto=validate requires it
-- to come from a migration.
-- ========================================================================

-- Ensure customer_refresh_tokens exists before indexing it.
-- Columns match com.wd.custapi.model.RefreshToken (token length 64 chars
-- because we store SHA-256 hex hashes, never raw JWTs).
CREATE TABLE IF NOT EXISTS customer_refresh_tokens (
    id           BIGSERIAL    PRIMARY KEY,
    token        VARCHAR(64)  NOT NULL UNIQUE,
    user_id      BIGINT       NOT NULL REFERENCES customer_users(id),
    expiry_date  TIMESTAMP    NOT NULL,
    revoked      BOOLEAN      NOT NULL DEFAULT FALSE
);

-- customer_refresh_tokens.token — used on every API request via findByToken()
-- Without this, every API call does a full sequential scan of the token table.
CREATE INDEX IF NOT EXISTS idx_customer_refresh_tokens_token
ON customer_refresh_tokens(token);

-- customer_refresh_tokens.expiry_date — used by nightly cleanup job
CREATE INDEX IF NOT EXISTS idx_customer_refresh_tokens_expiry
ON customer_refresh_tokens(expiry_date);

-- customer_password_reset_tokens.email — used in deleteByEmail() and findByEmailAndResetCode()
-- Called on every forgot-password and reset-password request.
CREATE INDEX IF NOT EXISTS idx_customer_password_reset_email
ON customer_password_reset_tokens(email);

-- customer_users.email — used on every authentication request (findByEmail)
-- This is the hottest query path in the entire application.
CREATE INDEX IF NOT EXISTS idx_customer_users_email
ON customer_users(email);

-- customer_projects.customer_id — used in project membership queries
CREATE INDEX IF NOT EXISTS idx_customer_projects_customer_id
ON customer_projects(customer_id);

-- project_members.customer_user_id — used in access control joins
CREATE INDEX IF NOT EXISTS idx_project_members_customer_user_id
ON project_members(customer_user_id);

-- project_members.project_id — used in access control joins
CREATE INDEX IF NOT EXISTS idx_project_members_project_id
ON project_members(project_id);
