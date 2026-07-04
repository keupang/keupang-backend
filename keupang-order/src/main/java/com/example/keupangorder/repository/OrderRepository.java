package com.example.keupangorder.repository;

import com.example.keupangorder.domain.Order;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderNumber(String orderNumber);

    List<Order> findByUserEmailOrderByCreatedAtDesc(String userEmail);
}
