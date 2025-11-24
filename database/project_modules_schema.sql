-- =====================================================
-- WD Builders - Customer Project Modules Schema
-- =====================================================
-- This schema contains all tables for the 11 project modules:
-- 1. Documents
-- 2. Quality Check
-- 3. Activity Feed
-- 4. Gallery
-- 5. Observations
-- 6. Queries
-- 7. Surveillance (CCTV)
-- 8. 360° View
-- 9. Site Visits
-- 10. Feedback
-- 11. BoQ (Bill of Quantities)
-- =====================================================

-- =====================================================
-- 1. DOCUMENTS MODULE
-- =====================================================

-- Document categories
CREATE TABLE IF NOT EXISTS document_categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    display_order INT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Insert default document categories
INSERT INTO document_categories (name, description, display_order, created_at)
VALUES
    ('Floor Plan Layout', 'Floor plan and layout drawings', 1, NOW()),
    ('3D Elevation', '3D elevation and exterior views', 2, NOW()),
    ('Detailed Project Costing', 'Project cost breakdowns and estimates', 3, NOW()),
    ('Structural Drawings', 'Structural engineering drawings', 4, NOW()),
    ('MEP', 'Mechanical, Electrical, and Plumbing drawings', 5, NOW()),
    ('Collaboration Agreement', 'Legal and collaboration documents', 6, NOW())
ON CONFLICT (name) DO NOTHING;

