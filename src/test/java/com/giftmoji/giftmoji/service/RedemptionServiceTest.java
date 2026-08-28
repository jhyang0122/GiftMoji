package com.giftmoji.giftmoji.service;

import com.giftmoji.giftmoji.entity.RedemptionLog;
import com.giftmoji.giftmoji.entity.User;
import com.giftmoji.giftmoji.entity.Voucher;
import com.giftmoji.giftmoji.repository.RedemptionLogRepository;
import com.giftmoji.giftmoji.repository.UserRepository;
import com.giftmoji.giftmoji.repository.VoucherRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RedemptionServiceTest {

	// Seeded via V4__seed_merchants_and_items.sql
	private static final UUID LATTE_ITEM_ID = UUID.fromString("b0000000-0000-0000-0000-000000000001");

	@Autowired
	private RedemptionService redemptionService;
	@Autowired
	private RedemptionLogRepository redemptionLogRepository;
	@Autowired
	private VoucherRepository voucherRepository;
	@Autowired
	private UserRepository userRepository;

	private User createUser(String email) {
		return userRepository.save(User.createFromGoogle(UUID.randomUUID().toString(), email, "Name", null));
	}

	private Voucher createPurchasedVoucher(String code, User owner) {
		Voucher voucher = Voucher.purchase(code, LATTE_ITEM_ID, owner.getId(), LocalDateTime.now().plusDays(1));
		return voucherRepository.save(voucher);
	}

	@Test
	@Transactional
	void redeemAsMerchant_success_writesExactlyOneRedemptionLog() {
		User owner = createUser("owner1@example.com");
		User staff = createUser("staff1@example.com");
		Voucher voucher = createPurchasedVoucher("redeem-code-1", owner);

		RedemptionResult result = redemptionService.redeemAsMerchant(voucher.getCode(), staff.getId());

		assertThat(result).isInstanceOf(RedemptionResult.Success.class);
		List<RedemptionLog> logs = redemptionLogRepository.findByVoucherId(voucher.getId());
		assertThat(logs).hasSize(1);
		assertThat(logs.get(0).getRedeemedByUserId()).isEqualTo(staff.getId());
	}

	@Test
	@Transactional
	void redeemAsMerchant_alreadyRedeemed_doesNotDuplicateLog() {
		User owner = createUser("owner2@example.com");
		User staff = createUser("staff2@example.com");
		Voucher voucher = createPurchasedVoucher("redeem-code-2", owner);

		redemptionService.redeemAsMerchant(voucher.getCode(), staff.getId());
		RedemptionResult second = redemptionService.redeemAsMerchant(voucher.getCode(), staff.getId());

		assertThat(second).isInstanceOf(RedemptionResult.AlreadyRedeemed.class);
		assertThat(redemptionLogRepository.findByVoucherId(voucher.getId())).hasSize(1);
	}

	@Test
	void concurrentRedeem_onlyOneSucceeds() throws InterruptedException {
		User owner = createUser("owner3@example.com");
		User staff = createUser("staff3@example.com");
		Voucher voucher = createPurchasedVoucher("redeem-code-concurrent", owner);

		int attempts = 10;
		ExecutorService pool = Executors.newFixedThreadPool(attempts);
		CountDownLatch ready = new CountDownLatch(attempts);
		CountDownLatch start = new CountDownLatch(1);
		AtomicInteger successCount = new AtomicInteger();
		AtomicInteger alreadyRedeemedCount = new AtomicInteger();

		try {
			for (int i = 0; i < attempts; i++) {
				pool.submit(() -> {
					ready.countDown();
					try {
						start.await();
						RedemptionResult result = redemptionService.redeemAsMerchant(voucher.getCode(), staff.getId());
						if (result instanceof RedemptionResult.Success) {
							successCount.incrementAndGet();
						} else if (result instanceof RedemptionResult.AlreadyRedeemed) {
							alreadyRedeemedCount.incrementAndGet();
						}
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				});
			}
			ready.await(5, TimeUnit.SECONDS);
			start.countDown();
			pool.shutdown();
			assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();
		} finally {
			pool.shutdownNow();
		}

		assertThat(successCount.get()).isEqualTo(1);
		assertThat(alreadyRedeemedCount.get()).isEqualTo(attempts - 1);
		assertThat(redemptionLogRepository.findByVoucherId(voucher.getId())).hasSize(1);
	}
}
