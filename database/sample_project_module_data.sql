-- =====================================================
-- Sample Data for All 11 Project Modules
-- =====================================================
-- Run this AFTER customer_users_schema.sql and project_modules_schema.sql
-- Password for all test users: password123
-- =====================================================

-- =====================================================
-- 1. CREATE SAMPLE PROJECTS
-- =====================================================

INSERT INTO customer_projects (name, code, location, start_date, end_date, progress)
VALUES 
    ('Luxury Villa - Whitefield', 'PRJ-2025-001', 'Whitefield, Bangalore', '2025-01-15', '2025-12-31', 45.5),
    ('Commercial Complex - MG Road', 'PRJ-2025-002', 'MG Road, Bangalore', '2024-11-01', '2026-06-30', 62.3),
    ('Residential Apartments - HSR Layout', 'PRJ-2025-003', 'HSR Layout, Bangalore', '2025-02-01', '2026-08-31', 28.7)
ON CONFLICT DO NOTHING;

-- Link projects to customers
INSERT INTO project_members (customer_id, project_id)
SELECT 
    u.id,
    p.id
FROM customer_users u
CROSS JOIN customer_projects p
WHERE u.email IN ('customer@test.com', 'john.doe@example.com', 'n@gmail.com')
ON CONFLICT DO NOTHING;

-- =====================================================
-- 2. DOCUMENTS MODULE - Sample Documents
-- =====================================================

-- Sample documents for Project 1 - Using REAL file from storage
INSERT INTO project_documents 
(project_id, category_id, filename, file_path, file_size, file_type, uploaded_by_id, description, version)
SELECT 
    p.id,
    (SELECT id FROM document_categories WHERE name = 'Floor Plan Layout'),
    'Ground_Floor_Plan.pdf',
    'projects/1/documents/ground-floor-plan-7294f446-3070-443b-aa62-4cd091d371a6.pdf',
    2456789,
    'application/pdf',
    (SELECT id FROM customer_users WHERE email = 'n@gmail.com'),
    'Ground floor architectural plan with dimensions',
    1
FROM customer_projects p
WHERE p.code = 'PRJ-2025-001'  -- This is Project 1: Luxury Villa - Whitefield
ON CONFLICT DO NOTHING;


-- Additional documents can be added here when available in storage
-- INSERT INTO project_documents (project_id, category_id, filename, file_path, file_size, file_type, uploaded_by_id, description, version)
-- VALUES (...)

-- =====================================================
-- 3. QUALITY CHECK MODULE - Sample Checks
-- =====================================================

INSERT INTO quality_checks (project_id, title, description, sop_reference, status, priority, created_by_id)
SELECT 
    (SELECT id FROM customer_projects WHERE code = 'PRJ-2025-001'),
    'Foundation Concrete Quality Inspection',
    'Inspect concrete mix ratio, slump test results, and curing process as per SOP',
    'SOP-QC-001',
    'ACTIVE',
    'HIGH',
    (SELECT id FROM customer_users WHERE email = 'customer@test.com')
ON CONFLICT DO NOTHING;

INSERT INTO quality_checks (project_id, title, description, sop_reference, status, priority, created_by_id, resolved_at, resolved_by_id, resolution_notes)
SELECT 
    (SELECT id FROM customer_projects WHERE code = 'PRJ-2025-001'),
    'Rebar Installation Check',
    'Verify rebar spacing, cover, and lap length as per structural drawings',
    'SOP-QC-002',
    'RESOLVED',
    'CRITICAL',
    (SELECT id FROM customer_users WHERE email = 'customer@test.com'),
    NOW() - INTERVAL '2 days',
    (SELECT id FROM customer_users WHERE email = 'john.doe@example.com'),
    'All rebar installation verified and approved. Cover maintained at 50mm.'
ON CONFLICT DO NOTHING;

