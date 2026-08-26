package com.giftmoji.giftmoji.api;

import com.giftmoji.giftmoji.entity.User;

public record UserResponse(String email, String displayName, String pictureUrl) {

	public static UserResponse from(User user) {
		return new UserResponse(user.getEmail(), user.getDisplayName(), user.getPictureUrl());
	}
}
