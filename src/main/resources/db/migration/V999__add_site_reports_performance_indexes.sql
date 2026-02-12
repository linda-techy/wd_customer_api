-- ========================================================================
-- Migration: Add Performance Indexes for Site Reports
-- Purpose: Optimize query performance for site report retrieval
-- Author: AI Assistant
-- Date: 2026-02-13
-- ========================================================================

-- Index for filtering site reports by project and sorting by date
-- Supports queries like: SELECT * FROM site_reports WHERE project_id = ? ORDER BY report_date DESC
CREATE INDEX IF NOT EXISTS idx_site_reports_project_date 
ON site_reports(project_id, report_date DESC);

-- Index for IN clause queries on project_id
-- Supports queries like: SELECT * FROM site_reports WHERE project_id IN (...)
-- Note: PostgreSQL can use btree indexes for IN clauses efficiently
CREATE INDEX IF NOT EXISTS idx_site_reports_project_ids 
ON site_reports(project_id) 
WHERE project_id IS NOT NULL;

-- Index for site_report_photos lookups by site_report_id
-- Ensures efficient loading of photos when retrieving reports
CREATE INDEX IF NOT EXISTS idx_site_report_photos_report_id
ON site_report_photos(site_report_id);

-- Comments for documentation
COMMENT ON INDEX idx_site_reports_project_date IS 
'Performance index for filtering site reports by project and sorting by date';

COMMENT ON INDEX idx_site_reports_project_ids IS 
'Performance index for IN clause queries on project_id';

COMMENT ON INDEX idx_site_report_photos_report_id IS 
'Performance index for loading photos by site report ID';
