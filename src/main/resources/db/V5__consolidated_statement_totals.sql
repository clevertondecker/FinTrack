-- Stores the bank-declared final amount on exactly one invoice in a multi-card group.
-- Card invoice totals remain purchase totals for reports and audit history.
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS statement_total_amount DECIMAL(15,2) NULL;
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS statement_paid_amount DECIMAL(15,2) NULL;
