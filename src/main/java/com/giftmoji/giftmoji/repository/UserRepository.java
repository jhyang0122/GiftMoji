package com.giftmoji.giftmoji.repository;

import com.giftmoji.giftmoji.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

	Optional<User> findByGoogleId(String googleId);

	Optional<User> findByEmail(String email);

	// Wallet debit/credit during send/cancel reads-then-writes based on
	// current balance, so the row must stay locked for the transaction
	// (mirrors VoucherRepository.findByCodeForRedemption).
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select u from User u where u.id = :id")
	Optional<User> findByIdForUpdate(UUID id);
}