-- Project documents
CREATE TABLE IF NOT EXISTS project_documents (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    filename VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT,
    file_type VARCHAR(50),
    uploaded_by_id BIGINT NOT NULL,
    upload_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    description TEXT,
    version INT DEFAULT 1,
    is_active BOOLEAN DEFAULT true,
    FOREIGN KEY (project_id) REFERENCES customer_projects(id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES document_categories(id),
    FOREIGN KEY (uploaded_by_id) REFERENCES portal_users(id)
);

CREATE INDEX idx_project_documents_project ON project_documents(project_id);
CREATE INDEX idx_project_documents_category ON project_documents(category_id);

-- =====================================================
-- 2. QUALITY CHECK MODULE
-- =====================================================

-- Quality check items
CREATE TABLE IF NOT EXISTS quality_checks (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    sop_reference VARCHAR(100),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    priority VARCHAR(20) DEFAULT 'MEDIUM',
    assigned_to_id BIGINT,
    created_by_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP,
    resolved_by_id BIGINT,
    resolution_notes TEXT,
    CONSTRAINT chk_qc_status CHECK (status IN ('ACTIVE', 'RESOLVED')),
    CONSTRAINT chk_qc_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    FOREIGN KEY (project_id) REFERENCES customer_projects(id) ON DELETE CASCADE,
    FOREIGN KEY (assigned_to_id) REFERENCES customer_users(id),
    FOREIGN KEY (created_by_id) REFERENCES customer_users(id),
    FOREIGN KEY (resolved_by_id) REFERENCES customer_users(id)
);

CREATE INDEX idx_quality_checks_project ON quality_checks(project_id);
CREATE INDEX idx_quality_checks_status ON quality_checks(status);

-- =====================================================
-- 3. ACTIVITY FEED MODULE
-- =====================================================

-- Activity types enum-like table
CREATE TABLE IF NOT EXISTS activity_types (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    icon VARCHAR(50),
    color VARCHAR(20)
);

INSERT INTO activity_types (name, icon, color) VALUES
    ('SITE_REPORT_ADDED', 'description', 'blue'),
    ('QUERY_ADDED', 'help_outline', 'orange'),
    ('QUERY_RESOLVED', 'check_circle', 'green'),
    ('OBSERVATION_ADDED', 'visibility', 'purple'),
    ('OBSERVATION_RESOLVED', 'check', 'green'),
    ('DOCUMENT_UPLOADED', 'upload_file', 'blue'),
    ('QUALITY_CHECK_ADDED', 'task_alt', 'red'),
    ('QUALITY_CHECK_RESOLVED', 'done_all', 'green'),
    ('SITE_VISIT_LOGGED', 'location_on', 'teal'),
    ('FEEDBACK_SUBMITTED', 'rate_review', 'amber'),
    ('BOQ_UPDATED', 'receipt_long', 'indigo')
ON CONFLICT (name) DO NOTHING;

-- Activity feed entries
CREATE TABLE IF NOT EXISTS activity_feeds (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    activity_type_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    reference_id BIGINT,
    reference_type VARCHAR(50),
    created_by_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB,
    FOREIGN KEY (project_id) REFERENCES customer_projects(id) ON DELETE CASCADE,
    FOREIGN KEY (activity_type_id) REFERENCES activity_types(id),
    FOREIGN KEY (created_by_id) REFERENCES customer_users(id)
);

CREATE INDEX idx_activity_feeds_project ON activity_feeds(project_id);
CREATE INDEX idx_activity_feeds_created_at ON activity_feeds(created_at DESC);
CREATE INDEX idx_activity_feeds_type ON activity_feeds(activity_type_id);

-- =====================================================
-- 4. GALLERY MODULE
-- =====================================================

-- Gallery images
CREATE TABLE IF NOT EXISTS gallery_images (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    image_path VARCHAR(500) NOT NULL,
    thumbnail_path VARCHAR(500),
    caption TEXT,
    taken_date DATE NOT NULL,
    uploaded_by_id BIGINT NOT NULL,
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    site_report_id BIGINT,
    location_tag VARCHAR(255),
    tags VARCHAR(255)[],
    FOREIGN KEY (project_id) REFERENCES customer_projects(id) ON DELETE CASCADE,
    FOREIGN KEY (uploaded_by_id) REFERENCES customer_users(id)
);

CREATE INDEX idx_gallery_images_project ON gallery_images(project_id);
CREATE INDEX idx_gallery_images_date ON gallery_images(taken_date DESC);
CREATE INDEX idx_gallery_images_report ON gallery_images(site_report_id);

-- =====================================================
-- 5. OBSERVATIONS MODULE
-- =====================================================

-- Staff roles for observations
CREATE TABLE IF NOT EXISTS staff_roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

INSERT INTO staff_roles (name) VALUES
    ('Project Engineer'),
    ('Area Project Engineer'),
    ('Site Engineer'),
    ('Site Supervisor')
ON CONFLICT (name) DO NOTHING;

-- Observations (formerly Snag)
CREATE TABLE IF NOT EXISTS observations (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    reported_by_id BIGINT NOT NULL,
    reported_by_role_id BIGINT,
    reported_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    priority VARCHAR(20) DEFAULT 'MEDIUM',
    location VARCHAR(255),
    image_path VARCHAR(500),
    resolved_date TIMESTAMP,
    resolved_by_id BIGINT,
    resolution_notes TEXT,
    CONSTRAINT chk_obs_status CHECK (status IN ('ACTIVE', 'RESOLVED')),
    CONSTRAINT chk_obs_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH')),
    FOREIGN KEY (project_id) REFERENCES customer_projects(id) ON DELETE CASCADE,
    FOREIGN KEY (reported_by_id) REFERENCES customer_users(id),
    FOREIGN KEY (reported_by_role_id) REFERENCES staff_roles(id),
    FOREIGN KEY (resolved_by_id) REFERENCES customer_users(id)
);

CREATE INDEX idx_observations_project ON observations(project_id);
CREATE INDEX idx_observations_status ON observations(status);

-- =====================================================
-- 6. QUERIES MODULE
-- =====================================================

-- Queries
CREATE TABLE IF NOT EXISTS project_queries (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    raised_by_id BIGINT NOT NULL,
    raised_by_role_id BIGINT,
    raised_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    priority VARCHAR(20) DEFAULT 'MEDIUM',
    category VARCHAR(50),
    assigned_to_id BIGINT,
    resolved_date TIMESTAMP,
    resolved_by_id BIGINT,
    resolution TEXT,
    CONSTRAINT chk_query_status CHECK (status IN ('ACTIVE', 'RESOLVED')),
    CONSTRAINT chk_query_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),
    FOREIGN KEY (project_id) REFERENCES customer_projects(id) ON DELETE CASCADE,
    FOREIGN KEY (raised_by_id) REFERENCES customer_users(id),
    FOREIGN KEY (raised_by_role_id) REFERENCES staff_roles(id),
    FOREIGN KEY (assigned_to_id) REFERENCES customer_users(id),
    FOREIGN KEY (resolved_by_id) REFERENCES customer_users(id)
);

CREATE INDEX idx_project_queries_project ON project_queries(project_id);
CREATE INDEX idx_project_queries_status ON project_queries(status);

-- =====================================================
-- 7. SURVEILLANCE (CCTV) MODULE
-- =====================================================

