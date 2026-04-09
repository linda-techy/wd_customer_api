-- ============================================================
-- V1003: Soft-delete columns + refresh token hash column resize
-- ============================================================
-- 1. Add deleted_at to 6 high-traffic entities so that portal
--    soft-deletes are respected by the customer API JPA queries
--    (@SQLDelete + @Where annotations rely on this column).
--
-- 2. Shrink refresh_tokens.token from VARCHAR(255) to VARCHAR(64)
--    now that we store SHA-256 hex hashes (always 64 chars).
-- ============================================================

-- Soft-delete columns
ALTER TABLE customer_projects  ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE observations        ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE site_reports        ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE payment_schedule    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE gallery_images      ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE project_documents   ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

-- Performance: partial indexes on un-deleted rows for the most common customer queries
CREATE INDEX IF NOT EXISTS idx_customer_projects_deleted
    ON customer_projects(id) WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_site_reports_project_deleted
    ON site_reports(project_id, report_date DESC) WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_gallery_images_project_deleted
    ON gallery_images(project_id, taken_date DESC) WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_observations_project_deleted
    ON observations(project_id, status) WHERE deleted_at IS NULL;

-- Refresh token column: shrink to 64 chars (SHA-256 hash length)
-- Safe to run even if the table already has rows — existing plaintext tokens will
-- no longer match hashed lookups, so all current sessions will be invalidated on
-- the next refresh attempt (customers must log in once — expected behaviour).
ALTER TABLE refresh_tokens ALTER COLUMN token TYPE VARCHAR(64);
