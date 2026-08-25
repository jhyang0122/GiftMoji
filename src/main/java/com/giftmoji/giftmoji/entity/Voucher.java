package com.giftmoji.giftmoji.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Entity
@Table(name = "voucher")
@Getter
public class Voucher {

	// Timestamps are wall-clock Sydney local time regardless of the deployment
	// host's own default zone (Azure App Service defaults to UTC).
	public static final ZoneId LOCAL_ZONE = ZoneId.of("Australia/Sydney");

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	private String code;

	@Enumerated(EnumType.STRING)
	private VoucherStatus status;

	private LocalDateTime createdAt;

	private LocalDateTime expiresAt;

	private LocalDateTime redeemedAt;

	protected Voucher() {
		// JPA
	}

	public static Voucher issue(String code, LocalDateTime expiresAt) {
		Voucher voucher = new Voucher();
		voucher.code = code;
		voucher.status = VoucherStatus.ISSUED;
		voucher.createdAt = LocalDateTime.now(LOCAL_ZONE);
		voucher.expiresAt = expiresAt;
		return voucher;
	}

	public boolean isExpired(LocalDateTime now) {
		return now.isAfter(expiresAt);
	}

	public void markExpired() {
		this.status = VoucherStatus.EXPIRED;
	}

	public void markRedeemed(LocalDateTime now) {
		this.status = VoucherStatus.REDEEMED;
		this.redeemedAt = now;
	}
}
