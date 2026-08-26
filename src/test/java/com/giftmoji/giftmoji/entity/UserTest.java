package com.giftmoji.giftmoji.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

	@Test
	void newUser_startsWithDefaultBalanceAndNoMerchantRole() {
		User user = User.createFromGoogle("sub", "a@b.com", "Ada", null);
		assertThat(user.getWalletBalance()).isEqualByComparingTo("50.00");
		assertThat(user.isMerchantStaff()).isFalse();
	}

	@Test
	void debit_reducesBalance_whenSufficient() {
		User user = User.createFromGoogle("sub", "a@b.com", "Ada", null);
		user.debit(new BigDecimal("20.00"));
		assertThat(user.getWalletBalance()).isEqualByComparingTo("30.00");
	}

	@Test
	void debit_throws_whenInsufficient() {
		User user = User.createFromGoogle("sub", "a@b.com", "Ada", null);
		assertThatThrownBy(() -> user.debit(new BigDecimal("999.00"))).isInstanceOf(IllegalStateException.class);
		assertThat(user.getWalletBalance()).isEqualByComparingTo("50.00");
	}

	@Test
	void credit_increasesBalance() {
		User user = User.createFromGoogle("sub", "a@b.com", "Ada", null);
		user.credit(new BigDecimal("10.00"));
		assertThat(user.getWalletBalance()).isEqualByComparingTo("60.00");
	}

	@Test
	void syncMerchantRole_togglesFlag() {
		User user = User.createFromGoogle("sub", "a@b.com", "Ada", null);
		user.syncMerchantRole(true);
		assertThat(user.isMerchantStaff()).isTrue();
		user.syncMerchantRole(false);
		assertThat(user.isMerchantStaff()).isFalse();
	}
}
