package com.giftmoji.giftmoji.api;

import com.giftmoji.giftmoji.entity.Gift;
import com.giftmoji.giftmoji.entity.Item;
import com.giftmoji.giftmoji.entity.User;
import com.giftmoji.giftmoji.entity.Voucher;
import com.giftmoji.giftmoji.entity.VoucherStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record GiftDetailResponse(
		UUID giftId,
		String itemName,
		String itemImageUrl,
		String counterpartyDisplayName,
		String counterpartyEmail,
		String message,
		VoucherStatus status,
		LocalDateTime sentAt,
		LocalDateTime viewedAt,
		LocalDateTime expiresAt,
		LocalDateTime redeemedAt,
		String code,
		String qrUrl
) {
	// Both sender and receiver can see the redemption code/QR for a gift
	// they're party to — the merchant-role redeem endpoint is the actual
	// access control on redemption, not knowledge of the code.
	public static GiftDetailResponse from(Gift gift, Voucher voucher, Item item, User counterparty) {
		return new GiftDetailResponse(
				gift.getId(), item.getName(), item.getImageUrl(),
				counterparty.getDisplayName(), counterparty.getEmail(), gift.getMessage(),
				voucher.getStatus(), gift.getSentAt(), gift.getViewedAt(), voucher.getExpiresAt(), voucher.getRedeemedAt(),
				voucher.getCode(), "/api/gifts/" + gift.getId() + "/qr");
	}
}
