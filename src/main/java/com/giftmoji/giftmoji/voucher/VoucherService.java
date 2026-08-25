package com.giftmoji.giftmoji.voucher;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;

@Service
public class VoucherService {

	private static final int DEFAULT_EXPIRY_DAYS = 90;
	private static final int CODE_ENTROPY_BYTES = 15;

	private final VoucherRepository voucherRepository;
	private final SecureRandom secureRandom = new SecureRandom();

	public VoucherService(VoucherRepository voucherRepository) {
		this.voucherRepository = voucherRepository;
	}

	@Transactional
	public Voucher issueVoucher(Integer expiryDays) {
		int days = expiryDays != null ? expiryDays : DEFAULT_EXPIRY_DAYS;
		Instant expiresAt = Instant.now().plus(days, ChronoUnit.DAYS);
		Voucher voucher = Voucher.issue(generateCode(), expiresAt);
		return voucherRepository.save(voucher);
	}

	public Optional<Voucher> findByCode(String code) {
		return voucherRepository.findByCode(code);
	}

	@Transactional
	public RedemptionResult redeem(String code) {
		Optional<Voucher> found = voucherRepository.findByCodeForRedemption(code);
		if (found.isEmpty()) {
			return new RedemptionResult.NotFound(code);
		}

		Voucher voucher = found.get();
		Instant now = Instant.now();

		if (voucher.getStatus() == VoucherStatus.REDEEMED) {
			return new RedemptionResult.AlreadyRedeemed(voucher);
		}

		if (voucher.getStatus() == VoucherStatus.EXPIRED || voucher.isExpired(now)) {
			voucher.markExpired();
			voucherRepository.save(voucher);
			return new RedemptionResult.Expired(voucher);
		}

		voucher.markRedeemed(now);
		voucherRepository.save(voucher);
		return new RedemptionResult.Success(voucher);
	}

	private String generateCode() {
		byte[] bytes = new byte[CODE_ENTROPY_BYTES];
		secureRandom.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}
}
