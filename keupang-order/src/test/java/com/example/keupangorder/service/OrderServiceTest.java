package com.example.keupangorder.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.keupangorder.client.AuthClient;
import com.example.keupangorder.client.StockClient;
import com.example.keupangorder.config.OrderPolicyProperties;
import com.example.keupangorder.domain.Order;
import com.example.keupangorder.domain.OrderStatus;
import com.example.keupangorder.exception.CustomException;
import com.example.keupangorder.repository.OrderRepository;
import com.example.keupangorder.repository.OutboxEventRepository;
import com.example.keupangorder.request.CreateOrderItemRequest;
import com.example.keupangorder.request.CreateOrderRequest;
import com.example.keupangorder.response.OrderResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private AuthClient authClient;

    @Mock
    private StockClient stockClient;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(
            orderRepository,
            outboxEventRepository,
            authClient,
            stockClient,
            new ObjectMapper(),
            new OrderPolicyProperties()
        );
    }

    @Test
    void createOrderReturnsExistingOrderWhenIdempotencyKeyIsReplayedWithSameRequest() {
        String token = "Bearer token";
        String userEmail = "user@keupang.local";
        String idempotencyKey = "order-001";
        CreateOrderRequest request = new CreateOrderRequest(
            List.of(new CreateOrderItemRequest(1L, 1)),
            idempotencyKey
        );
        Order existingOrder = Order.builder()
            .orderNumber("ORD-EXISTING")
            .userEmail(userEmail)
            .idempotencyKey(idempotencyKey)
            .requestHash(sha256("1:1"))
            .totalPrice(1000)
            .status(OrderStatus.PENDING)
            .build();

        when(authClient.validateToken(token)).thenReturn(Map.of("role", "USER", "email", userEmail));
        when(orderRepository.findByUserEmailAndIdempotencyKey(userEmail, idempotencyKey))
            .thenReturn(Optional.of(existingOrder));

        OrderResponse response = orderService.createOrder(token, request);

        assertThat(response.orderNumber()).isEqualTo("ORD-EXISTING");
        verify(stockClient, never()).getStockDetail(any());
        verify(orderRepository, never()).save(any());
        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    void createOrderRejectsIdempotencyKeyReusedWithDifferentRequest() {
        String token = "Bearer token";
        String userEmail = "user@keupang.local";
        String idempotencyKey = "order-001";
        CreateOrderRequest request = new CreateOrderRequest(
            List.of(new CreateOrderItemRequest(1L, 2)),
            idempotencyKey
        );
        Order existingOrder = Order.builder()
            .orderNumber("ORD-EXISTING")
            .userEmail(userEmail)
            .idempotencyKey(idempotencyKey)
            .requestHash(sha256("1:1"))
            .totalPrice(1000)
            .status(OrderStatus.PENDING)
            .build();

        when(authClient.validateToken(token)).thenReturn(Map.of("role", "USER", "email", userEmail));
        when(orderRepository.findByUserEmailAndIdempotencyKey(userEmail, idempotencyKey))
            .thenReturn(Optional.of(existingOrder));

        assertThatThrownBy(() -> orderService.createOrder(token, request))
            .isInstanceOf(CustomException.class)
            .extracting("status")
            .isEqualTo(HttpStatus.CONFLICT);
        verify(stockClient, never()).getStockDetail(any());
        verify(orderRepository, never()).save(any());
        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    void createOrderRejectsWhenOrderCreationIsDisabledByPolicy() {
        OrderService disabledOrderService = new OrderService(
            orderRepository,
            outboxEventRepository,
            authClient,
            stockClient,
            new ObjectMapper(),
            new OrderPolicyProperties(false, 10, OrderPolicyProperties.StockReservationMode.SYNC)
        );
        CreateOrderRequest request = new CreateOrderRequest(
            List.of(new CreateOrderItemRequest(1L, 1)),
            "order-disabled"
        );

        assertThatThrownBy(() -> disabledOrderService.createOrder("Bearer token", request))
            .isInstanceOf(CustomException.class)
            .extracting("status")
            .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        verify(authClient, never()).validateToken(any());
        verify(stockClient, never()).getStockDetail(any());
        verify(orderRepository, never()).save(any());
        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    void createOrderRejectsWhenItemCountExceedsPolicyLimit() {
        OrderService limitedOrderService = new OrderService(
            orderRepository,
            outboxEventRepository,
            authClient,
            stockClient,
            new ObjectMapper(),
            new OrderPolicyProperties(true, 1, OrderPolicyProperties.StockReservationMode.SYNC)
        );
        CreateOrderRequest request = new CreateOrderRequest(
            List.of(
                new CreateOrderItemRequest(1L, 1),
                new CreateOrderItemRequest(2L, 1)
            ),
            "order-too-many-items"
        );

        assertThatThrownBy(() -> limitedOrderService.createOrder("Bearer token", request))
            .isInstanceOf(CustomException.class)
            .extracting("status")
            .isEqualTo(HttpStatus.BAD_REQUEST);
        verify(authClient, never()).validateToken(any());
        verify(stockClient, never()).getStockDetail(any());
        verify(orderRepository, never()).save(any());
        verify(outboxEventRepository, never()).save(any());
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