-- CCTV cameras
CREATE TABLE IF NOT EXISTS cctv_cameras (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    camera_name VARCHAR(100) NOT NULL,
    location VARCHAR(255),
    stream_url VARCHAR(500),
    snapshot_url VARCHAR(500),
    is_installed BOOLEAN DEFAULT false,
    is_active BOOLEAN DEFAULT true,
    installation_date DATE,
    last_active TIMESTAMP,
    camera_type VARCHAR(50),
    resolution VARCHAR(20),
    notes TEXT,
    FOREIGN KEY (project_id) REFERENCES customer_projects(id) ON DELETE CASCADE
);

CREATE INDEX idx_cctv_cameras_project ON cctv_cameras(project_id);

-- =====================================================
-- 8. 360° VIEW MODULE
-- =====================================================

-- 360 degree views
CREATE TABLE IF NOT EXISTS view_360 (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    view_url VARCHAR(500) NOT NULL,
    thumbnail_url VARCHAR(500),
    capture_date DATE,
    location VARCHAR(255),
    uploaded_by_id BIGINT NOT NULL,
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT true,
    view_count INT DEFAULT 0,
    FOREIGN KEY (project_id) REFERENCES customer_projects(id) ON DELETE CASCADE,
    FOREIGN KEY (uploaded_by_id) REFERENCES customer_users(id)
);

CREATE INDEX idx_view_360_project ON view_360(project_id);

-- =====================================================
-- 9. SITE VISITS MODULE
-- =====================================================

-- Site visits
CREATE TABLE IF NOT EXISTS site_visits (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    visitor_id BIGINT NOT NULL,
    visitor_role_id BIGINT,
    check_in_time TIMESTAMP NOT NULL,
    check_out_time TIMESTAMP,
    purpose VARCHAR(255),
    notes TEXT,
    findings TEXT,
    location VARCHAR(255),
    weather_conditions VARCHAR(100),
    attendees VARCHAR(255)[],
    FOREIGN KEY (project_id) REFERENCES customer_projects(id) ON DELETE CASCADE,
    FOREIGN KEY (visitor_id) REFERENCES customer_users(id),
    FOREIGN KEY (visitor_role_id) REFERENCES staff_roles(id)
);

CREATE INDEX idx_site_visits_project ON site_visits(project_id);
CREATE INDEX idx_site_visits_visitor ON site_visits(visitor_id);
CREATE INDEX idx_site_visits_checkin ON site_visits(check_in_time DESC);

-- =====================================================
-- 10. FEEDBACK MODULE
-- =====================================================

-- Feedback forms/surveys
CREATE TABLE IF NOT EXISTS feedback_forms (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    form_type VARCHAR(50),
    created_by_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT true,
    FOREIGN KEY (project_id) REFERENCES customer_projects(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by_id) REFERENCES customer_users(id)
);

-- Feedback responses
CREATE TABLE IF NOT EXISTS feedback_responses (
    id BIGSERIAL PRIMARY KEY,
    form_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    rating INT,
    comments TEXT,
    response_data JSONB,
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_completed BOOLEAN DEFAULT true,
    CONSTRAINT chk_rating CHECK (rating >= 1 AND rating <= 5),
    FOREIGN KEY (form_id) REFERENCES feedback_forms(id) ON DELETE CASCADE,
    FOREIGN KEY (project_id) REFERENCES customer_projects(id) ON DELETE CASCADE,
    FOREIGN KEY (customer_id) REFERENCES customer_users(id)
);

CREATE INDEX idx_feedback_forms_project ON feedback_forms(project_id);
CREATE INDEX idx_feedback_responses_form ON feedback_responses(form_id);
CREATE INDEX idx_feedback_responses_customer ON feedback_responses(customer_id);

-- =====================================================
-- 11. BOQ (BILL OF QUANTITIES) MODULE
-- =====================================================

-- BoQ work types
CREATE TABLE IF NOT EXISTS boq_work_types (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    display_order INT DEFAULT 0
);

INSERT INTO boq_work_types (name, description, display_order) VALUES
    ('Foundation', 'Foundation and earthwork', 1),
    ('Masonry', 'Brick and block work', 2),
    ('Concrete', 'RCC and concrete works', 3),
    ('Steel', 'Structural steel work', 4),
    ('Electrical', 'Electrical installations', 5),
    ('Plumbing', 'Plumbing and sanitary', 6),
    ('Finishing', 'Plastering, painting, flooring', 7),
    ('HVAC', 'Heating, ventilation, and air conditioning', 8),
    ('Carpentry', 'Woodwork and carpentry', 9),
    ('Miscellaneous', 'Other works', 10)
