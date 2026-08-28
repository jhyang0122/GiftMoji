package com.giftmoji.giftmoji.service;

import com.giftmoji.giftmoji.entity.RedemptionLog;
import com.giftmoji.giftmoji.repository.RedemptionLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

// Wraps VoucherService's existing, already-tested redeem logic rather than
// modifying it, so RedemptionLog stays purely a redeem-service concern.
@Service
public class RedemptionService {

	private final VoucherService voucherService;
	private final RedemptionLogRepository redemptionLogRepository;

	public RedemptionService(VoucherService voucherService, RedemptionLogRepository redemptionLogRepository) {
		this.voucherService = voucherService;
		this.redemptionLogRepository = redemptionLogRepository;
	}

	@Transactional
	public RedemptionResult redeemAsMerchant(String code, UUID redeemedByUserId) {
		RedemptionResult result = voucherService.redeem(code);
		if (result instanceof RedemptionResult.Success success) {
			RedemptionLog log = RedemptionLog.forMerchantStaffRedemption(
					success.voucher().getId(), redeemedByUserId, LocalDateTime.now());
			redemptionLogRepository.save(log);
		}
		return result;
	}
}
