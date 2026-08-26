package com.giftmoji.giftmoji.service;

import com.giftmoji.giftmoji.repository.VoucherRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

// Lazy checks at redemption time (VoucherService.redeem) are the real
// correctness boundary, so this sweep is a list-accuracy nicety, not a
// security control — an interval this coarse is fine, and cheap on
// Azure App Service's F1 tier.
@Service
public class VoucherExpirySweepService {

	private static final Logger log = LoggerFactory.getLogger(VoucherExpirySweepService.class);

	private final VoucherRepository voucherRepository;

	public VoucherExpirySweepService(VoucherRepository voucherRepository) {
		this.voucherRepository = voucherRepository;
	}

	@Scheduled(fixedDelay = 15, timeUnit = TimeUnit.MINUTES)
	@Transactional
	public void sweep() {
		int expired = voucherRepository.expireOverdue(LocalDateTime.now());
		if (expired > 0) {
			log.info("Expired {} overdue vouchers", expired);
		}
	}
}
