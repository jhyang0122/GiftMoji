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
// "user" is a reserved word in SQL Server, so name the table explicitly.
@Table(name = "app_user")
@Getter
public class User {

	// The Flyway-managed schema uses CHAR(36) for UUID PKs/FKs (portable
	// across H2 and Azure SQL Server), not each dialect's native UUID type.
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@JdbcTypeCode(SqlTypes.CHAR)
	private UUID id;

	@Column(nullable = false, unique = true)
	private String googleId;

	private String email;

	private String displayName;

	private String pictureUrl;

	private LocalDateTime createdAt;

	private LocalDateTime lastLoginAt;

	protected User() {
		// JPA
	}

	public static User createFromGoogle(String googleId, String email, String displayName, String pictureUrl) {
		User user = new User();
		user.googleId = googleId;
		user.email = email;
		user.displayName = displayName;
		user.pictureUrl = pictureUrl;
		LocalDateTime now = LocalDateTime.now();
		user.createdAt = now;
		user.lastLoginAt = now;
		return user;
	}

	public void recordLogin(String email, String displayName, String pictureUrl) {
		this.email = email;
		this.displayName = displayName;
		this.pictureUrl = pictureUrl;
		this.lastLoginAt = LocalDateTime.now();
	}
}
