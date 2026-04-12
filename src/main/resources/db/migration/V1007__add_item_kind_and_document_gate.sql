-- =============================================================================
-- V1007: Add item_kind column + supporting index for BOQ visibility gate
-- =============================================================================
-- item_kind mirrors the column added by Portal API V24.
-- IF NOT EXISTS guards against running after Portal has already applied V24.
--
-- The visibility gate itself is enforced in the Customer API query layer:
-- BoqItemRepository only returns items whose boq_document_id belongs to an
-- APPROVED boq_documents row (see BoqItemRepository.findApprovedByProjectId).
-- No DDL change is needed for the gate — boq_document_id and boq_documents
-- already exist from Portal API V19.
-- =============================================================================

ALTER TABLE boq_items
    ADD COLUMN IF NOT EXISTS item_kind VARCHAR(20) NOT NULL DEFAULT 'BASE';

-- Constraint is skipped if Portal already added it (ALTER TABLE ... ADD CONSTRAINT
-- will fail if it exists; use DO block to guard).
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'chk_boq_item_kind'
          AND conrelid = 'boq_items'::regclass
    ) THEN
        ALTER TABLE boq_items
            ADD CONSTRAINT chk_boq_item_kind
                CHECK (item_kind IN ('BASE', 'ADDON', 'OPTIONAL', 'EXCLUSION'));
    END IF;
END$$;

CREATE INDEX IF NOT EXISTS idx_boq_items_item_kind ON boq_items(item_kind);
