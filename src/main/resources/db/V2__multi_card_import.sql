-- Migration: Multi-card import support
-- This script fixes the 'Data truncated for column status' error on AWS
-- by converting the status column from ENUM to VARCHAR and adding new columns.

-- 1. Fix invoice_imports.status column: convert ENUM to VARCHAR to support new status values
--    (PENDING_REVIEW was added but MySQL ENUM columns don't auto-update with ddl-auto=update)
ALTER TABLE invoice_imports MODIFY COLUMN status VARCHAR(255) NOT NULL;

-- 2. Add created_invoice_ids column for multi-card imports (stores JSON array of invoice IDs)
ALTER TABLE invoice_imports ADD COLUMN IF NOT EXISTS created_invoice_ids TEXT;

-- 3. Add import_group_id column to invoices (groups invoices from same multi-card import)
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS import_group_id VARCHAR(36);
