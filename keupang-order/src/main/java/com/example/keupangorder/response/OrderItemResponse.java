package com.example.keupangorder.response;

import com.example.keupangorder.domain.OrderItem;

public record OrderItemResponse(
    Long stockId,
    Long productId,
    String productName,
    Integer unitPrice,
    Integer quantity,
    Integer lineTotal
) {
    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
            item.getStockId(),
            item.getProductId(),
            item.getProductName(),
            item.getUnitPrice(),
            item.getQuantity(),
            item.lineTotal()
        );
    }
}
