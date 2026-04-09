-- V1001: Add customer notifications table and FCM token column
-- Part of: BA Gap 8 — In-App Notification System
-- Date: 2026-03-22

-- FCM device token stored on the customer_users row (one active device per user).
-- Allows portal API to push targeted notifications to logged-in customers.
ALTER TABLE customer_users
    ADD COLUMN IF NOT EXISTS fcm_token VARCHAR(512);

-- Phone and WhatsApp columns (may already exist from portal API Hibernate DDL — safe with IF NOT EXISTS)
ALTER TABLE customer_users
    ADD COLUMN IF NOT EXISTS phone VARCHAR(50);

ALTER TABLE customer_users
    ADD COLUMN IF NOT EXISTS whatsapp VARCHAR(50);

-- In-app notification store for customer-facing notifications
CREATE TABLE IF NOT EXISTS customer_notifications (
    id            BIGSERIAL PRIMARY KEY,
    customer_user_id BIGINT NOT NULL REFERENCES customer_users(id) ON DELETE CASCADE,
    project_id    BIGINT REFERENCES customer_projects(id) ON DELETE SET NULL,
    title         VARCHAR(255) NOT NULL,
    body          TEXT,
    notification_type VARCHAR(50),   -- SITE_REPORT, PAYMENT, BOQ, DOCUMENT, MILESTONE, GENERAL
    reference_id  BIGINT,            -- ID of the linked entity (e.g. site_report_id, payment_schedule_id)
    is_read       BOOLEAN NOT NULL DEFAULT false,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Index for fast unread count query per user (used in notification bell badge)
CREATE INDEX IF NOT EXISTS idx_notif_user_unread
    ON customer_notifications(customer_user_id, is_read, created_at DESC);

-- Index for project-scoped notification queries
CREATE INDEX IF NOT EXISTS idx_notif_project
    ON customer_notifications(project_id, created_at DESC);