INSERT INTO quality_checks (project_id, title, description, sop_reference, status, priority, created_by_id)
SELECT 
    (SELECT id FROM customer_projects WHERE code = 'PRJ-2025-001'),
    'Wall Alignment Verification',
    'Check verticality and alignment of walls using plumb bob and level',
    'SOP-QC-005',
    'ACTIVE',
    'MEDIUM',
    (SELECT id FROM customer_users WHERE email = 'customer@test.com')
ON CONFLICT DO NOTHING;

-- =====================================================
-- 4. ACTIVITY FEED - Sample Activities
-- =====================================================

INSERT INTO activity_feeds (project_id, activity_type_id, title, description, created_by_id, created_at)
SELECT 
    (SELECT id FROM customer_projects WHERE code = 'PRJ-2025-001'),
    (SELECT id FROM activity_types WHERE name = 'SITE_REPORT_ADDED'),
    'Daily Site Report Added',
    'Site report for foundation work completion',
    (SELECT id FROM customer_users WHERE email = 'customer@test.com'),
    NOW() - INTERVAL '1 hour'
ON CONFLICT DO NOTHING;

INSERT INTO activity_feeds (project_id, activity_type_id, title, description, created_by_id, created_at)
SELECT 
    (SELECT id FROM customer_projects WHERE code = 'PRJ-2025-001'),
    (SELECT id FROM activity_types WHERE name = 'DOCUMENT_UPLOADED'),
    'Document Uploaded',
    'Floor plan layout uploaded',
    (SELECT id FROM customer_users WHERE email = 'customer@test.com'),
    NOW() - INTERVAL '3 hours'
ON CONFLICT DO NOTHING;

INSERT INTO activity_feeds (project_id, activity_type_id, title, description, created_by_id, created_at)
SELECT 
    (SELECT id FROM customer_projects WHERE code = 'PRJ-2025-001'),
    (SELECT id FROM activity_types WHERE name = 'QUALITY_CHECK_RESOLVED'),
    'Quality Check Completed',
    'Rebar installation quality check approved',
    (SELECT id FROM customer_users WHERE email = 'john.doe@example.com'),
    NOW() - INTERVAL '2 days'
ON CONFLICT DO NOTHING;

INSERT INTO activity_feeds (project_id, activity_type_id, title, description, created_by_id, created_at)
SELECT 
    (SELECT id FROM customer_projects WHERE code = 'PRJ-2025-001'),
    (SELECT id FROM activity_types WHERE name = 'QUERY_ADDED'),
    'New Query Raised',
    'Query about electrical conduit routing',
    (SELECT id FROM customer_users WHERE email = 'customer@test.com'),
    NOW() - INTERVAL '5 hours'
ON CONFLICT DO NOTHING;

-- =====================================================
-- 5. GALLERY MODULE - Sample Images
-- =====================================================

-- Create sample site report first
INSERT INTO site_reports (project_id, report_date, title, description, weather, work_progress, manpower_deployed, created_by_id)
SELECT 
    (SELECT id FROM customer_projects WHERE code = 'PRJ-2025-001'),
    CURRENT_DATE,
    'Foundation Work Progress',
    'Foundation excavation completed, concrete pouring in progress',
    'Sunny, 28°C',
    'Foundation work 80% complete. Concrete pouring started at 7 AM.',
    25,
    (SELECT id FROM customer_users WHERE email = 'customer@test.com')
ON CONFLICT DO NOTHING;

-- Sample gallery images
INSERT INTO gallery_images (project_id, image_path, thumbnail_path, caption, taken_date, uploaded_by_id, site_report_id, location_tag)
SELECT 
    (SELECT id FROM customer_projects WHERE code = 'PRJ-2025-001'),
    'projects/1/gallery/foundation-work-uuid-1.jpg',
    'projects/1/gallery/thumbs/foundation-work-uuid-1.jpg',
    'Foundation excavation completed',
    CURRENT_DATE - INTERVAL '2 days',
    (SELECT id FROM customer_users WHERE email = 'customer@test.com'),
    (SELECT id FROM site_reports WHERE project_id = (SELECT id FROM customer_projects WHERE code = 'PRJ-2025-001') LIMIT 1),
    'Foundation Area'
