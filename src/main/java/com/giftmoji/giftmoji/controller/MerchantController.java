package com.giftmoji.giftmoji.controller;

import com.giftmoji.giftmoji.api.ItemResponse;
import com.giftmoji.giftmoji.api.MerchantResponse;
import com.giftmoji.giftmoji.service.CatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/merchants")
public class MerchantController {

	private final CatalogService catalogService;

	public MerchantController(CatalogService catalogService) {
		this.catalogService = catalogService;
	}

	@GetMapping
	public List<MerchantResponse> list() {
		return catalogService.listMerchants().stream().map(MerchantResponse::from).toList();
	}

	@GetMapping("/{merchantId}/items")
	public List<ItemResponse> items(@PathVariable UUID merchantId) {
		return catalogService.listItemsForMerchant(merchantId).stream().map(ItemResponse::from).toList();
	}
}
