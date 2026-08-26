package com.giftmoji.giftmoji.repository;

import com.giftmoji.giftmoji.entity.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MerchantRepository extends JpaRepository<Merchant, UUID> {

	List<Merchant> findAllByOrderByNameAsc();
}
