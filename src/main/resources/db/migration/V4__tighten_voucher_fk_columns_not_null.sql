-- V3 left these nullable because Voucher.java didn't map them yet. It now
-- does (feature/gifting-redemption-flow), and every voucher is created via
-- Voucher.purchase(...), which always sets all three — so backfill is a
-- no-op on any environment that only ever wrote through that path, and
-- these can be safely tightened back to NOT NULL.
ALTER TABLE voucher ALTER COLUMN item_id CHAR(36) NOT NULL;
ALTER TABLE voucher ALTER COLUMN purchased_by_user_id CHAR(36) NOT NULL;
ALTER TABLE voucher ALTER COLUMN current_holder_id CHAR(36) NOT NULL;
