-- V1017: Backfill project_queries into support_tickets (§9-E consolidation, additive).
--
-- Real project_queries columns (verified from portal_api entity + Hibernate DDL):
--   id, project_id, asked_by (FK customer_users), subject, question (TEXT),
--   answer (TEXT), status (VARCHAR 50: OPEN/IN_PROGRESS/RESOLVED/CLOSED),
--   responded_by_id (FK portal_users), responded_at, created_at
--
-- ticket_number format 'PQ-<id>' is guaranteed not to collide with 'TKT-NNNNN'.
-- Both INSERTs are idempotent via NOT EXISTS guards.

-- 1. Copy each project_query row into support_tickets.
INSERT INTO support_tickets
    (ticket_number, customer_user_id, project_id, subject, description,
     category, priority, status, assigned_to, created_at, updated_at, resolved_at)
SELECT
    'PQ-' || pq.id,
    pq.asked_by,
    pq.project_id,
    pq.subject,
    COALESCE(pq.question, ''),
    'PROJECT_QUERY',
    'MEDIUM',
    CASE
        WHEN pq.status IN ('RESOLVED', 'CLOSED') THEN pq.status
        WHEN pq.status = 'IN_PROGRESS'           THEN 'IN_PROGRESS'
        ELSE 'OPEN'
    END,
    pq.responded_by_id,
    COALESCE(pq.created_at,  NOW()),
    COALESCE(pq.responded_at, pq.created_at, NOW()),
    CASE WHEN pq.status IN ('RESOLVED', 'CLOSED') THEN pq.responded_at ELSE NULL END
FROM project_queries pq
WHERE pq.asked_by IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM support_tickets st WHERE st.ticket_number = 'PQ-' || pq.id
  );

-- 2. For resolved queries that have an answer, insert a staff reply on the backfilled ticket.
INSERT INTO support_ticket_replies
    (ticket_id, user_id, user_type, user_name, message, created_at)
SELECT
    st.id,
    COALESCE(pq.responded_by_id, pq.asked_by),
    'STAFF',
    NULL,
    pq.answer,
    COALESCE(pq.responded_at, pq.created_at, NOW())
FROM project_queries pq
JOIN support_tickets st ON st.ticket_number = 'PQ-' || pq.id
WHERE pq.answer IS NOT NULL
  AND pq.answer <> ''
  AND NOT EXISTS (
      SELECT 1 FROM support_ticket_replies r
       WHERE r.ticket_id = st.id AND r.message = pq.answer
  );