ON CONFLICT DO NOTHING;

INSERT INTO gallery_images (project_id, image_path, thumbnail_path, caption, taken_date, uploaded_by_id, location_tag)
SELECT 
    (SELECT id FROM customer_projects WHERE code = 'PRJ-2025-001'),
    'projects/1/gallery/rebar-work-uuid-2.jpg',
    'projects/1/gallery/thumbs/rebar-work-uuid-2.jpg',
    'Rebar installation in progress',
    CURRENT_DATE - INTERVAL '1 day',
    (SELECT id FROM customer_users WHERE email = 'customer@test.com'),
    'Foundation Area'
ON CONFLICT DO NOTHING;

INSERT INTO gallery_images (project_id, image_path, thumbnail_path, caption, taken_date, uploaded_by_id, location_tag)
SELECT 
    (SELECT id FROM customer_projects WHERE code = 'PRJ-2025-001'),
    'projects/1/gallery/concrete-pour-uuid-3.jpg',
    'projects/1/gallery/thumbs/concrete-pour-uuid-3.jpg',
    'Concrete pouring for foundation',
    CURRENT_DATE,
    (SELECT id FROM customer_users WHERE email = 'customer@test.com'),
    'Foundation Area'
ON CONFLICT DO NOTHING;

-- =====================================================
-- 6. OBSERVATIONS MODULE - Sample Observations
-- =====================================================

INSERT INTO observations (project_id, title, description, reported_by_id, reported_by_role_id, status, priority, location)
SELECT 
    (SELECT id FROM customer_projects WHERE code = 'PRJ-2025-001'),
    'Minor crack in foundation wall',
    'Observed a hairline crack in the north-side foundation wall. Needs immediate inspection.',
    (SELECT id FROM customer_users WHERE email = 'customer@test.com'),
    (SELECT id FROM staff_roles WHERE name = 'Project Engineer'),
    'ACTIVE',
    'HIGH',
    'Foundation - North Wall'
ON CONFLICT DO NOTHING;

INSERT INTO observations (project_id, title, description, reported_by_id, reported_by_role_id, status, priority, location, resolved_date, resolved_by_id, resolution_notes)
SELECT 
    (SELECT id FROM customer_projects WHERE code = 'PRJ-2025-001'),
    'Water accumulation in excavation',
    'Rainwater accumulated in excavation pit. Required pumping before work continuation.',
    (SELECT id FROM customer_users WHERE email = 'customer@test.com'),
    (SELECT id FROM staff_roles WHERE name = 'Site Supervisor'),
    'RESOLVED',
    'MEDIUM',
    'Foundation Pit - Section A',
    NOW() - INTERVAL '1 day',
    (SELECT id FROM customer_users WHERE email = 'john.doe@example.com'),
    'Water pumped out successfully. Drainage system improved to prevent future accumulation.'
ON CONFLICT DO NOTHING;

INSERT INTO observations (project_id, title, description, reported_by_id, reported_by_role_id, status, priority, location)
SELECT 
    (SELECT id FROM customer_projects WHERE code = 'PRJ-2025-001'),
    'Improper material storage',
    'Construction materials stored too close to excavation edge. Safety hazard.',
    (SELECT id FROM customer_users WHERE email = 'customer@test.com'),
    (SELECT id FROM staff_roles WHERE name = 'Site Engineer'),
    'ACTIVE',
    'LOW',
    'Material Storage Area'
ON CONFLICT DO NOTHING;

-- =====================================================
-- 7. QUERIES MODULE - Sample Queries
-- =====================================================

