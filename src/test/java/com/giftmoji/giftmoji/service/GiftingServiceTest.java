package com.giftmoji.giftmoji.service;

import com.giftmoji.giftmoji.entity.Gift;
import com.giftmoji.giftmoji.entity.User;
import com.giftmoji.giftmoji.entity.Voucher;
import com.giftmoji.giftmoji.entity.VoucherStatus;
import com.giftmoji.giftmoji.repository.GiftRepository;
import com.giftmoji.giftmoji.repository.UserRepository;
import com.giftmoji.giftmoji.repository.VoucherRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class GiftingServiceTest {

	// Seeded via V4__seed_merchants_and_items.sql
	private static final UUID LATTE_ITEM_ID = UUID.fromString("b0000000-0000-0000-0000-000000000001");
	private static final UUID MASSAGE_ITEM_ID = UUID.fromString("b0000000-0000-0000-0000-000000000006");

	@Autowired
	private GiftingService giftingService;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private GiftRepository giftRepository;
	@Autowired
	private VoucherRepository voucherRepository;

	private User createUser(String email) {
		return userRepository.save(User.createFromGoogle(UUID.randomUUID().toString(), email, "Name", null));
	}

	@Test
	void sendGift_debitsSenderAndCreatesSentVoucher() {
		User sender = createUser("sender1@example.com");
		User receiver = createUser("receiver1@example.com");

		GiftSendResult result = giftingService.sendGift(sender.getId(), LATTE_ITEM_ID, receiver.getEmail(), "Enjoy!");

		assertThat(result).isInstanceOf(GiftSendResult.Success.class);
		Gift gift = ((GiftSendResult.Success) result).gift();
		Voucher voucher = voucherRepository.findById(gift.getVoucherId()).orElseThrow();
		assertThat(voucher.getStatus()).isEqualTo(VoucherStatus.SENT);
		assertThat(voucher.getCurrentHolderId()).isEqualTo(receiver.getId());

		User debitedSender = userRepository.findById(sender.getId()).orElseThrow();
		assertThat(debitedSender.getWalletBalance()).isEqualByComparingTo(new BigDecimal("44.50"));

		assertThat(giftingService.receivedGifts(receiver.getId())).extracting(Gift::getId).contains(gift.getId());
	}

	@Test
	void sendGift_insufficientBalance_leavesStateUntouched() {
		User sender = createUser("sender2@example.com");
		User receiver = createUser("receiver2@example.com");

		GiftSendResult result = giftingService.sendGift(sender.getId(), MASSAGE_ITEM_ID, receiver.getEmail(), null);

		assertThat(result).isInstanceOf(GiftSendResult.InsufficientBalance.class);
		User unchangedSender = userRepository.findById(sender.getId()).orElseThrow();
		assertThat(unchangedSender.getWalletBalance()).isEqualByComparingTo("50.00");
		assertThat(giftingService.receivedGifts(receiver.getId())).isEmpty();
	}

	@Test
	void sendGift_rejectsUnknownReceiver() {
		User sender = createUser("sender3@example.com");
		GiftSendResult result = giftingService.sendGift(sender.getId(), LATTE_ITEM_ID, "nobody@example.com", null);
		assertThat(result).isInstanceOf(GiftSendResult.ReceiverNotFound.class);
	}

	@Test
	void sendGift_rejectsSelfGift() {
		User sender = createUser("sender4@example.com");
		GiftSendResult result = giftingService.sendGift(sender.getId(), LATTE_ITEM_ID, sender.getEmail(), null);
		assertThat(result).isInstanceOf(GiftSendResult.SelfGift.class);
	}

	@Test
	void firstView_transitionsSentToViewed_andIsIdempotent() {
		User sender = createUser("sender5@example.com");
		User receiver = createUser("receiver5@example.com");
		Gift gift = ((GiftSendResult.Success) giftingService.sendGift(sender.getId(), LATTE_ITEM_ID, receiver.getEmail(), null)).gift();

		GiftViewResult first = giftingService.getGiftDetailForViewer(gift.getId(), receiver.getId());
		assertThat(first).isInstanceOf(GiftViewResult.Success.class);
		Voucher afterFirstView = ((GiftViewResult.Success) first).voucher();
		assertThat(afterFirstView.getStatus()).isEqualTo(VoucherStatus.VIEWED);
		Gift afterFirstViewGift = ((GiftViewResult.Success) first).gift();
		assertThat(afterFirstViewGift.getViewedAt()).isNotNull();

		GiftViewResult second = giftingService.getGiftDetailForViewer(gift.getId(), receiver.getId());
		Gift afterSecondViewGift = ((GiftViewResult.Success) second).gift();
		assertThat(afterSecondViewGift.getViewedAt()).isEqualTo(afterFirstViewGift.getViewedAt());
	}

	@Test
	void cancelGift_refundsSender_whenUnviewed() {
		User sender = createUser("sender6@example.com");
		User receiver = createUser("receiver6@example.com");
		Gift gift = ((GiftSendResult.Success) giftingService.sendGift(sender.getId(), LATTE_ITEM_ID, receiver.getEmail(), null)).gift();

		GiftCancelResult result = giftingService.cancelGift(gift.getId(), sender.getId());

		assertThat(result).isInstanceOf(GiftCancelResult.Success.class);
		Voucher voucher = ((GiftCancelResult.Success) result).voucher();
		assertThat(voucher.getStatus()).isEqualTo(VoucherStatus.CANCELLED);
		User refundedSender = userRepository.findById(sender.getId()).orElseThrow();
		assertThat(refundedSender.getWalletBalance()).isEqualByComparingTo("50.00");
	}

	@Test
	void cancelGift_rejectedAfterViewed() {
		User sender = createUser("sender7@example.com");
		User receiver = createUser("receiver7@example.com");
		Gift gift = ((GiftSendResult.Success) giftingService.sendGift(sender.getId(), LATTE_ITEM_ID, receiver.getEmail(), null)).gift();
		giftingService.getGiftDetailForViewer(gift.getId(), receiver.getId());

		GiftCancelResult result = giftingService.cancelGift(gift.getId(), sender.getId());
		assertThat(result).isInstanceOf(GiftCancelResult.NotCancellable.class);
	}

	@Test
	void cancelGift_rejectedForNonSender() {
		User sender = createUser("sender8@example.com");
		User receiver = createUser("receiver8@example.com");
		User stranger = createUser("stranger8@example.com");
		Gift gift = ((GiftSendResult.Success) giftingService.sendGift(sender.getId(), LATTE_ITEM_ID, receiver.getEmail(), null)).gift();

		GiftCancelResult result = giftingService.cancelGift(gift.getId(), stranger.getId());
		assertThat(result).isInstanceOf(GiftCancelResult.Forbidden.class);
	}
}
