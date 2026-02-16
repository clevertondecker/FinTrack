-- Allow user_id to be NULL so item shares can be assigned to trusted contacts (no system user).
-- Run this if Hibernate ddl-auto=update did not alter the column (e.g. MySQL).
-- MySQL:
ALTER TABLE item_shares MODIFY COLUMN user_id BIGINT NULL;