INSERT INTO project_queries (project_id, title, description, raised_by_id, raised_by_role_id, status, priority, category)
SELECT 
    (SELECT id FROM customer_projects WHERE code = 'PRJ-2025-001'),
    'Electrical conduit routing clarification',
    'Need clarification on electrical conduit routing through foundation. Drawing shows conflict with plumbing lines.',
    (SELECT id FROM customer_users WHERE email = 'customer@test.com'),
    (SELECT id FROM staff_roles WHERE name = 'Site Engineer'),
    'ACTIVE',
    'HIGH',
    'Electrical'
ON CONFLICT DO NOTHING;

INSERT INTO project_queries (project_id, title, description, raised_by_id, raised_by_role_id, status, priority, category, resolved_date, resolved_by_id, resolution)
SELECT 
    (SELECT id FROM customer_projects WHERE code = 'PRJ-2025-001'),
    'Tile specification change approval',
    'Customer wants to change bathroom tile specification from original plan. Need approval and cost impact.',
    (SELECT id FROM customer_users WHERE email = 'customer@test.com'),
    (SELECT id FROM staff_roles WHERE name = 'Project Engineer'),
    'RESOLVED',
    'MEDIUM',
    'Materials',
    NOW() - INTERVAL '3 days',
    (SELECT id FROM customer_users WHERE email = 'john.doe@example.com'),
    'Change approved. Cost difference: +₹45,000. Updated BOQ and schedule accordingly.'
ON CONFLICT DO NOTHING;

INSERT INTO project_queries (project_id, title, description, raised_by_id, raised_by_role_id, status, priority, category)
SELECT 
    (SELECT id FROM customer_projects WHERE code = 'PRJ-2025-001'),
    'Parking layout modification',
    'Can we modify the parking layout to accommodate one additional vehicle?',
    (SELECT id FROM customer_users WHERE email = 'customer@test.com'),
    (SELECT id FROM staff_roles WHERE name = 'Site Supervisor'),
    'ACTIVE',
    'LOW',
    'Design'
ON CONFLICT DO NOTHING;

-- =====================================================
-- 8. CCTV MODULE - Sample Cameras
-- =====================================================

INSERT INTO cctv_cameras (project_id, camera_name, location, stream_url, snapshot_url, is_installed, is_active, installation_date, camera_type, resolution)
SELECT 
    (SELECT id FROM customer_projects WHERE code = 'PRJ-2025-001'),
    'Main Gate Camera',
    'Main Entrance Gate',
    'rtsp://camera1.example.com:554/stream',
    'http://camera1.example.com/snapshot.jpg',
    true,
    true,
    '2025-01-20',
    'PTZ',
    '1080p'
ON CONFLICT DO NOTHING;

INSERT INTO cctv_cameras (project_id, camera_name, location, stream_url, snapshot_url, is_installed, is_active, installation_date, camera_type, resolution)
SELECT 
    (SELECT id FROM customer_projects WHERE code = 'PRJ-2025-001'),
    'Foundation Area Camera',
    'Foundation Work Area',
    'rtsp://camera2.example.com:554/stream',
    'http://camera2.example.com/snapshot.jpg',
    true,
    true,
    '2025-01-20',
    'Fixed Dome',
    '4K'
ON CONFLICT DO NOTHING;

INSERT INTO cctv_cameras (project_id, camera_name, location, stream_url, is_installed, is_active, camera_type, resolution)
SELECT 
    (SELECT id FROM customer_projects WHERE code = 'PRJ-2025-001'),
    'Material Storage Camera',
    'Material Storage Yard',
    'rtsp://camera3.example.com:554/stream',
    true,
    true,
    'Bullet',
    '1080p'
ON CONFLICT DO NOTHING;

-- =====================================================
-- 9. 360° VIEW MODULE - Sample Views
-- =====================================================