ON CONFLICT (name) DO NOTHING;

-- BoQ items
CREATE TABLE IF NOT EXISTS boq_items (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    work_type_id BIGINT NOT NULL,
    item_code VARCHAR(50),
    description VARCHAR(500) NOT NULL,
    quantity DECIMAL(15, 3) NOT NULL,
    unit VARCHAR(20) NOT NULL,
    rate DECIMAL(15, 2) NOT NULL,
    amount DECIMAL(15, 2) GENERATED ALWAYS AS (quantity * rate) STORED,
    specifications TEXT,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by_id BIGINT NOT NULL,
    is_active BOOLEAN DEFAULT true,
    FOREIGN KEY (project_id) REFERENCES customer_projects(id) ON DELETE CASCADE,
    FOREIGN KEY (work_type_id) REFERENCES boq_work_types(id),
    FOREIGN KEY (created_by_id) REFERENCES customer_users(id)
);

CREATE INDEX idx_boq_items_project ON boq_items(project_id);
CREATE INDEX idx_boq_items_work_type ON boq_items(work_type_id);

-- BoQ item history for tracking changes
CREATE TABLE IF NOT EXISTS boq_item_history (
    id BIGSERIAL PRIMARY KEY,
    boq_item_id BIGINT NOT NULL,
    changed_by_id BIGINT NOT NULL,
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    field_changed VARCHAR(50) NOT NULL,
    old_value VARCHAR(255),
    new_value VARCHAR(255),
    FOREIGN KEY (boq_item_id) REFERENCES boq_items(id) ON DELETE CASCADE,
    FOREIGN KEY (changed_by_id) REFERENCES customer_users(id)
);

-- =====================================================
-- SUPPORTING TABLES
-- =====================================================

-- Site reports (referenced by activity feed and gallery)
CREATE TABLE IF NOT EXISTS site_reports (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    report_date DATE NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    weather VARCHAR(100),
    work_progress TEXT,
    manpower_deployed INT,
    equipment_used TEXT,
    created_by_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES customer_projects(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by_id) REFERENCES customer_users(id)
);

CREATE INDEX idx_site_reports_project ON site_reports(project_id);
CREATE INDEX idx_site_reports_date ON site_reports(report_date DESC);

-- Add foreign key for gallery images to site reports
ALTER TABLE gallery_images 
ADD CONSTRAINT fk_gallery_site_report 
FOREIGN KEY (site_report_id) REFERENCES site_reports(id) ON DELETE SET NULL;

-- =====================================================
-- COMMENTS/DOCUMENTATION
-- =====================================================

COMMENT ON TABLE project_documents IS 'Stores all project-related documents categorized by type';
COMMENT ON TABLE quality_checks IS 'Quality check SOP checklist items with active/resolved status';
COMMENT ON TABLE activity_feeds IS 'Timeline of all project activities and events';
COMMENT ON TABLE gallery_images IS 'Site photos and images grouped by date';
COMMENT ON TABLE observations IS 'Site observations and small faults noted by engineers';
COMMENT ON TABLE project_queries IS 'Queries and questions raised by project staff';
COMMENT ON TABLE cctv_cameras IS 'CCTV surveillance camera configuration and streams';
COMMENT ON TABLE view_360 IS '360-degree virtual reality views of the project';
COMMENT ON TABLE site_visits IS 'Log of site visits with check-in/check-out times';
COMMENT ON TABLE feedback_forms IS 'Customer feedback forms and surveys';
COMMENT ON TABLE feedback_responses IS 'Submitted feedback responses from customers';
COMMENT ON TABLE boq_items IS 'Bill of Quantities with work items and costs';
COMMENT ON TABLE site_reports IS 'Daily/periodic site progress reports';

-- =====================================================
-- SAMPLE DATA (Optional - for testing)
-- =====================================================

-- You can uncomment the following to insert sample data for testing

/*
-- Sample project
INSERT INTO customer_projects (name, code, location, start_date, end_date, progress)
VALUES ('Luxury Villa - Whitefield', 'PRJ-2025-001', 'Whitefield, Bangalore', '2025-01-15', '2025-12-31', 35.5)
ON CONFLICT DO NOTHING;
*/

-- End of schema

