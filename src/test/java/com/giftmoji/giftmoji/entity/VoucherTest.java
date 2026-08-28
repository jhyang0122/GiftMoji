package com.giftmoji.giftmoji.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VoucherTest {

	@Test
	void fullLifecycle_purchaseSendViewRedeem() {
		LocalDateTime expiresAt = LocalDateTime.now().plusDays(90);
		Voucher voucher = Voucher.purchase("code123", UUID.randomUUID(), UUID.randomUUID(), expiresAt);
		assertThat(voucher.getStatus()).isEqualTo(VoucherStatus.PURCHASED);
		UUID sender = voucher.getCurrentHolderId();

		UUID receiver = UUID.randomUUID();
		voucher.send(receiver);
		assertThat(voucher.getStatus()).isEqualTo(VoucherStatus.SENT);
		assertThat(voucher.getCurrentHolderId()).isEqualTo(receiver);
		assertThat(voucher.getCurrentHolderId()).isNotEqualTo(sender);

		voucher.markViewed();
		assertThat(voucher.getStatus()).isEqualTo(VoucherStatus.VIEWED);

		voucher.markRedeemed(LocalDateTime.now());
		assertThat(voucher.getStatus()).isEqualTo(VoucherStatus.REDEEMED);
		assertThat(voucher.getRedeemedAt()).isNotNull();
	}

	@Test
	void markViewed_isIdempotent_onceAlreadyViewed() {
		Voucher voucher = Voucher.purchase("code", UUID.randomUUID(), UUID.randomUUID(), LocalDateTime.now().plusDays(1));
		voucher.send(UUID.randomUUID());
		voucher.markViewed();
		voucher.markViewed();
		assertThat(voucher.getStatus()).isEqualTo(VoucherStatus.VIEWED);
	}

	@Test
	void cancel_onlyAllowedWhileSent() {
		Voucher voucher = Voucher.purchase("code", UUID.randomUUID(), UUID.randomUUID(), LocalDateTime.now().plusDays(1));
		assertThatThrownBy(voucher::cancel).isInstanceOf(IllegalStateException.class);

		voucher.send(UUID.randomUUID());
		voucher.cancel();
		assertThat(voucher.getStatus()).isEqualTo(VoucherStatus.CANCELLED);

		assertThatThrownBy(voucher::cancel).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void isExpired_comparesAgainstExpiresAt() {
		Voucher voucher = Voucher.purchase("code", UUID.randomUUID(), UUID.randomUUID(), LocalDateTime.now().minusSeconds(1));
		assertThat(voucher.isExpired(LocalDateTime.now())).isTrue();

		Voucher notExpired = Voucher.purchase("code2", UUID.randomUUID(), UUID.randomUUID(), LocalDateTime.now().plusDays(1));
		assertThat(notExpired.isExpired(LocalDateTime.now())).isFalse();
	}
}
