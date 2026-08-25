package com.giftmoji.giftmoji.voucher;

import java.time.Instant;

public record VoucherResponse(
		String code,
		VoucherStatus status,
		Instant createdAt,
		Instant expiresAt,
		Instant redeemedAt,
		String qrUrl
) {
	public static VoucherResponse from(Voucher voucher) {
		return new VoucherResponse(
				voucher.getCode(),
				voucher.getStatus(),
				voucher.getCreatedAt(),
				voucher.getExpiresAt(),
				voucher.getRedeemedAt(),
				"/api/vouchers/" + voucher.getCode() + "/qr"
		);
	}
}
