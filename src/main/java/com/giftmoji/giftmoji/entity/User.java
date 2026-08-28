package com.giftmoji.giftmoji.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Nationalized;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
// "user" is a reserved word in SQL Server, so name the table explicitly.
@Table(name = "app_user")
@Getter
public class User {

	// MVP has no real payment gateway (spec §8) — every new user starts with
	// a mocked wallet balance instead of a top-up flow.
	private static final BigDecimal DEFAULT_STARTING_BALANCE = new BigDecimal("50.00");

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@JdbcTypeCode(SqlTypes.CHAR)
	private UUID id;

	@Nationalized
	@Column(nullable = false, unique = true)
	private String googleId;

	@Nationalized
	@Column(nullable = false, unique = true)
	private String email;

	@Nationalized
	private String displayName;

	@Nationalized
	private String pictureUrl;

	@Column(nullable = false)
	private BigDecimal walletBalance;

	@Column(nullable = false)
	private boolean merchantStaff;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	@Column(nullable = false)
	private LocalDateTime lastLoginAt;

	protected User() {
		// JPA
	}

	public static User createFromGoogle(String googleId, String email, String displayName, String pictureUrl) {
		User user = new User();
		user.googleId = googleId;
		user.email = email.toLowerCase();
		user.displayName = displayName;
		user.pictureUrl = pictureUrl;
		user.walletBalance = DEFAULT_STARTING_BALANCE;
		user.merchantStaff = false;
		LocalDateTime now = LocalDateTime.now();
		user.createdAt = now;
		user.lastLoginAt = now;
		return user;
	}

	public void recordLogin(String email, String displayName, String pictureUrl) {
		this.email = email.toLowerCase();
		this.displayName = displayName;
		this.pictureUrl = pictureUrl;
		this.lastLoginAt = LocalDateTime.now();
	}

	// Applied on every login (not just creation) so config-allowlist edits
	// take effect on redeploy without a manual DB fix.
	public void syncMerchantRole(boolean isMerchant) {
		this.merchantStaff = isMerchant;
	}

	public boolean hasSufficientBalance(BigDecimal amount) {
		return walletBalance.compareTo(amount) >= 0;
	}

	public void debit(BigDecimal amount) {
		if (!hasSufficientBalance(amount)) {
			throw new IllegalStateException("Insufficient wallet balance");
		}
		this.walletBalance = walletBalance.subtract(amount);
	}

	public void credit(BigDecimal amount) {
		this.walletBalance = walletBalance.add(amount);
	}
}
