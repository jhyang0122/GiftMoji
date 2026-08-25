package com.giftmoji.giftmoji.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "voucher")
@Getter
public class Voucher {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	private String code;

	@Enumerated(EnumType.STRING)
	private VoucherStatus status;

	private Instant createdAt;

	private Instant expiresAt;

	private Instant redeemedAt;

	protected Voucher() {
		// JPA
	}

	public static Voucher issue(String code, Instant expiresAt) {
		Voucher voucher = new Voucher();
		voucher.code = code;
		voucher.status = VoucherStatus.ISSUED;
		voucher.createdAt = Instant.now();
		voucher.expiresAt = expiresAt;
		return voucher;
	}

	public boolean isExpired(Instant now) {
		return now.isAfter(expiresAt);
	}

	public void markExpired() {
		this.status = VoucherStatus.EXPIRED;
	}

	public void markRedeemed(Instant now) {
		this.status = VoucherStatus.REDEEMED;
		this.redeemedAt = now;
	}
}
