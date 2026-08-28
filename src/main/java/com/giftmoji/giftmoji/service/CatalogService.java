package com.giftmoji.giftmoji.service;

import com.giftmoji.giftmoji.entity.Item;
import com.giftmoji.giftmoji.entity.Merchant;
import com.giftmoji.giftmoji.repository.ItemRepository;
import com.giftmoji.giftmoji.repository.MerchantRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CatalogService {

	private final MerchantRepository merchantRepository;
	private final ItemRepository itemRepository;

	public CatalogService(MerchantRepository merchantRepository, ItemRepository itemRepository) {
		this.merchantRepository = merchantRepository;
		this.itemRepository = itemRepository;
	}

	public List<Merchant> listMerchants() {
		return merchantRepository.findAllByOrderByNameAsc();
	}

	public List<Item> listItemsForMerchant(UUID merchantId) {
		return itemRepository.findByMerchantIdAndActiveTrueOrderByNameAsc(merchantId);
	}

	public Optional<Item> getActiveItem(UUID itemId) {
		return itemRepository.findByIdAndActiveTrue(itemId);
	}
}
