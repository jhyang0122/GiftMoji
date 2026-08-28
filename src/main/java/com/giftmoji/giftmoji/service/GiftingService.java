package com.giftmoji.giftmoji.service;

import com.giftmoji.giftmoji.entity.Gift;
import com.giftmoji.giftmoji.entity.Item;
import com.giftmoji.giftmoji.entity.User;
import com.giftmoji.giftmoji.entity.Voucher;
import com.giftmoji.giftmoji.entity.VoucherStatus;
import com.giftmoji.giftmoji.repository.GiftRepository;
import com.giftmoji.giftmoji.repository.ItemRepository;
import com.giftmoji.giftmoji.repository.UserRepository;
import com.giftmoji.giftmoji.repository.VoucherRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class GiftingService {

	private static final Logger log = LoggerFactory.getLogger(GiftingService.class);

	// Matches gift.message NVARCHAR(1000) in the schema.
	private static final int MAX_MESSAGE_LENGTH = 1000;

	private final VoucherRepository voucherRepository;
	private final GiftRepository giftRepository;
	private final UserRepository userRepository;
	private final ItemRepository itemRepository;
	private final CatalogService catalogService;
	private final VoucherService voucherService;

	public GiftingService(
			VoucherRepository voucherRepository,
			GiftRepository giftRepository,
			UserRepository userRepository,
			ItemRepository itemRepository,
			CatalogService catalogService,
			VoucherService voucherService) {
		this.voucherRepository = voucherRepository;
		this.giftRepository = giftRepository;
		this.userRepository = userRepository;
		this.itemRepository = itemRepository;
		this.catalogService = catalogService;
		this.voucherService = voucherService;
	}

	public List<Gift> receivedGifts(UUID receiverId) {
		return giftRepository.findByReceiverIdOrderBySentAtDesc(receiverId);
	}

	public List<Gift> sentGifts(UUID senderId) {
		return giftRepository.findBySenderIdOrderBySentAtDesc(senderId);
	}

	@Transactional
	public GiftSendResult sendGift(UUID senderId, UUID itemId, String receiverEmail, String message) {
		if (message != null && message.length() > MAX_MESSAGE_LENGTH) {
			return new GiftSendResult.MessageTooLong();
		}

		Optional<Item> itemOpt = catalogService.getActiveItem(itemId);
		if (itemOpt.isEmpty()) {
			return new GiftSendResult.ItemNotFound();
		}
		Item item = itemOpt.get();

		if (receiverEmail == null || receiverEmail.isBlank()) {
			return new GiftSendResult.ReceiverNotFound();
		}
		Optional<User> receiverOpt = userRepository.findByEmail(receiverEmail.toLowerCase());
		if (receiverOpt.isEmpty()) {
			return new GiftSendResult.ReceiverNotFound();
		}
		User receiver = receiverOpt.get();

		if (receiver.getId().equals(senderId)) {
			return new GiftSendResult.SelfGift();
		}

		// Locked for the duration of the transaction so a concurrent send by
		// the same sender can't both read a stale balance and both succeed.
		User sender = userRepository.findByIdForUpdate(senderId).orElseThrow();
		if (!sender.hasSufficientBalance(item.getPrice())) {
			return new GiftSendResult.InsufficientBalance();
		}
		sender.debit(item.getPrice());
		userRepository.save(sender);

		LocalDateTime expiresAt = LocalDateTime.now().plus(item.getDefaultExpiryDays(), ChronoUnit.DAYS);
		Voucher voucher = Voucher.purchase(voucherService.generateVoucherCode(), item.getId(), sender.getId(), expiresAt);
		voucher.send(receiver.getId());
		voucher = voucherRepository.save(voucher);

		Gift gift = Gift.create(voucher.getId(), sender.getId(), receiver.getId(), message);
		gift = giftRepository.save(gift);

		log.info("Sender {} sent item {} to receiver {}", sender.getId(), item.getId(), receiver.getId());
		return new GiftSendResult.Success(gift);
	}

	// Read-only: safe for GET, so link prefetching/scanning/image proxies
	// can't silently mark a gift viewed and disable sender cancellation.
	@Transactional(readOnly = true)
	public GiftViewResult getGiftDetailForViewer(UUID giftId, UUID viewerId) {
		Optional<Gift> giftOpt = giftRepository.findById(giftId);
		if (giftOpt.isEmpty()) {
			return new GiftViewResult.NotFound();
		}
		Gift gift = giftOpt.get();

		if (!gift.getSenderId().equals(viewerId) && !gift.getReceiverId().equals(viewerId)) {
			return new GiftViewResult.Forbidden();
		}

		Voucher voucher = voucherRepository.findById(gift.getVoucherId()).orElseThrow();
		return new GiftViewResult.Success(gift, voucher);
	}

	// Mutates state — only call from a receiver-initiated action (spec §4.4:
	// once viewed, the sender's cancel/refund is permanently disabled).
	@Transactional
	public GiftViewResult markGiftViewed(UUID giftId, UUID viewerId) {
		Optional<Gift> giftOpt = giftRepository.findById(giftId);
		if (giftOpt.isEmpty()) {
			return new GiftViewResult.NotFound();
		}
		Gift gift = giftOpt.get();

		if (!gift.getSenderId().equals(viewerId) && !gift.getReceiverId().equals(viewerId)) {
			return new GiftViewResult.Forbidden();
		}

		// Locked like cancelGift/redeem so a concurrent cancel or redemption
		// can't be silently overwritten by this transaction's stale read.
		Voucher voucher = voucherRepository.findByIdForUpdate(gift.getVoucherId()).orElseThrow();

		if (viewerId.equals(gift.getReceiverId()) && voucher.getStatus() == VoucherStatus.SENT) {
			voucher.markViewed();
			voucherRepository.save(voucher);
			gift.recordViewed(LocalDateTime.now());
			giftRepository.save(gift);
		}

		return new GiftViewResult.Success(gift, voucher);
	}

	@Transactional
	public GiftCancelResult cancelGift(UUID giftId, UUID requesterId) {
		Optional<Gift> giftOpt = giftRepository.findById(giftId);
		if (giftOpt.isEmpty()) {
			return new GiftCancelResult.NotFound();
		}
		Gift gift = giftOpt.get();

		if (!gift.getSenderId().equals(requesterId)) {
			return new GiftCancelResult.Forbidden();
		}

		Voucher voucher = voucherRepository.findByIdForUpdate(gift.getVoucherId()).orElseThrow();

		// A gift can only be pulled back before the receiver has opened it —
		// once viewed, cancellation is disabled (spec §4.4).
		if (voucher.getStatus() != VoucherStatus.SENT || gift.getViewedAt() != null) {
			return new GiftCancelResult.NotCancellable();
		}

		voucher.cancel();
		voucherRepository.save(voucher);

		Item item = itemRepository.findById(voucher.getItemId()).orElseThrow();
		User sender = userRepository.findByIdForUpdate(requesterId).orElseThrow();
		sender.credit(item.getPrice());
		userRepository.save(sender);

		log.info("Sender {} cancelled gift {} and was refunded {}", requesterId, giftId, item.getPrice());
		return new GiftCancelResult.Success(gift, voucher);
	}
}
