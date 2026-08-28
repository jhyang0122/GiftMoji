package com.giftmoji.giftmoji.controller;

import com.giftmoji.giftmoji.api.ItemResponse;
import com.giftmoji.giftmoji.service.CatalogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/items")
public class ItemController {

	private final CatalogService catalogService;

	public ItemController(CatalogService catalogService) {
		this.catalogService = catalogService;
	}

	@GetMapping("/{itemId}")
	public ResponseEntity<ItemResponse> get(@PathVariable UUID itemId) {
		return catalogService.getActiveItem(itemId)
				.map(item -> ResponseEntity.ok(ItemResponse.from(item)))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
}
