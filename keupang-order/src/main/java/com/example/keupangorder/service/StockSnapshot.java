package com.example.keupangorder.service;

public record StockSnapshot(
    Long stockId,
    Long productId,
    String productName,
    Integer price,
    Integer quantity
) {
}
