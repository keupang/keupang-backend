package com.example.keupangstock.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Getter
@NoArgsConstructor
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long productId;
    private SaleState saleState;
    private Integer price;
    private Integer quantity;

    @JsonIgnore
    @OneToMany(mappedBy = "stock", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StockDetailImage> detailImages = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt; // 최신순 정렬

    private Integer sales = 0;

    @Builder
    public Stock(Long id, Long productId, SaleState saleState, Integer price, Integer quantity,
            List<StockDetailImage> detailImages, Integer sales, LocalDateTime createdAt) {
        this.id = id;
        this.productId = productId;
        this.saleState = saleState;
        this.price = price;
        this.quantity = quantity;
        this.detailImages = detailImages;
        this.sales = sales;
        this.createdAt = createdAt;
    }
}
