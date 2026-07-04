package com.example.keupangorder.response;

import com.example.keupangorder.domain.Order;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
    Long id,
    String orderNumber,
    String userEmail,
    Integer totalPrice,
    String status,
    List<OrderItemResponse> items,
    LocalDateTime createdAt
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
            order.getId(),
            order.getOrderNumber(),
            order.getUserEmail(),
            order.getTotalPrice(),
            order.getStatus().name(),
            order.getItems().stream().map(OrderItemResponse::from).toList(),
            order.getCreatedAt()
        );
    }
}
