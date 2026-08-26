# GiftMoji domain model & flow

This document describes the backend domain model and the send → receive →
redeem gifting flow as implemented (see `feature/flyway-schema-migrations`,
`feature/gift-voucher-domain-model`, `feature/gifting-redemption-flow`, and
`feature/backend-domain-tests`). For the original product requirements this
implements, see [`CLAUDE.md`](../CLAUDE.md).

## Domain model

```mermaid
erDiagram
    MERCHANT ||--o{ ITEM : offers
    ITEM ||--o{ VOUCHER : "instance of"
    USER ||--o{ VOUCHER : "purchased (purchasedByUserId)"
    USER ||--o{ VOUCHER : "currently holds (currentHolderId)"
    VOUCHER ||--|| GIFT : "gifted as"
    USER ||--o{ GIFT : sends
    USER ||--o{ GIFT : receives
    VOUCHER ||--o{ REDEMPTION_LOG : "redeemed via"
    USER ||--o{ REDEMPTION_LOG : "staff redeemedBy"

    MERCHANT {
        UUID id PK
        string name
        string description
        string logoUrl
    }
    ITEM {
        UUID id PK
        UUID merchantId FK
        string name
        decimal price
        boolean active
        int defaultExpiryDays
    }
    USER {
        UUID id PK
        string googleId
        string email
        decimal walletBalance
        boolean merchantStaff
    }
    VOUCHER {
        UUID id PK
        string code
        VoucherStatus status
        UUID itemId FK
        UUID purchasedByUserId FK
        UUID currentHolderId FK
        datetime expiresAt
        datetime redeemedAt
    }
    GIFT {
        UUID id PK
        UUID voucherId FK
        UUID senderId FK
        UUID receiverId FK
        string message
        datetime sentAt
        datetime viewedAt
    }
    REDEMPTION_LOG {
        UUID id PK
        UUID voucherId FK
        string redeemedByRole
        UUID redeemedByUserId FK
        datetime redeemedAt
    }
```

