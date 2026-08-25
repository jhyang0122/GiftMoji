package com.giftmoji.giftmoji.repository;

import com.giftmoji.giftmoji.entity.Voucher;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface VoucherRepository extends JpaRepository<Voucher, UUID> {

	Optional<Voucher> findByCode(String code);

	// Redemption reads-then-writes based on current status, so the row must
	// stay locked for the duration of the transaction: otherwise two
	// concurrent redeem requests can both read ISSUED and both succeed.
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select v from Voucher v where v.code = :code")
	Optional<Voucher> findByCodeForRedemption(String code);
}
