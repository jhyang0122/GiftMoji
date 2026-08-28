package com.giftmoji.giftmoji.service;

import com.giftmoji.giftmoji.entity.Gift;
import com.giftmoji.giftmoji.entity.Voucher;

public sealed interface GiftViewResult {

	record Success(Gift gift, Voucher voucher) implements GiftViewResult {}

	record NotFound() implements GiftViewResult {}

	record Forbidden() implements GiftViewResult {}
}
