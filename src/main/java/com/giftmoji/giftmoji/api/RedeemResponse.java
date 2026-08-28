package com.giftmoji.giftmoji.api;

import com.giftmoji.giftmoji.entity.Voucher;
import com.giftmoji.giftmoji.entity.VoucherStatus;

public record RedeemResponse(String code, VoucherStatus status, String itemName, String message) {

	public static RedeemResponse success(Voucher voucher, String itemName) {
		return new RedeemResponse(voucher.getCode(), voucher.getStatus(), itemName, "Redeemed successfully.");
	}

	public static RedeemResponse alreadyRedeemed(Voucher voucher, String itemName) {
		return new RedeemResponse(voucher.getCode(), voucher.getStatus(), itemName,
				"This voucher was already redeemed at " + voucher.getRedeemedAt() + ".");
	}

	public static RedeemResponse expired(Voucher voucher, String itemName) {
		return new RedeemResponse(voucher.getCode(), voucher.getStatus(), itemName,
				"This voucher expired at " + voucher.getExpiresAt() + ".");
	}

	public static RedeemResponse cancelled(Voucher voucher, String itemName) {
		return new RedeemResponse(voucher.getCode(), voucher.getStatus(), itemName,
				"This voucher was cancelled by the sender.");
	}
}
