-- User.createFromGoogle/recordLogin now normalize email to lowercase on
-- write, and GiftingService.sendGift looks receivers up by lowercased
-- email. Backfill any row written before that change (e.g. under a
-- case-sensitive collation) so an existing user is findable by email
-- immediately, rather than only after their next login re-normalizes it.
UPDATE app_user SET email = LOWER(email) WHERE email <> LOWER(email);
