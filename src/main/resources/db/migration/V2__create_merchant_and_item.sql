-- UUID PKs/FKs use CHAR(36) uniformly (not native UUID/UNIQUEIDENTIFIER)
-- so the same DDL is valid on both H2 (local dev) and Azure SQL Server
-- (prod) without vendor-specific migration trees. Free-text columns use
-- NVARCHAR (not VARCHAR) because SQL Server's plain VARCHAR is
-- non-Unicode by default and would corrupt emoji/non-ASCII content.
CREATE TABLE merchant (
    id          CHAR(36)      NOT NULL PRIMARY KEY,
    name        NVARCHAR(200) NOT NULL,
    description NVARCHAR(1000),
    logo_url    NVARCHAR(500),
    created_at  DATETIME2     NOT NULL
);

CREATE TABLE item (
    id                  CHAR(36)      NOT NULL PRIMARY KEY,
    merchant_id         CHAR(36)      NOT NULL REFERENCES merchant(id),
    name                NVARCHAR(200) NOT NULL,
    description         NVARCHAR(1000),
    price               DECIMAL(19,2) NOT NULL,
    image_url           NVARCHAR(500),
    active              BIT           NOT NULL DEFAULT 1,
    default_expiry_days INT           NOT NULL,
    created_at          DATETIME2     NOT NULL
);

CREATE INDEX idx_item_merchant_id ON item(merchant_id);