**Notes on the model:**
- `User` has no password — it's Google-OAuth-only, upserted on login. It
  carries a mocked wallet (`walletBalance`, seeded at `$50` on creation —
  there's no real payment gateway in this MVP) and a `merchantStaff` flag
  used for the redeem-screen authorization (see below).
- `Voucher.currentHolderId` starts equal to `purchasedByUserId` and moves to
  the receiver once the gift is sent — it's who's "holding" the voucher
  right now, independent of who paid for it.
- `Gift` is the join between a `Voucher` and the sender/receiver pair. A
  voucher has at most one gift (the send happens once, atomically, with the
  voucher).
- `RedemptionLog` is an audit trail, written once per successful redemption
  by `RedemptionService` — never mutated afterwards.

## Voucher lifecycle

```mermaid
stateDiagram-v2
    [*] --> PURCHASED : sender buys an item (wallet debited)
    PURCHASED --> SENT : send to receiver
    SENT --> VIEWED : receiver opens the gift (first view only)
    SENT --> CANCELLED : sender cancels (only while unviewed)
    PURCHASED --> EXPIRED : expiry sweep / lazy check
    SENT --> EXPIRED : expiry sweep / lazy check
    VIEWED --> EXPIRED : expiry sweep / lazy check
    PURCHASED --> REDEEMED : merchant-role redeem
    SENT --> REDEEMED : merchant-role redeem
    VIEWED --> REDEEMED : merchant-role redeem
    REDEEMED --> [*]
    CANCELLED --> [*]
    EXPIRED --> [*]
```

Redemption is allowed from `PURCHASED`, `SENT`, or `VIEWED` — a merchant
doesn't require the receiver to have opened the gift first. Cancellation is
narrower: only from `SENT` and only before `Gift.viewedAt` is set, so a gift
can't be pulled back once the receiver has seen it (spec §4.4).

Expiry is enforced two ways: a `@Scheduled` sweep (`VoucherExpirySweepService`,
every 15 minutes) bulk-transitions overdue vouchers for list-accuracy, and
`VoucherService.redeem()` independently re-checks `expiresAt` at redemption
time regardless of stored status — the sweep is a UX nicety, not the actual
security boundary.

## End-to-end flow

```mermaid
sequenceDiagram
    actor Sender
    actor Receiver
    actor MerchantStaff
    participant API as Backend API
    participant DB as Database

    Sender->>API: GET /api/merchants, /api/merchants/{id}/items
    API->>DB: browse catalog
    API-->>Sender: merchants & items

    Sender->>API: POST /api/gifts {itemId, receiverEmail, message}
    API->>DB: lock sender row, check balance
    alt insufficient balance
        API-->>Sender: 402 Payment Required
    else ok
        API->>DB: debit sender wallet
        API->>DB: create Voucher (PURCHASED → SENT), create Gift
        API-->>Sender: 201 GiftDetailResponse
    end

    Receiver->>API: GET /api/gifts/received
    API-->>Receiver: list of gifts (no code/QR)

    Receiver->>API: GET /api/gifts/{giftId}
    API->>DB: first view? Voucher SENT → VIEWED, Gift.viewedAt set
    API-->>Receiver: GiftDetailResponse (includes code + QR)

    opt sender cancels before receiver views
        Sender->>API: POST /api/gifts/{giftId}/cancel
        API->>DB: Voucher SENT → CANCELLED, refund sender wallet
        API-->>Sender: 200 (or 409 if already viewed)
    end

    MerchantStaff->>API: POST /api/merchant/redeem {code}
    Note over API: requires ROLE_MERCHANT
    API->>DB: lock voucher row (pessimistic write)
    alt already redeemed / cancelled / expired
        API-->>MerchantStaff: 409 / 410 with reason
    else redeemable
        API->>DB: Voucher → REDEEMED, write RedemptionLog
        API-->>MerchantStaff: 200 success
    end
```

## API surface

| Method | Path | Auth | Purpose |
|---|---|---|---|
| `GET` | `/api/merchants` | authenticated | list merchants |
| `GET` | `/api/merchants/{id}/items` | authenticated | list a merchant's active items |
| `GET` | `/api/items/{id}` | authenticated | fetch one active item |
| `POST` | `/api/gifts` | authenticated | buy an item and send it to a receiver by email |
| `GET` | `/api/gifts/received` | authenticated | gifts sent to me |
| `GET` | `/api/gifts/sent` | authenticated | gifts I sent |
| `GET` | `/api/gifts/{id}` | authenticated, sender or receiver only | gift detail (marks first view) |
| `GET` | `/api/gifts/{id}/qr` | authenticated, sender or receiver only | voucher QR as PNG |
| `POST` | `/api/gifts/{id}/cancel` | authenticated, sender only | cancel before the receiver views it |
| `POST` | `/api/merchant/redeem` | authenticated + `ROLE_MERCHANT` | redeem a voucher by code |
| `GET` | `/api/auth/me` | public (401 if not logged in) | current user |

`ROLE_MERCHANT` is granted via a config-driven email allowlist
(`giftmoji.merchant-emails`) checked on every login — there's no admin UI
yet to manage staff accounts, so this is the deliberately simple MVP
mechanism (see `GiftMojiOidcUserService`).

## Redemption atomicity

Two concurrent redeem requests for the same code must not both succeed.
`VoucherRepository.findByCodeForRedemption` takes a `PESSIMISTIC_WRITE` lock
inside `VoucherService.redeem()`'s transaction, so the second request blocks
until the first commits and then sees the already-`REDEEMED` status. This is
covered by a concurrency test (`RedemptionServiceTest`) that fires 10
simultaneous redemptions at one voucher and asserts exactly one `Success`
and exactly one `RedemptionLog` row.
