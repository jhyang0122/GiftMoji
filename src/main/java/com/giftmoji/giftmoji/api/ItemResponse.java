package com.giftmoji.giftmoji.api;

import com.giftmoji.giftmoji.entity.Item;

import java.math.BigDecimal;
import java.util.UUID;

public record ItemResponse(
		UUID id,
		UUID merchantId,
		String name,
		String description,
		BigDecimal price,
		String imageUrl,
		int defaultExpiryDays
) {
	public static ItemResponse from(Item item) {
		return new ItemResponse(
				item.getId(), item.getMerchantId(), item.getName(), item.getDescription(),
				item.getPrice(), item.getImageUrl(), item.getDefaultExpiryDays());
	}
}
