-- ============================================================================
-- V1005: Fix BOQ column mapping and align precision with Portal API
-- ============================================================================
-- The Customer API entity previously mapped its Java fields 'rate' and 'amount'
-- to DB columns 'rate' and 'amount' (Hibernate ddl-auto default naming).
-- The Portal API (which owns and writes the boq_items table) uses 'unit_rate'
-- and 'total_amount'. This migration removes the spurious columns if they were
-- created by Hibernate ddl-auto, and aligns quantity precision to (18,6).
-- ============================================================================

-- Drop spurious columns added by Hibernate ddl-auto=update if they exist.
-- These columns were never written to by Portal API and contain no useful data.
ALTER TABLE boq_items DROP COLUMN IF EXISTS rate;
ALTER TABLE boq_items DROP COLUMN IF EXISTS amount;

-- Align quantity precision to industry standard (18,6) matching Portal API V2.
-- unit_rate, total_amount, executed_quantity, billed_quantity were already
-- upgraded to (18,6) by Portal API V2 migration.
ALTER TABLE boq_items ALTER COLUMN quantity TYPE NUMERIC(18,6);
