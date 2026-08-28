package com.giftmoji.giftmoji.api;

import com.giftmoji.giftmoji.entity.Gift;
import com.giftmoji.giftmoji.entity.Item;
import com.giftmoji.giftmoji.entity.User;
import com.giftmoji.giftmoji.entity.Voucher;
import com.giftmoji.giftmoji.entity.VoucherStatus;

import java.time.LocalDateTime;
import java.util.UUID;

// Deliberately excludes the voucher code/QR — list views never need the
// redemption secret, only the detail view does.
public record GiftSummaryResponse(
		UUID giftId,
		String itemName,
		String counterpartyDisplayName,
		String counterpartyEmail,
		String message,
		VoucherStatus status,
		LocalDateTime sentAt,
		LocalDateTime viewedAt,
		LocalDateTime expiresAt
) {
	public static GiftSummaryResponse from(Gift gift, Voucher voucher, Item item, User counterparty) {
		return new GiftSummaryResponse(
				gift.getId(), item.getName(), counterparty.getDisplayName(), counterparty.getEmail(),
				gift.getMessage(), voucher.getStatus(), gift.getSentAt(), gift.getViewedAt(), voucher.getExpiresAt());
	}
}
