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
@Table(name = "merchant")
@Getter
public class Merchant {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@JdbcTypeCode(SqlTypes.CHAR)
	private UUID id;

	@Nationalized
	@Column(nullable = false)
	private String name;

	@Nationalized
	private String description;

	@Nationalized
	private String logoUrl;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	protected Merchant() {
		// JPA
	}
}
