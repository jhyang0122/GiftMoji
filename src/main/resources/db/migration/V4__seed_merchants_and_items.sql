-- Seeded/admin-managed catalog for MVP (spec: no merchant self-service
-- portal yet). Fixed UUID literals keep rows stable/referenceable across
-- environments. created_at uses a fixed timestamp for reproducibility.

INSERT INTO merchant (id, name, description, logo_url, created_at) VALUES
('a0000000-0000-0000-0000-000000000001', 'Brew & Bean Coffee', 'Neighborhood coffee roaster and cafe.', NULL, '2026-01-01T00:00:00'),
('a0000000-0000-0000-0000-000000000002', 'Cornerstone Bakery', 'Fresh-baked donuts, cakes, and pastries.', NULL, '2026-01-01T00:00:00'),
('a0000000-0000-0000-0000-000000000003', 'Green Leaf Spa', 'Massage and skincare treatments.', NULL, '2026-01-01T00:00:00');

INSERT INTO item (id, merchant_id, name, description, price, image_url, active, default_expiry_days, created_at) VALUES
('b0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', 'Signature Latte', 'A 12oz signature latte, any milk.', 5.50, NULL, 1, 90, '2026-01-01T00:00:00'),
('b0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000001', 'Cold Brew', 'A 16oz cold brew coffee.', 4.75, NULL, 1, 90, '2026-01-01T00:00:00'),
('b0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000001', 'Pastry Combo', 'Any pastry paired with a drip coffee.', 8.00, NULL, 1, 90, '2026-01-01T00:00:00'),
('b0000000-0000-0000-0000-000000000004', 'a0000000-0000-0000-0000-000000000002', 'Dozen Donuts', 'A dozen assorted donuts.', 12.00, NULL, 1, 90, '2026-01-01T00:00:00'),
('b0000000-0000-0000-0000-000000000005', 'a0000000-0000-0000-0000-000000000002', 'Birthday Cake Slice', 'One slice of celebration cake.', 6.00, NULL, 1, 90, '2026-01-01T00:00:00'),
('b0000000-0000-0000-0000-000000000006', 'a0000000-0000-0000-0000-000000000003', '60-Minute Massage', 'A full-body relaxation massage.', 75.00, NULL, 1, 180, '2026-01-01T00:00:00'),
('b0000000-0000-0000-0000-000000000007', 'a0000000-0000-0000-0000-000000000003', 'Facial Treatment', 'A rejuvenating facial treatment.', 60.00, NULL, 1, 180, '2026-01-01T00:00:00');
