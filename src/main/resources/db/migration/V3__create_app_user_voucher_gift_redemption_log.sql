CREATE TABLE app_user (
    id               CHAR(36)      NOT NULL PRIMARY KEY,
    google_id        NVARCHAR(255) NOT NULL UNIQUE,
    email            NVARCHAR(255) NOT NULL UNIQUE,
    display_name     NVARCHAR(200),
    picture_url      NVARCHAR(500),
    wallet_balance   DECIMAL(19,2) NOT NULL DEFAULT 0,
    merchant_staff   BIT           NOT NULL DEFAULT 0,
    created_at       DATETIME2     NOT NULL,
    last_login_at    DATETIME2     NOT NULL
);

CREATE TABLE voucher (
    id                    CHAR(36)      NOT NULL PRIMARY KEY,
    code                  NVARCHAR(64)  NOT NULL UNIQUE,
    status                NVARCHAR(20)  NOT NULL,
    item_id               CHAR(36)      NOT NULL REFERENCES item(id),
    purchased_by_user_id  CHAR(36)      NOT NULL REFERENCES app_user(id),
    current_holder_id     CHAR(36)      NOT NULL REFERENCES app_user(id),
    created_at            DATETIME2     NOT NULL,
    expires_at            DATETIME2     NOT NULL,
    redeemed_at           DATETIME2
);

-- Backs the expiry sweep's bulk UPDATE (status IN (...) AND expires_at < :cutoff).
CREATE INDEX idx_voucher_status_expires_at ON voucher(status, expires_at);

CREATE TABLE gift (
    id          CHAR(36)     NOT NULL PRIMARY KEY,
    voucher_id  CHAR(36)     NOT NULL REFERENCES voucher(id),
    sender_id   CHAR(36)     NOT NULL REFERENCES app_user(id),
    receiver_id CHAR(36)     NOT NULL REFERENCES app_user(id),
    message     NVARCHAR(1000),
    sent_at     DATETIME2    NOT NULL,
    viewed_at   DATETIME2
);

CREATE INDEX idx_gift_receiver_id ON gift(receiver_id);
CREATE INDEX idx_gift_sender_id ON gift(sender_id);
CREATE INDEX idx_gift_voucher_id ON gift(voucher_id);

CREATE TABLE redemption_log (
    id                  CHAR(36)     NOT NULL PRIMARY KEY,
    voucher_id          CHAR(36)     NOT NULL REFERENCES voucher(id),
    redeemed_by_role    NVARCHAR(30) NOT NULL,
    redeemed_by_user_id CHAR(36)     NOT NULL REFERENCES app_user(id),
    redeemed_at         DATETIME2    NOT NULL,
    metadata            NVARCHAR(1000)
);

CREATE INDEX idx_redemption_log_voucher_id ON redemption_log(voucher_id);
