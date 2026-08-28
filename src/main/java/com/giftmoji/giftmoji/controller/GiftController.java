package com.giftmoji.giftmoji.controller;

import com.giftmoji.giftmoji.api.GiftDetailResponse;
import com.giftmoji.giftmoji.api.GiftSummaryResponse;
import com.giftmoji.giftmoji.api.SendGiftRequest;
import com.giftmoji.giftmoji.entity.Gift;
import com.giftmoji.giftmoji.entity.Item;
import com.giftmoji.giftmoji.entity.User;
import com.giftmoji.giftmoji.entity.Voucher;
import com.giftmoji.giftmoji.repository.ItemRepository;
import com.giftmoji.giftmoji.repository.UserRepository;
import com.giftmoji.giftmoji.repository.VoucherRepository;
import com.giftmoji.giftmoji.service.GiftCancelResult;
import com.giftmoji.giftmoji.service.GiftSendResult;
import com.giftmoji.giftmoji.service.GiftViewResult;
import com.giftmoji.giftmoji.service.GiftingService;
import com.giftmoji.giftmoji.service.QrCodeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/gifts")
public class GiftController {

	private final GiftingService giftingService;
	private final UserRepository userRepository;
	private final VoucherRepository voucherRepository;
	private final ItemRepository itemRepository;
	private final QrCodeService qrCodeService;

	public GiftController(
			GiftingService giftingService,
			UserRepository userRepository,
			VoucherRepository voucherRepository,
			ItemRepository itemRepository,
			QrCodeService qrCodeService) {
		this.giftingService = giftingService;
		this.userRepository = userRepository;
		this.voucherRepository = voucherRepository;
		this.itemRepository = itemRepository;
		this.qrCodeService = qrCodeService;
	}

