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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "item")
@Getter
public class Item {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@JdbcTypeCode(SqlTypes.CHAR)
	private UUID id;

	@Column(nullable = false)
	@JdbcTypeCode(SqlTypes.CHAR)
	private UUID merchantId;

	@Column(nullable = false)
	private String name;

	private String description;

	@Column(nullable = false)
	private BigDecimal price;

	private String imageUrl;

	@Column(nullable = false)
	private boolean active;

	@Column(nullable = false)
	private int defaultExpiryDays;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	protected Item() {
		// JPA
	}
}
