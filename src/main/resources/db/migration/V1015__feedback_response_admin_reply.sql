-- Card 4.13: admin→customer feedback response loop
-- All columns are NULLABLE so existing rows are unaffected.
-- ADD COLUMN IF NOT EXISTS is idempotent.
ALTER TABLE feedback_responses ADD COLUMN IF NOT EXISTS admin_response TEXT;
ALTER TABLE feedback_responses ADD COLUMN IF NOT EXISTS admin_responded_at TIMESTAMP;
ALTER TABLE feedback_responses ADD COLUMN IF NOT EXISTS admin_responded_by_id BIGINT;
