package com.giftmoji.giftmoji.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Nationalized;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "voucher")
@Getter
public class Voucher {

	// The Flyway-managed schema uses CHAR(36) for UUID PKs/FKs (portable
	// across H2 and Azure SQL Server), not each dialect's native UUID type.
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@JdbcTypeCode(SqlTypes.CHAR)
	private UUID id;

	@Nationalized
	private String code;

	@Nationalized
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
		voucher.createdAt = LocalDateTime.now();
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
