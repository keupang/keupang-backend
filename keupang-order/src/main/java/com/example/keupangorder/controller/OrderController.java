package com.example.keupangorder.controller;

import com.example.keupangorder.request.CreateOrderRequest;
import com.example.keupangorder.response.OrderResponse;
import com.example.keupangorder.service.OrderService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/order")
public class OrderController {
    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<?> createOrder(
        @RequestHeader("Authorization") String token,
        @RequestBody CreateOrderRequest request
    ) {
        OrderResponse order = orderService.createOrder(token, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "status", 201,
            "code", 20103,
            "message", "SUCCESS_ORDER_CREATED",
            "content", Map.of("detail", "주문 생성 요청이 접수되었습니다."),
            "data", Map.of("order", order)
        ));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMyOrders(@RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(Map.of(
            "status", 200,
            "code", 20010,
            "message", "SUCCESS_READ_MY_ORDERS",
            "content", Map.of("detail", "내 주문 목록 조회에 성공했습니다."),
            "data", Map.of("orders", orderService.getMyOrders(token))
        ));
    }
}
