package com.giftmoji.giftmoji.repository;

import com.giftmoji.giftmoji.entity.Voucher;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface VoucherRepository extends JpaRepository<Voucher, UUID> {

	Optional<Voucher> findByCode(String code);

	// Redemption reads-then-writes based on current status, so the row must
	// stay locked for the duration of the transaction: otherwise two
	// concurrent redeem requests can both read a redeemable status and both
	// succeed.
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select v from Voucher v where v.code = :code")
	Optional<Voucher> findByCodeForRedemption(String code);

	// Cancellation reads-then-writes based on current status, mirroring the
	// redemption lock above.
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select v from Voucher v where v.id = :id")
	Optional<Voucher> findByIdForUpdate(UUID id);

	// Backs the expiry sweep: a single bulk UPDATE rather than a per-row
	// loop, cheap enough for Azure App Service's F1 tier. Correctness never
	// depends on sweep cadence — redeem() independently re-checks expiry.
	// clearAutomatically evicts the persistence context so bulk-updated
	// rows aren't masked by stale first-level-cached entities afterward.
	@Modifying(clearAutomatically = true)
	@Query("update Voucher v set v.status = com.giftmoji.giftmoji.entity.VoucherStatus.EXPIRED " +
			"where v.status in (com.giftmoji.giftmoji.entity.VoucherStatus.PURCHASED, " +
			"com.giftmoji.giftmoji.entity.VoucherStatus.SENT, com.giftmoji.giftmoji.entity.VoucherStatus.VIEWED) " +
			"and v.expiresAt < :cutoff")
	int expireOverdue(LocalDateTime cutoff);
}
