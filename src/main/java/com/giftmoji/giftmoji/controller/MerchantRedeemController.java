package com.giftmoji.giftmoji.controller;

import com.giftmoji.giftmoji.api.RedeemRequest;
import com.giftmoji.giftmoji.api.RedeemResponse;
import com.giftmoji.giftmoji.entity.Item;
import com.giftmoji.giftmoji.entity.User;
import com.giftmoji.giftmoji.entity.Voucher;
import com.giftmoji.giftmoji.repository.ItemRepository;
import com.giftmoji.giftmoji.repository.UserRepository;
import com.giftmoji.giftmoji.service.RedemptionResult;
import com.giftmoji.giftmoji.service.RedemptionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Only reachable by an authenticated user with ROLE_MERCHANT
// (see SecurityConfig: /api/merchant/** requires that role) — this is the
// spec's "someone other than the receiver validates the code" flow.
@RestController
@RequestMapping("/api/merchant")
public class MerchantRedeemController {

	private final RedemptionService redemptionService;
	private final UserRepository userRepository;
	private final ItemRepository itemRepository;

	public MerchantRedeemController(RedemptionService redemptionService, UserRepository userRepository, ItemRepository itemRepository) {
		this.redemptionService = redemptionService;
		this.userRepository = userRepository;
		this.itemRepository = itemRepository;
	}

	@PostMapping("/redeem")
	public ResponseEntity<RedeemResponse> redeem(@AuthenticationPrincipal OidcUser principal, @RequestBody RedeemRequest request) {
		User staff = userRepository.findByGoogleId(principal.getSubject()).orElseThrow();
		RedemptionResult result = redemptionService.redeemAsMerchant(request.code(), staff.getId());

		if (result instanceof RedemptionResult.Success success) {
			return ResponseEntity.ok(RedeemResponse.success(success.voucher(), itemName(success.voucher())));
		}
		if (result instanceof RedemptionResult.AlreadyRedeemed already) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body(RedeemResponse.alreadyRedeemed(already.voucher(), itemName(already.voucher())));
		}
		if (result instanceof RedemptionResult.Cancelled cancelled) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body(RedeemResponse.cancelled(cancelled.voucher(), itemName(cancelled.voucher())));
		}
		if (result instanceof RedemptionResult.Expired expired) {
			return ResponseEntity.status(HttpStatus.GONE).body(RedeemResponse.expired(expired.voucher(), itemName(expired.voucher())));
		}
		return ResponseEntity.notFound().build();
	}

	private String itemName(Voucher voucher) {
		return itemRepository.findById(voucher.getItemId()).map(Item::getName).orElse(null);
	}
}
