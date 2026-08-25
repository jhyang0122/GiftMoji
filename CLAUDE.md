# GiftMoji — Product Spec (MVP)

## 1. Overview

GiftMoji lets a sender buy a specific item/product voucher — a mobile 기프티콘-style gift (e.g. "1 Americano at Cafe X") — and send it digitally to a friend or family member, who redeems it via a QR/barcode-backed code. The app is mobile-first (responsive web is sufficient unless a native app is explicitly wanted later), with a Spring Boot / Java 21 / Gradle backend deployed to Azure App Service (F1 free tier).

The MVP is self-contained: there is no real merchant/POS integration. Redemption is simulated within the app itself, but modeled the way it works in the real product — a party other than the receiver validates and marks the code redeemed — rather than a receiver self-serve "mark as used" button, since that would skip the actual mechanic being demonstrated.

## 2. Target users

- **Sender** — wants to gift a specific item to someone without needing their address or payment details.
- **Receiver** — gets notified, views the voucher, and eventually redeems it.
- **Merchant (simulated for MVP)** — validates a voucher code and marks it redeemed. In the MVP this is a demo role inside GiftMoji itself, not a real onboarded business.

## 3. Core domain concepts

- **Merchant** — a business offering giftable items. MVP: seeded/admin-managed list, no merchant self-service portal.
- **Item** — a specific product a merchant offers as a voucher (name, price, image, default expiry, active flag).
- **Voucher** — an instance of an Item once purchased: has a unique code (backs the QR image), a status, an expiry date, and a current holder.
- **Gift** — the act of sending a Voucher from sender to receiver (sender, receiver, message, timestamps).
- **User** — sender/receiver account.
- **RedemptionLog** — audit record of when and how a voucher was redeemed.

## 4. Core user flows

### 4.1 Send a gift
1. Sender browses items by merchant/category.
2. Selects an item and pays (see open questions — real gateway vs. mocked balance for MVP).
3. Selects a receiver (by email/phone/username) and adds an optional message.
4. Confirms — a Voucher is created in `SENT` status; the receiver is notified.

### 4.2 Receive a gift
1. Receiver is notified (in-app minimum; push/email/SMS as stretch goals).
2. Opens GiftMoji and sees the gift in a "Received" list with the sender's message.
3. Views the voucher, including its QR/barcode and expiry date.

### 4.3 Redeem a gift
Recommended MVP approach: a separate "merchant" screen/role enters or scans the voucher code; the backend validates it's unexpired and unredeemed, then atomically marks it `REDEEMED`. This keeps the core value proposition intact — someone other than the receiver validates the code — even though no real merchant onboarding exists yet.

An alternative (receiver self-redeems with a button tap) is simpler to build but skips the part of the flow that actually matters, so it's not the recommended default.

### 4.4 Cancel / expire
- Sender can cancel an unredeemed voucher, but only while it's still `SENT` and unviewed — once the receiver has viewed it, cancellation is disabled so a gift can't be yanked back after the fact. Refund handling is an open question (see §8).
- Vouchers carry an expiry date (item-level default, e.g. 90 days). Expired vouchers can no longer be redeemed and show as expired in the receiver's list.

## 5. MVP feature list

**In scope:**
- User registration/login (email/password or OAuth)
- Browse merchants/items (seeded data — no merchant admin UI needed yet)
- Send a voucher to another registered user
- Notification of a received gift (in-app minimum)
- View a QR/barcode-backed voucher
- Redeem via a merchant-role screen (code entry or camera scan) with atomic status transition
- Voucher lifecycle: `PURCHASED → SENT → VIEWED → REDEEMED / EXPIRED / CANCELLED`
- Expiry handling (scheduled sweep or lazy check on read)

**Out of scope for MVP (future phases):**
- Real payment gateway integration
- Real merchant onboarding / self-service portal / POS integration
- Partial redemption or balance-based vouchers
- Refunds to original payment method
- Multi-currency / multi-region support
- Social features (group gifting, wishlists)

## 6. Voucher/QR security & edge cases

- The QR payload shouldn't be the only secret. Real 기프티콘 services pair a code with a barcode a clerk keys in or scans, sometimes plus a PIN. For MVP: generate a high-entropy or HMAC-signed token, render it as a QR code, and validate it server-side — never trust client-reported redemption state.
- Redemption must be atomic (a DB transaction or optimistic lock) so the same code can't be redeemed twice from concurrent requests — this, not preventing screenshots outright, is the real defense against a voucher being shared as an image: a second scan of an already-redeemed code just returns "already redeemed" with a timestamp.
- Expiry is checked server-side at redemption time, not just for display purposes.
- Regenerating the QR display periodically (short-lived display tokens) is a reasonable future hardening step, but likely overkill for MVP.

## 7. Data model sketch

Fits the existing Spring Boot / JPA / PostgreSQL stack:

- `User(id, name, email, phone, passwordHash, createdAt)`
- `Merchant(id, name, description, logoUrl)`
- `Item(id, merchantId, name, description, price, imageUrl, active, defaultExpiryDays)`
- `Voucher(id, itemId, code [unique, indexed], status, purchasedByUserId, currentHolderId, expiresAt, createdAt, redeemedAt)`
- `Gift(id, voucherId, senderId, receiverId, message, sentAt, viewedAt)`
- `RedemptionLog(id, voucherId, redeemedByRole, redeemedAt, metadata)`

## 8. Open questions

- **Payment:** a real gateway in test mode (e.g. Stripe), or a mocked wallet balance for MVP?
- **Notifications:** in-app only for v1, or also push/email/SMS?
- **Merchant role:** a genuine second "merchant" login, or an internal admin/staff-only redeem screen?
- **Identity:** email/password, or social login (e.g. Google) to reduce signup friction?
- **Refunds:** if a sender cancels before the receiver views it, does the sender get their payment back automatically, or is this manual/deferred to a later phase?

## 9. Non-functional notes

- Azure App Service F1 (free tier) means cold starts and limited resources — keep the app lightweight, avoid heavy background processing, and prefer a simple scheduled expiry sweep over a queuing system.
- Mobile-first responsive web satisfies "usable anywhere via smartphone" without committing to a native app.