INSERT INTO view_360 (project_id, title, description, view_url, thumbnail_url, capture_date, location, uploaded_by_id, view_count)
SELECT 
    (SELECT id FROM customer_projects WHERE code = 'PRJ-2025-001'),
    'Foundation 360° View',
    'Complete 360° view of foundation work area showing completed excavation and rebar installation',
    'https://360view.example.com/project1/foundation',
    'https://360view.example.com/project1/foundation/thumb.jpg',
    CURRENT_DATE - INTERVAL '5 days',
    'Foundation Area',
    (SELECT id FROM customer_users WHERE email = 'customer@test.com'),
    45
ON CONFLICT DO NOTHING;

INSERT INTO view_360 (project_id, title, description, view_url, thumbnail_url, capture_date, location, uploaded_by_id, view_count)
SELECT 
    (SELECT id FROM customer_projects WHERE code = 'PRJ-2025-001'),
    'Site Overview 360°',
    'Full site overview showing all work areas and material storage',
    'https://360view.example.com/project1/site-overview',
    'https://360view.example.com/project1/site-overview/thumb.jpg',
    CURRENT_DATE,
    'Complete Site',
    (SELECT id FROM customer_users WHERE email = 'customer@test.com'),
    23
ON CONFLICT DO NOTHING;

-- =====================================================
-- 10. SITE VISITS MODULE - Sample Visits
-- =====================================================

INSERT INTO site_visits (project_id, visitor_id, visitor_role_id, check_in_time, check_out_time, purpose, notes, findings, location, weather_conditions, attendees)
SELECT 
    (SELECT id FROM customer_projects WHERE code = 'PRJ-2025-001'),
    (SELECT id FROM customer_users WHERE email = 'customer@test.com'),
    (SELECT id FROM staff_roles WHERE name = 'Project Engineer'),
    NOW() - INTERVAL '2 days' - INTERVAL '6 hours',
    NOW() - INTERVAL '2 days' - INTERVAL '3 hours',
    'Weekly progress inspection',
    'Inspected foundation work progress and quality',
    'Foundation work progressing well. Minor issue with rebar spacing noted and corrected on site.',
    'Foundation Area',
    'Sunny, 30°C',
    ARRAY['Site Supervisor', 'Structural Engineer', 'Quality Inspector']
ON CONFLICT DO NOTHING;

INSERT INTO site_visits (project_id, visitor_id, visitor_role_id, check_in_time, check_out_time, purpose, notes, findings, weather_conditions)
SELECT 
    (SELECT id FROM customer_projects WHERE code = 'PRJ-2025-001'),
    (SELECT id FROM customer_users WHERE email = 'john.doe@example.com'),
    (SELECT id FROM staff_roles WHERE name = 'Area Project Engineer'),
    NOW() - INTERVAL '1 day' - INTERVAL '4 hours',
    NOW() - INTERVAL '1 day' - INTERVAL '2 hours',
    'Quality audit',
    'Conducted quality audit as per schedule',
    'Overall quality satisfactory. Recommended additional curing time for concrete.',
    'Partly cloudy, 27°C'
ON CONFLICT DO NOTHING;

-- Current ongoing visit (no checkout)
INSERT INTO site_visits (project_id, visitor_id, visitor_role_id, check_in_time, purpose, location, weather_conditions)
SELECT 
    (SELECT id FROM customer_projects WHERE code = 'PRJ-2025-001'),
    (SELECT id FROM customer_users WHERE email = 'customer@test.com'),
    (SELECT id FROM staff_roles WHERE name = 'Site Supervisor'),
    NOW() - INTERVAL '2 hours',
    'Daily supervision and coordination',
    'Entire Site',
    'Clear, 29°C'
ON CONFLICT DO NOTHING;

-- =====================================================
-- 11. FEEDBACK MODULE - Sample Forms & Responses
-- =====================================================

INSERT INTO feedback_forms (project_id, title, description, form_type, created_by_id)
SELECT 
    (SELECT id FROM customer_projects WHERE code = 'PRJ-2025-001'),
    'Foundation Phase Completion Survey',
    'Please provide your feedback on the foundation work completion quality and timeline',
    'Phase Completion',
    (SELECT id FROM customer_users WHERE email = 'customer@test.com')
