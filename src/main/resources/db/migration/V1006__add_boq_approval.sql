-- ============================================================================
-- V1006: Add boq_approvals table for customer BOQ approval / change requests
-- ============================================================================
-- Customers can respond to a BOQ with one of two actions:
--   APPROVED        — customer confirms acceptance of the BOQ
--   CHANGE_REQUESTED — customer requests modifications with an optional message
-- Each submission inserts a new row; the latest row per project is the
-- current status (no UPDATE/DELETE — append-only audit trail).
-- ============================================================================

CREATE TABLE boq_approvals (
    id                 BIGSERIAL    PRIMARY KEY,
    project_id         BIGINT       NOT NULL REFERENCES customer_projects(id),
    customer_user_id   BIGINT       NOT NULL REFERENCES customer_users(id),
    status             VARCHAR(30)  NOT NULL,   -- 'APPROVED' | 'CHANGE_REQUESTED'
    message            TEXT,
    created_at         TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_boq_approvals_project    ON boq_approvals(project_id);
CREATE INDEX idx_boq_approvals_customer   ON boq_approvals(customer_user_id);
CREATE INDEX idx_boq_approvals_created_at ON boq_approvals(created_at DESC);
