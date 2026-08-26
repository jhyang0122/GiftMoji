package com.giftmoji.giftmoji.api;

import java.util.UUID;

public record SendGiftRequest(UUID itemId, String receiverEmail, String message) {
}
