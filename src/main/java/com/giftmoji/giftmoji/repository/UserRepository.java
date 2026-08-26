package com.giftmoji.giftmoji.repository;

import com.giftmoji.giftmoji.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

	Optional<User> findByGoogleId(String googleId);
}
