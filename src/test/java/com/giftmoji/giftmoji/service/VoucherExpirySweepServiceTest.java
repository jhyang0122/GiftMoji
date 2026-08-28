package com.giftmoji.giftmoji.service;

import com.giftmoji.giftmoji.entity.User;
import com.giftmoji.giftmoji.entity.Voucher;
import com.giftmoji.giftmoji.entity.VoucherStatus;
import com.giftmoji.giftmoji.repository.UserRepository;
import com.giftmoji.giftmoji.repository.VoucherRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class VoucherExpirySweepServiceTest {

	private static final UUID LATTE_ITEM_ID = UUID.fromString("b0000000-0000-0000-0000-000000000001");

	@Autowired
	private VoucherExpirySweepService sweepService;
	@Autowired
	private VoucherRepository voucherRepository;
	@Autowired
	private UserRepository userRepository;

	@Test
	void sweep_expiresOverdueSentVouchers() {
		User owner = userRepository.save(User.createFromGoogle(UUID.randomUUID().toString(), "owner@example.com", "Name", null));
		Voucher voucher = Voucher.purchase("sweep-code", LATTE_ITEM_ID, owner.getId(), LocalDateTime.now().minusDays(1));
		voucher.send(owner.getId());
		voucher = voucherRepository.save(voucher);

		sweepService.sweep();

		Voucher reloaded = voucherRepository.findById(voucher.getId()).orElseThrow();
		assertThat(reloaded.getStatus()).isEqualTo(VoucherStatus.EXPIRED);
	}

	@Test
	void sweep_leavesUnexpiredVouchersAlone() {
		User owner = userRepository.save(User.createFromGoogle(UUID.randomUUID().toString(), "owner2@example.com", "Name", null));
		Voucher voucher = Voucher.purchase("sweep-code-2", LATTE_ITEM_ID, owner.getId(), LocalDateTime.now().plusDays(1));
		voucher.send(owner.getId());
		voucher = voucherRepository.save(voucher);

		sweepService.sweep();

		Voucher reloaded = voucherRepository.findById(voucher.getId()).orElseThrow();
		assertThat(reloaded.getStatus()).isEqualTo(VoucherStatus.SENT);
	}
}
