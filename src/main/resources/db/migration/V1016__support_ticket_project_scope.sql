-- V1016: Optional project scoping for support tickets (§9-E consolidation foundation).
-- The project_id column + FK already exist from V1012/V1014; these statements are
-- idempotent guards so the column is present on any environment that predates them.
-- The new index backs the project-scoped customer finder
-- (findByCustomerUser_IdAndProjectIdOrderByCreatedAtDesc).

ALTER TABLE support_tickets ADD COLUMN IF NOT EXISTS project_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_support_tickets_project ON support_tickets(project_id);
