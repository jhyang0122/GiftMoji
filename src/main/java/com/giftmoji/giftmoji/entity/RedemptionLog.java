package com.giftmoji.giftmoji.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "redemption_log")
@Getter
public class RedemptionLog {

	public static final String ROLE_MERCHANT_STAFF = "MERCHANT_STAFF";

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@JdbcTypeCode(SqlTypes.CHAR)
	private UUID id;

	@Column(nullable = false)
	@JdbcTypeCode(SqlTypes.CHAR)
	private UUID voucherId;

	@Column(nullable = false)
	private String redeemedByRole;

	@Column(nullable = false)
	@JdbcTypeCode(SqlTypes.CHAR)
	private UUID redeemedByUserId;

	@Column(nullable = false)
	private LocalDateTime redeemedAt;

	private String metadata;

	protected RedemptionLog() {
		// JPA
	}

	public static RedemptionLog forMerchantStaffRedemption(UUID voucherId, UUID redeemedByUserId, LocalDateTime now) {
		RedemptionLog log = new RedemptionLog();
		log.voucherId = voucherId;
		log.redeemedByRole = ROLE_MERCHANT_STAFF;
		log.redeemedByUserId = redeemedByUserId;
		log.redeemedAt = now;
		return log;
	}
}
