package com.giftmoji.giftmoji.service;

import com.giftmoji.giftmoji.entity.Voucher;
import com.giftmoji.giftmoji.entity.VoucherStatus;
import com.giftmoji.giftmoji.repository.VoucherRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;

@Service
public class VoucherService {

	private static final Logger log = LoggerFactory.getLogger(VoucherService.class);

	private static final int DEFAULT_EXPIRY_DAYS = 90;
	private static final int CODE_ENTROPY_BYTES = 15;
	private static final int CODE_LOG_PREFIX_LENGTH = 6;

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
		voucher = voucherRepository.save(voucher);
		log.info("Issued voucher {} expiring at {}", maskCode(voucher.getCode()), voucher.getExpiresAt());
		return voucher;
	}

	public Optional<Voucher> findByCode(String code) {
		return voucherRepository.findByCode(code);
	}

	@Transactional
	public RedemptionResult redeem(String code) {
		Optional<Voucher> found = voucherRepository.findByCodeForRedemption(code);
		if (found.isEmpty()) {
			log.warn("Redeem attempted for unknown voucher code {}", maskCode(code));
			return new RedemptionResult.NotFound(code);
		}

		Voucher voucher = found.get();
		Instant now = Instant.now();

		if (voucher.getStatus() == VoucherStatus.REDEEMED) {
			log.info("Voucher {} already redeemed at {}", maskCode(code), voucher.getRedeemedAt());
			return new RedemptionResult.AlreadyRedeemed(voucher);
		}

		if (voucher.getStatus() == VoucherStatus.EXPIRED || voucher.isExpired(now)) {
			voucher.markExpired();
			voucherRepository.save(voucher);
			log.info("Voucher {} is expired (expired at {}), redemption rejected", maskCode(code), voucher.getExpiresAt());
			return new RedemptionResult.Expired(voucher);
		}

		voucher.markRedeemed(now);
		voucherRepository.save(voucher);
		log.info("Voucher {} redeemed", maskCode(code));
		return new RedemptionResult.Success(voucher);
	}

	private String generateCode() {
		byte[] bytes = new byte[CODE_ENTROPY_BYTES];
		secureRandom.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	// Vouchers codes are the redemption secret, so log only a short prefix
	// rather than the full high-entropy token.
	private String maskCode(String code) {
		if (code == null || code.length() <= CODE_LOG_PREFIX_LENGTH) {
			return code;
		}
		return code.substring(0, CODE_LOG_PREFIX_LENGTH) + "...";
	}
}
