package com.giftmoji.giftmoji.api;

import com.giftmoji.giftmoji.entity.Merchant;

import java.util.UUID;

public record MerchantResponse(UUID id, String name, String description, String logoUrl) {

	public static MerchantResponse from(Merchant merchant) {
		return new MerchantResponse(merchant.getId(), merchant.getName(), merchant.getDescription(), merchant.getLogoUrl());
	}
}
