package com.example.keupangorder.request;

import java.util.List;

public record CreateOrderRequest(
    List<CreateOrderItemRequest> items,
    String idempotencyKey
) {
}
