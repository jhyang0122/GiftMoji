package com.giftmoji.giftmoji.repository;

import com.giftmoji.giftmoji.entity.Gift;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GiftRepository extends JpaRepository<Gift, UUID> {

	List<Gift> findByReceiverIdOrderBySentAtDesc(UUID receiverId);

	List<Gift> findBySenderIdOrderBySentAtDesc(UUID senderId);

	Optional<Gift> findByVoucherId(UUID voucherId);
}
