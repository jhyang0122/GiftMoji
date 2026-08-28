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

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "gift")
@Getter
public class Gift {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@JdbcTypeCode(SqlTypes.CHAR)
	private UUID id;

	@Column(nullable = false)
	@JdbcTypeCode(SqlTypes.CHAR)
	private UUID voucherId;

	@Column(nullable = false)
	@JdbcTypeCode(SqlTypes.CHAR)
	private UUID senderId;

	@Column(nullable = false)
	@JdbcTypeCode(SqlTypes.CHAR)
	private UUID receiverId;

	@Nationalized
	private String message;

	@Column(nullable = false)
	private LocalDateTime sentAt;

	private LocalDateTime viewedAt;

	protected Gift() {
		// JPA
	}

	public static Gift create(UUID voucherId, UUID senderId, UUID receiverId, String message) {
		Gift gift = new Gift();
		gift.voucherId = voucherId;
		gift.senderId = senderId;
		gift.receiverId = receiverId;
		gift.message = message;
		gift.sentAt = LocalDateTime.now();
		return gift;
	}

	// First-view-only: a receiver reopening an already-viewed gift must not
	// reset viewedAt, since that timestamp also gates cancellation.
	public void recordViewed(LocalDateTime now) {
		if (this.viewedAt == null) {
			this.viewedAt = now;
		}
	}
}