ON CONFLICT DO NOTHING;

INSERT INTO feedback_forms (project_id, title, description, form_type, created_by_id)
SELECT 
    (SELECT id FROM customer_projects WHERE code = 'PRJ-2025-001'),
    'Monthly Progress Satisfaction Survey',
    'Your monthly feedback helps us improve our service quality',
    'Monthly Survey',
    (SELECT id FROM customer_users WHERE email = 'customer@test.com')
ON CONFLICT DO NOTHING;

-- Sample feedback response
INSERT INTO feedback_responses (form_id, project_id, customer_id, rating, comments, submitted_at)
SELECT 
    (SELECT id FROM feedback_forms WHERE title = 'Foundation Phase Completion Survey' LIMIT 1),
    (SELECT id FROM customer_projects WHERE code = 'PRJ-2025-001'),
    (SELECT id FROM customer_users WHERE email = 'john.doe@example.com'),
    5,
    'Excellent work quality and timely completion. Very satisfied with the foundation work.',
    NOW() - INTERVAL '1 day'
ON CONFLICT DO NOTHING;

-- =====================================================
-- 12. BOQ MODULE - Sample BOQ Items
-- =====================================================

INSERT INTO boq_items (project_id, work_type_id, item_code, description, quantity, unit, rate, specifications, created_by_id)
SELECT 
    (SELECT id FROM customer_projects WHERE code = 'PRJ-2025-001'),
    (SELECT id FROM boq_work_types WHERE name = 'Foundation'),
    'FND-001',
    'Excavation for foundation (including dewatering)',
    450.00,
    'Cum',
    350.00,
    'Machine excavation with manual trimming',
    (SELECT id FROM customer_users WHERE email = 'customer@test.com')
ON CONFLICT DO NOTHING;

INSERT INTO boq_items (project_id, work_type_id, item_code, description, quantity, unit, rate, specifications, created_by_id)
SELECT 
    (SELECT id FROM customer_projects WHERE code = 'PRJ-2025-001'),
    (SELECT id FROM boq_work_types WHERE name = 'Foundation'),
    'FND-002',
    'PCC M15 grade concrete',
    35.00,
    'Cum',
    5500.00,
    'Plain cement concrete 1:2:4 with 40mm aggregate',
    (SELECT id FROM customer_users WHERE email = 'customer@test.com')
ON CONFLICT DO NOTHING;

INSERT INTO boq_items (project_id, work_type_id, item_code, description, quantity, unit, rate, specifications, created_by_id)
SELECT 
    (SELECT id FROM customer_projects WHERE code = 'PRJ-2025-001'),
    (SELECT id FROM boq_work_types WHERE name = 'Foundation'),
    'FND-003',
    'RCC M25 grade concrete for foundation',
    125.00,
    'Cum',
    7800.00,
    'RCC with 12mm & 16mm TMT bars',
    (SELECT id FROM customer_users WHERE email = 'customer@test.com')
ON CONFLICT DO NOTHING;

INSERT INTO boq_items (project_id, work_type_id, item_code, description, quantity, unit, rate, specifications, created_by_id)
SELECT 
    (SELECT id FROM customer_projects WHERE code = 'PRJ-2025-001'),
    (SELECT id FROM boq_work_types WHERE name = 'Masonry'),
    'MSN-001',
    '230mm thick brick wall in cement mortar',
    850.00,
    'Sqm',
    580.00,
    'Class A bricks with 1:6 cement sand mortar',
    (SELECT id FROM customer_users WHERE email = 'customer@test.com')
ON CONFLICT DO NOTHING;

