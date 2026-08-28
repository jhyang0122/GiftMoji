package com.giftmoji.giftmoji.repository;

import com.giftmoji.giftmoji.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ItemRepository extends JpaRepository<Item, UUID> {

	List<Item> findByMerchantIdAndActiveTrueOrderByNameAsc(UUID merchantId);

	Optional<Item> findByIdAndActiveTrue(UUID id);
}
