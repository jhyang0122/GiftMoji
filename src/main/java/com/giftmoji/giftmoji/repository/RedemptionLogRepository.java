package com.giftmoji.giftmoji.repository;

import com.giftmoji.giftmoji.entity.RedemptionLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RedemptionLogRepository extends JpaRepository<RedemptionLog, UUID> {

	List<RedemptionLog> findByVoucherId(UUID voucherId);
}
