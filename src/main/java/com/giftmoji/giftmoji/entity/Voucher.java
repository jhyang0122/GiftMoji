package com.giftmoji.giftmoji.entity;

import jakarta.persistence.Column;
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

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@JdbcTypeCode(SqlTypes.CHAR)
	private UUID id;

	@Nationalized
	@Column(nullable = false)
	private String code;

	@Nationalized
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private VoucherStatus status;

	@Column(nullable = false)
	@JdbcTypeCode(SqlTypes.CHAR)
	private UUID itemId;

	@Column(nullable = false)
	@JdbcTypeCode(SqlTypes.CHAR)
	private UUID purchasedByUserId;

	@Column(nullable = false)
	@JdbcTypeCode(SqlTypes.CHAR)
	private UUID currentHolderId;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	@Column(nullable = false)
	private LocalDateTime expiresAt;

	private LocalDateTime redeemedAt;

	protected Voucher() {
		// JPA
	}

	public static Voucher purchase(String code, UUID itemId, UUID purchasedByUserId, LocalDateTime expiresAt) {
		Voucher voucher = new Voucher();
		voucher.code = code;
		voucher.status = VoucherStatus.PURCHASED;
		voucher.itemId = itemId;
		voucher.purchasedByUserId = purchasedByUserId;
		voucher.currentHolderId = purchasedByUserId;
		voucher.createdAt = LocalDateTime.now();
		voucher.expiresAt = expiresAt;
		return voucher;
	}

	public void send(UUID receiverId) {
		if (this.status != VoucherStatus.PURCHASED) {
			throw new IllegalStateException("Voucher must be PURCHASED to send, was " + this.status);
		}
		this.status = VoucherStatus.SENT;
		this.currentHolderId = receiverId;
	}

	// First-view-only: repeat views must not disturb an already-recorded
	// VIEWED transition (this also guards cancellation eligibility upstream).
	public void markViewed() {
		if (this.status == VoucherStatus.SENT) {
			this.status = VoucherStatus.VIEWED;
		}
	}

	public void cancel() {
		if (this.status != VoucherStatus.SENT) {
			throw new IllegalStateException("Voucher can only be cancelled while SENT and unviewed, was " + this.status);
		}
		this.status = VoucherStatus.CANCELLED;
		this.currentHolderId = purchasedByUserId;
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
