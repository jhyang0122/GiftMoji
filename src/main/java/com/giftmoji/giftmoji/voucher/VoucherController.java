package com.giftmoji.giftmoji.voucher;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vouchers")
public class VoucherController {

	private final VoucherService voucherService;
	private final QrCodeService qrCodeService;

	public VoucherController(VoucherService voucherService, QrCodeService qrCodeService) {
		this.voucherService = voucherService;
		this.qrCodeService = qrCodeService;
	}

	@PostMapping
	public ResponseEntity<VoucherResponse> issue(@RequestBody(required = false) IssueVoucherRequest request) {
		Integer expiryDays = request != null ? request.expiryDays() : null;
		Voucher voucher = voucherService.issueVoucher(expiryDays);
		return ResponseEntity.status(HttpStatus.CREATED).body(VoucherResponse.from(voucher));
	}

	@GetMapping("/{code}")
	public ResponseEntity<VoucherResponse> get(@PathVariable String code) {
		return voucherService.findByCode(code)
				.map(voucher -> ResponseEntity.ok(VoucherResponse.from(voucher)))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}

	@GetMapping(value = "/{code}/qr", produces = MediaType.IMAGE_PNG_VALUE)
	public ResponseEntity<byte[]> qr(@PathVariable String code) {
		return voucherService.findByCode(code)
				.map(voucher -> ResponseEntity.ok(qrCodeService.generatePng(voucher.getCode())))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}

	@PostMapping("/{code}/redeem")
	public ResponseEntity<RedeemResponse> redeem(@PathVariable String code) {
		RedemptionResult result = voucherService.redeem(code);

		if (result instanceof RedemptionResult.Success success) {
			return ResponseEntity.ok(RedeemResponse.success(success.voucher()));
		}
		if (result instanceof RedemptionResult.AlreadyRedeemed alreadyRedeemed) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body(RedeemResponse.alreadyRedeemed(alreadyRedeemed.voucher()));
		}
		if (result instanceof RedemptionResult.Expired expired) {
			return ResponseEntity.status(HttpStatus.GONE).body(RedeemResponse.expired(expired.voucher()));
		}
		return ResponseEntity.notFound().build();
	}
}
