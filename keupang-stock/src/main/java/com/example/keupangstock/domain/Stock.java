package com.example.keupangstock.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Id;
    private Long productId;
    private SaleState saleState;
    private Integer price;
    private Integer quantity;
    private String detailImage;

    @Builder
    public Stock(Long productId, SaleState saleState, Integer price, Integer quantity,
        String detailImage) {
        this.productId = productId;
        this.saleState = saleState;
        this.price = price;
        this.quantity = quantity;
        this.detailImage = detailImage;
    }
}