	@PostMapping
	public ResponseEntity<GiftDetailResponse> send(@AuthenticationPrincipal OidcUser principal, @RequestBody SendGiftRequest request) {
		Optional<User> senderOpt = currentUser(principal);
		if (senderOpt.isEmpty()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		User sender = senderOpt.get();

		GiftSendResult result = giftingService.sendGift(sender.getId(), request.itemId(), request.receiverEmail(), request.message());

		if (result instanceof GiftSendResult.Success success) {
			return ResponseEntity.status(HttpStatus.CREATED).body(toDetail(success.gift(), sender.getId()));
		}
		if (result instanceof GiftSendResult.InsufficientBalance) {
			return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).build();
		}
		if (result instanceof GiftSendResult.SelfGift) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
		}
		if (result instanceof GiftSendResult.MessageTooLong) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
		}
		return ResponseEntity.notFound().build();
	}

	@GetMapping("/received")
	public ResponseEntity<List<GiftSummaryResponse>> received(@AuthenticationPrincipal OidcUser principal) {
		return currentUser(principal)
				.map(user -> ResponseEntity.ok(toSummaries(giftingService.receivedGifts(user.getId()), user.getId())))
				.orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
	}

	@GetMapping("/sent")
	public ResponseEntity<List<GiftSummaryResponse>> sent(@AuthenticationPrincipal OidcUser principal) {
		return currentUser(principal)
				.map(user -> ResponseEntity.ok(toSummaries(giftingService.sentGifts(user.getId()), user.getId())))
				.orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
	}

	@GetMapping("/{giftId}")
	public ResponseEntity<GiftDetailResponse> get(@AuthenticationPrincipal OidcUser principal, @PathVariable UUID giftId) {
		Optional<User> viewerOpt = currentUser(principal);
		if (viewerOpt.isEmpty()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		User viewer = viewerOpt.get();

		GiftViewResult result = giftingService.getGiftDetailForViewer(giftId, viewer.getId());
		if (result instanceof GiftViewResult.Success success) {
			return ResponseEntity.ok(toDetailResponse(success.gift(), success.voucher(), viewer.getId()));
		}
		if (result instanceof GiftViewResult.Forbidden) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
		return ResponseEntity.notFound().build();
	}

	@GetMapping(value = "/{giftId}/qr", produces = MediaType.IMAGE_PNG_VALUE)
	public ResponseEntity<byte[]> qr(@AuthenticationPrincipal OidcUser principal, @PathVariable UUID giftId) {
		Optional<User> viewerOpt = currentUser(principal);
		if (viewerOpt.isEmpty()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		GiftViewResult result = giftingService.getGiftDetailForViewer(giftId, viewerOpt.get().getId());
		if (result instanceof GiftViewResult.Success success) {
			return ResponseEntity.ok(qrCodeService.generatePng(success.voucher().getCode()));
		}
		if (result instanceof GiftViewResult.Forbidden) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
		return ResponseEntity.notFound().build();
	}

	// Receiver-initiated: the only path that marks a gift VIEWED (spec §4.4).
	// The SPA should call this once when the receiver actually opens the
	// gift detail view — not on every GET, which must stay side-effect-free.
	@PostMapping("/{giftId}/view")
	public ResponseEntity<GiftDetailResponse> markViewed(@AuthenticationPrincipal OidcUser principal, @PathVariable UUID giftId) {
		Optional<User> viewerOpt = currentUser(principal);
		if (viewerOpt.isEmpty()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		User viewer = viewerOpt.get();

		GiftViewResult result = giftingService.markGiftViewed(giftId, viewer.getId());
		if (result instanceof GiftViewResult.Success success) {
			return ResponseEntity.ok(toDetailResponse(success.gift(), success.voucher(), viewer.getId()));
		}
		if (result instanceof GiftViewResult.Forbidden) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
		return ResponseEntity.notFound().build();
	}

	@PostMapping("/{giftId}/cancel")
	public ResponseEntity<GiftDetailResponse> cancel(@AuthenticationPrincipal OidcUser principal, @PathVariable UUID giftId) {
		Optional<User> requesterOpt = currentUser(principal);
		if (requesterOpt.isEmpty()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		User requester = requesterOpt.get();

		GiftCancelResult result = giftingService.cancelGift(giftId, requester.getId());
		if (result instanceof GiftCancelResult.Success success) {
			return ResponseEntity.ok(toDetailResponse(success.gift(), success.voucher(), requester.getId()));
		}
		if (result instanceof GiftCancelResult.Forbidden) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
		if (result instanceof GiftCancelResult.NotCancellable) {
			return ResponseEntity.status(HttpStatus.CONFLICT).build();
		}
		return ResponseEntity.notFound().build();
	}

	private Optional<User> currentUser(OidcUser principal) {
		if (principal == null) {
			return Optional.empty();
		}
		return userRepository.findByGoogleId(principal.getSubject());
	}

	private GiftDetailResponse toDetail(Gift gift, UUID viewerId) {
		Voucher voucher = voucherRepository.findById(gift.getVoucherId()).orElseThrow();
		return toDetailResponse(gift, voucher, viewerId);
	}

	private GiftDetailResponse toDetailResponse(Gift gift, Voucher voucher, UUID viewerId) {
		Item item = itemRepository.findById(voucher.getItemId()).orElseThrow();
		User counterparty = userRepository.findById(counterpartyId(gift, viewerId)).orElseThrow();
		return GiftDetailResponse.from(gift, voucher, item, counterparty);
	}

	// Batches the voucher/item/user lookups behind a gift list into a
	// handful of queries instead of one round trip per gift per entity
	// (three N+1s otherwise) — matters on the F1 tier's limited resources.
	private List<GiftSummaryResponse> toSummaries(List<Gift> gifts, UUID viewerId) {
		Map<UUID, Voucher> vouchersById = voucherRepository.findAllById(gifts.stream().map(Gift::getVoucherId).toList())
				.stream().collect(Collectors.toMap(Voucher::getId, Function.identity()));
		Map<UUID, Item> itemsById = itemRepository.findAllById(vouchersById.values().stream().map(Voucher::getItemId).toList())
				.stream().collect(Collectors.toMap(Item::getId, Function.identity()));
		Map<UUID, User> usersById = userRepository.findAllById(gifts.stream().map(g -> counterpartyId(g, viewerId)).toList())
				.stream().collect(Collectors.toMap(User::getId, Function.identity()));

		return gifts.stream()
				.map(gift -> {
					Voucher voucher = vouchersById.get(gift.getVoucherId());
					Item item = itemsById.get(voucher.getItemId());
					User counterparty = usersById.get(counterpartyId(gift, viewerId));
					return GiftSummaryResponse.from(gift, voucher, item, counterparty);
				})
				.toList();
	}

	private UUID counterpartyId(Gift gift, UUID viewerId) {
		return gift.getSenderId().equals(viewerId) ? gift.getReceiverId() : gift.getSenderId();
	}
}
