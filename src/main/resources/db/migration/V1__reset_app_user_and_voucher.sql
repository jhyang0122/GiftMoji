-- The app_user/voucher tables were previously managed by Hibernate's
-- ddl-auto=update with no migration history. Flyway now owns the schema
-- going forward; both tables are recreated from scratch in V3 with the
-- columns/FKs the full domain model needs. Pre-launch app, no data worth
-- preserving in either H2 (wiped on restart anyway) or Azure SQL.
DROP TABLE IF EXISTS voucher;
DROP TABLE IF EXISTS app_user;
