package com.giftmoji.giftmoji.service;

import com.giftmoji.giftmoji.entity.Gift;
import com.giftmoji.giftmoji.entity.Voucher;

public sealed interface GiftCancelResult {

	record Success(Gift gift, Voucher voucher) implements GiftCancelResult {}

	record NotFound() implements GiftCancelResult {}

	record Forbidden() implements GiftCancelResult {}

	record NotCancellable() implements GiftCancelResult {}
}
