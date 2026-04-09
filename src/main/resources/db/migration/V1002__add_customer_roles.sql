-- =============================================================================
-- V1002 — Expand Customer Role Set (8 roles)
-- =============================================================================
-- Adds 3 new customer portal roles and updates display descriptions for the
-- existing 5 roles. Existing role codes are NEVER changed so existing
-- customer_users rows require no data migration.
--
-- New roles:
--   CUSTOMER_ADMIN — Customer portal administrator (full access)
--   CONTRACTOR     — Contractor (tasks/progress; no financials)
--   BUILDER        — Builder (tasks/progress; no financials)
-- =============================================================================

-- Update descriptions for the 5 existing roles (human-readable improvement)
UPDATE customer_roles
SET description = 'Primary project owner / main client — full access including financials'
WHERE name = 'CUSTOMER';

UPDATE customer_roles
SET description = 'Architecture firm — technical plans and construction progress; financial data hidden'
WHERE name = 'ARCHITECT';

UPDATE customer_roles
SET description = 'Interior design team — design scope, interior milestones and progress; financial data hidden'
WHERE name = 'INTERIOR_DESIGNER';

UPDATE customer_roles
SET description = 'On-site engineer or contractor — tasks, schedules and progress timeline; financial data hidden'
WHERE name = 'SITE_ENGINEER';

UPDATE customer_roles
SET description = 'Read-only access — views overall project progress and current phase only'
WHERE name = 'VIEWER';

-- Insert 3 new roles (idempotent)
INSERT INTO customer_roles (name, description)
SELECT v.name, v.description
FROM (VALUES
    ('CUSTOMER_ADMIN', 'Customer portal administrator — full project access including financials and member management'),
    ('CONTRACTOR',     'Contractor — views tasks, schedules and progress timeline; financial data hidden'),
    ('BUILDER',        'Builder — views tasks, schedules and progress timeline; financial data hidden')
) AS v(name, description)
WHERE NOT EXISTS (
    SELECT 1 FROM customer_roles cr WHERE cr.name = v.name
);
