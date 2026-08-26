package com.giftmoji.giftmoji.service;

import com.giftmoji.giftmoji.entity.Gift;

public sealed interface GiftSendResult {

	record Success(Gift gift) implements GiftSendResult {}

	record InsufficientBalance() implements GiftSendResult {}

	record ItemNotFound() implements GiftSendResult {}

	record ReceiverNotFound() implements GiftSendResult {}

	record SelfGift() implements GiftSendResult {}
}
