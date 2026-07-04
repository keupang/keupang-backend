package com.example.keupangorder.repository;

import com.example.keupangorder.domain.OutboxEvent;
import com.example.keupangorder.domain.OutboxStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    List<OutboxEvent> findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus status);
}
