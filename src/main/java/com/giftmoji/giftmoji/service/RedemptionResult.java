package com.giftmoji.giftmoji.service;

import com.giftmoji.giftmoji.entity.Voucher;

public sealed interface RedemptionResult {

	record Success(Voucher voucher) implements RedemptionResult {}

	record AlreadyRedeemed(Voucher voucher) implements RedemptionResult {}

	record Expired(Voucher voucher) implements RedemptionResult {}

	record NotFound(String code) implements RedemptionResult {}
}
