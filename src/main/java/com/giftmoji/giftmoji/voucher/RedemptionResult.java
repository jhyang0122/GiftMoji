package com.giftmoji.giftmoji.voucher;

public sealed interface RedemptionResult {

	record Success(Voucher voucher) implements RedemptionResult {}

	record AlreadyRedeemed(Voucher voucher) implements RedemptionResult {}

	record Expired(Voucher voucher) implements RedemptionResult {}

	record NotFound(String code) implements RedemptionResult {}
}
