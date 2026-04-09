-- V1004: Add created_by_type discriminators, fix Observation ACTIVE enum,
--         denormalize site_report submitter name, add optimistic lock version.

-- 1. Add created_by_type to site_visits
--    STAFF = visit logged by portal staff; CUSTOMER = visit logged by customer self-check-in
ALTER TABLE site_visits
    ADD COLUMN IF NOT EXISTS created_by_type VARCHAR(10) NOT NULL DEFAULT 'CUSTOMER';
CREATE INDEX IF NOT EXISTS idx_site_visits_created_by_type
    ON site_visits (created_by_type) WHERE created_by_type IS NOT NULL;

-- 2. Add uploaded_by_type to gallery_images
ALTER TABLE gallery_images
    ADD COLUMN IF NOT EXISTS uploaded_by_type VARCHAR(10) NOT NULL DEFAULT 'CUSTOMER';
CREATE INDEX IF NOT EXISTS idx_gallery_images_uploaded_by_type
    ON gallery_images (uploaded_by_type);

-- 3. Add uploaded_by_type to project_documents
ALTER TABLE project_documents
    ADD COLUMN IF NOT EXISTS uploaded_by_type VARCHAR(10) NOT NULL DEFAULT 'PORTAL';
CREATE INDEX IF NOT EXISTS idx_project_documents_uploaded_by_type
    ON project_documents (uploaded_by_type);

-- 4. Denormalize submitter name on site_reports
--    (portal_users FK is not available to customer API; store name at write time)
ALTER TABLE site_reports
    ADD COLUMN IF NOT EXISTS submitted_by_name VARCHAR(150);

-- 5. Fix duplicate ACTIVE status — migrate to OPEN (idempotent)
UPDATE observations SET status = 'OPEN' WHERE status = 'ACTIVE';

-- 6. Add optimistic-lock version column to customer_projects
ALTER TABLE customer_projects
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
