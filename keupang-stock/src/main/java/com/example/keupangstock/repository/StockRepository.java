package com.example.keupangstock.repository;

import com.example.keupangstock.domain.Stock;
import java.util.Collection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockRepository extends JpaRepository<Stock, Long> {
    Page<Stock> findByProductIdInAndPriceBetween(Collection<Long> productId, Integer price,
        Integer price2, Pageable pageable);
}
