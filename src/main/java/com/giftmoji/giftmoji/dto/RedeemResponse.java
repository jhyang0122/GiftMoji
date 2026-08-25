package com.giftmoji.giftmoji.dto;

import com.giftmoji.giftmoji.entity.Voucher;
import com.giftmoji.giftmoji.entity.VoucherStatus;

public record RedeemResponse(String code, VoucherStatus status, String message) {

	public static RedeemResponse success(Voucher voucher) {
		return new RedeemResponse(voucher.getCode(), voucher.getStatus(), "Redeemed successfully.");
	}

	public static RedeemResponse alreadyRedeemed(Voucher voucher) {
		return new RedeemResponse(voucher.getCode(), voucher.getStatus(),
				"This voucher was already redeemed at " + voucher.getRedeemedAt() + ".");
	}

	public static RedeemResponse expired(Voucher voucher) {
		return new RedeemResponse(voucher.getCode(), voucher.getStatus(),
				"This voucher expired at " + voucher.getExpiresAt() + ".");
	}
}
