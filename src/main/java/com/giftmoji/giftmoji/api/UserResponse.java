package com.giftmoji.giftmoji.api;

import com.giftmoji.giftmoji.entity.User;

import java.math.BigDecimal;

public record UserResponse(String email, String displayName, String pictureUrl, BigDecimal walletBalance, boolean merchantStaff) {

	public static UserResponse from(User user) {
		return new UserResponse(user.getEmail(), user.getDisplayName(), user.getPictureUrl(), user.getWalletBalance(), user.isMerchantStaff());
	}
}
