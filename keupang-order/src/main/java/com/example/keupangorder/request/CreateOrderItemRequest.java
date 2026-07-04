package com.example.keupangorder.request;

public record CreateOrderItemRequest(
    Long stockId,
    Integer quantity
) {
}