INSERT INTO boq_items (project_id, work_type_id, item_code, description, quantity, unit, rate, specifications, created_by_id)
SELECT 
    (SELECT id FROM customer_projects WHERE code = 'PRJ-2025-001'),
    (SELECT id FROM boq_work_types WHERE name = 'Electrical'),
    'ELE-001',
    'Electrical wiring with copper cables',
    1.00,
    'LS',
    185000.00,
    'Complete electrical installation as per approved drawings',
    (SELECT id FROM customer_users WHERE email = 'customer@test.com')
ON CONFLICT DO NOTHING;

INSERT INTO boq_items (project_id, work_type_id, item_code, description, quantity, unit, rate, specifications, created_by_id)
SELECT 
    (SELECT id FROM customer_projects WHERE code = 'PRJ-2025-001'),
    (SELECT id FROM boq_work_types WHERE name = 'Plumbing'),
    'PLB-001',
    'CPVC plumbing for water supply',
    1.00,
    'LS',
    145000.00,
    'Complete plumbing with CPVC pipes and fittings',
    (SELECT id FROM customer_users WHERE email = 'customer@test.com')
ON CONFLICT DO NOTHING;

INSERT INTO boq_items (project_id, work_type_id, item_code, description, quantity, unit, rate, specifications, created_by_id)
SELECT 
    (SELECT id FROM customer_projects WHERE code = 'PRJ-2025-001'),
    (SELECT id FROM boq_work_types WHERE name = 'Finishing'),
    'FIN-001',
    'Internal wall plastering 12mm thick',
    1650.00,
    'Sqm',
    185.00,
    'Cement sand plaster 1:4 with smooth finish',
    (SELECT id FROM customer_users WHERE email = 'customer@test.com')
ON CONFLICT DO NOTHING;

INSERT INTO boq_items (project_id, work_type_id, item_code, description, quantity, unit, rate, specifications, created_by_id)
SELECT 
    (SELECT id FROM customer_projects WHERE code = 'PRJ-2025-001'),
    (SELECT id FROM boq_work_types WHERE name = 'Finishing'),
    'FIN-002',
    'Premium emulsion paint (2 coats)',
    1650.00,
    'Sqm',
    95.00,
    'Asian Paints Royale or equivalent',
    (SELECT id FROM customer_users WHERE email = 'customer@test.com')
ON CONFLICT DO NOTHING;

-- =====================================================
-- Display Summary
-- =====================================================

SELECT 'Sample data inserted successfully!' as status;

-- Show project summary
SELECT 
    p.name,
    p.code,
    p.progress,
    COUNT(DISTINCT pd.id) as documents,
    COUNT(DISTINCT qc.id) as quality_checks,
    COUNT(DISTINCT af.id) as activities,
    COUNT(DISTINCT gi.id) as gallery_images,
    COUNT(DISTINCT o.id) as observations,
    COUNT(DISTINCT pq.id) as queries,
    COUNT(DISTINCT cc.id) as cctv_cameras,
    COUNT(DISTINCT v3.id) as view_360,
    COUNT(DISTINCT sv.id) as site_visits,
    COUNT(DISTINCT ff.id) as feedback_forms,
    COUNT(DISTINCT bi.id) as boq_items
FROM customer_projects p
LEFT JOIN project_documents pd ON p.id = pd.project_id
LEFT JOIN quality_checks qc ON p.id = qc.project_id
LEFT JOIN activity_feeds af ON p.id = af.project_id
LEFT JOIN gallery_images gi ON p.id = gi.project_id
LEFT JOIN observations o ON p.id = o.project_id
LEFT JOIN project_queries pq ON p.id = pq.project_id
LEFT JOIN cctv_cameras cc ON p.id = cc.project_id
LEFT JOIN view_360 v3 ON p.id = v3.project_id
LEFT JOIN site_visits sv ON p.id = sv.project_id
LEFT JOIN feedback_forms ff ON p.id = ff.project_id
LEFT JOIN boq_items bi ON p.id = bi.project_id
WHERE p.code = 'PRJ-2025-001'
GROUP BY p.id, p.name, p.code, p.progress;

-- End of sample data


