package com.giftmoji.giftmoji.api;

import com.giftmoji.giftmoji.entity.Voucher;
import com.giftmoji.giftmoji.entity.VoucherStatus;

import java.time.LocalDateTime;

public record VoucherResponse(
		String code,
		VoucherStatus status,
		LocalDateTime createdAt,
		LocalDateTime expiresAt,
		LocalDateTime redeemedAt,
		String qrUrl
) {
	public static VoucherResponse from(Voucher voucher) {
		return new VoucherResponse(
				voucher.getCode(),
				voucher.getStatus(),
				voucher.getCreatedAt(),
				voucher.getExpiresAt(),
				voucher.getRedeemedAt(),
				"/api/voucher/" + voucher.getCode() + "/qr"
		);
	}
}
