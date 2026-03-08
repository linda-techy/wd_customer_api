-- ========================================================================
-- Migration: Add Performance Indexes for Auth and Core Tables
-- Purpose: Prevent full table scans on token lookups, user emails, and
--          project queries that run on every authenticated request.
-- Date: 2026-03-09
-- ========================================================================

-- refresh_tokens.token — used on every API request via findByToken()
-- Without this, every API call does a full sequential scan of the token table.
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_token
ON refresh_tokens(token);

-- refresh_tokens.expiry_date — used by nightly cleanup job
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expiry
ON refresh_tokens(expiry_date);

-- password_reset_tokens.email — used in deleteByEmail() and findByEmailAndResetCode()
-- Called on every forgot-password and reset-password request.
CREATE INDEX IF NOT EXISTS idx_password_reset_email
ON password_reset_tokens(email);

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
