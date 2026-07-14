package com.example.keupangorder.service;

import com.example.keupangorder.client.AuthClient;
import com.example.keupangorder.client.StockClient;
import com.example.keupangorder.config.OrderPolicyProperties;
import com.example.keupangorder.domain.Order;
import com.example.keupangorder.domain.OrderItem;
import com.example.keupangorder.domain.OrderStatus;
import com.example.keupangorder.domain.OutboxEvent;
import com.example.keupangorder.exception.CustomException;
import com.example.keupangorder.repository.OrderRepository;
import com.example.keupangorder.repository.OutboxEventRepository;
import com.example.keupangorder.request.CreateOrderItemRequest;
import com.example.keupangorder.request.CreateOrderRequest;
import com.example.keupangorder.response.OrderResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final AuthClient authClient;
    private final StockClient stockClient;
    private final ObjectMapper objectMapper;
    private final OrderPolicyProperties orderPolicyProperties;

    @Transactional
    public OrderResponse createOrder(String token, CreateOrderRequest request) {
        validateRequest(request);
        String userEmail = resolveUserEmail(token);
        String idempotencyKey = normalizeIdempotencyKey(request.idempotencyKey());
        String requestHash = createRequestHash(request);

        Order existingOrder = orderRepository.findByUserEmailAndIdempotencyKey(userEmail, idempotencyKey).orElse(null);
        if (existingOrder != null) {
            validateIdempotentReplay(existingOrder, requestHash);
            return OrderResponse.from(existingOrder);
        }

        List<OrderItem> orderItems = new ArrayList<>();
        int totalPrice = 0;
        for (CreateOrderItemRequest itemRequest : request.items()) {
            StockSnapshot stock = getStockSnapshot(itemRequest.stockId());
            if (itemRequest.quantity() > stock.quantity()) {
                throw new CustomException(
                    HttpStatus.CONFLICT,
                    40901,
                    "재고 수량이 부족합니다.",
                    "주문 수량을 줄인 뒤 다시 시도해주세요.",
                    "INSUFFICIENT_STOCK"
                );
            }

            OrderItem orderItem = OrderItem.builder()
                .stockId(stock.stockId())
                .productId(stock.productId())
                .productName(stock.productName())
                .unitPrice(stock.price())
                .quantity(itemRequest.quantity())
                .build();
            totalPrice += orderItem.lineTotal();
            orderItems.add(orderItem);
        }

        Order order = Order.builder()
            .orderNumber(createOrderNumber())
            .userEmail(userEmail)
            .idempotencyKey(idempotencyKey)
            .requestHash(requestHash)
            .totalPrice(totalPrice)
            .status(OrderStatus.PENDING)
            .build();
        orderItems.forEach(order::addItem);

        Order savedOrder = orderRepository.save(order);
        outboxEventRepository.save(createOrderCreatedEvent(savedOrder));
        return OrderResponse.from(savedOrder);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(String token) {
        String userEmail = resolveUserEmail(token);
        return orderRepository.findByUserEmailOrderByCreatedAtDesc(userEmail)
            .stream()
            .map(OrderResponse::from)
            .toList();
    }

    private String resolveUserEmail(String token) {
        if (token == null || token.isBlank()) {
            throw new CustomException(
                HttpStatus.UNAUTHORIZED,
                40182,
                "jwt 토큰이 없습니다.",
                "로그인 후 접근해주세요.",
                "EMPTY_ACCESS_TOKEN"
            );
        }

        Map<String, Object> authResponse = authClient.validateToken(token);
        String role = (String) authResponse.get("role");
        String email = (String) authResponse.get("email");
        if (!"USER".equals(role) && !"ADMIN".equals(role)) {
            throw new CustomException(
                HttpStatus.UNAUTHORIZED,
                40181,
                "접근 권한이 없습니다.",
                "유효한 역할이 필요합니다.",
                "FORBIDDEN_ACCESS_TOKEN"
            );
        }
        return email;
    }

    private void validateRequest(CreateOrderRequest request) {
        if (!orderPolicyProperties.createEnabled()) {
            throw new CustomException(
                HttpStatus.SERVICE_UNAVAILABLE,
                50301,
                "현재 주문 생성이 일시 중지되었습니다.",
                "잠시 후 다시 시도해주세요.",
                "ORDER_CREATE_DISABLED"
            );
        }
        if (request == null || request.items() == null || request.items().isEmpty()) {
            throw new CustomException(
                HttpStatus.BAD_REQUEST,
                40001,
                "주문 상품이 없습니다.",
                "하나 이상의 상품을 담아 주문해주세요.",
                "EMPTY_ORDER_ITEMS"
            );
        }
        if (request.items().size() > orderPolicyProperties.maxItemsPerOrder()) {
            throw new CustomException(
                HttpStatus.BAD_REQUEST,
                40004,
                "한 번에 주문할 수 있는 상품 개수를 초과했습니다.",
                "상품 개수를 " + orderPolicyProperties.maxItemsPerOrder() + "개 이하로 줄여주세요.",
                "ORDER_ITEMS_LIMIT_EXCEEDED"
            );
        }
        if (orderPolicyProperties.stockReservationMode() != OrderPolicyProperties.StockReservationMode.SYNC) {
            throw new CustomException(
                HttpStatus.SERVICE_UNAVAILABLE,
                50302,
                "비동기 재고 예약 모드는 아직 사용할 수 없습니다.",
                "stockReservationMode를 SYNC로 설정해주세요.",
                "ASYNC_STOCK_RESERVATION_NOT_READY"
            );
        }
        if (request.idempotencyKey() == null || request.idempotencyKey().isBlank()
            || request.idempotencyKey().length() > 120) {
            throw new CustomException(
                HttpStatus.BAD_REQUEST,
                40003,
                "멱등성 키가 올바르지 않습니다.",
                "idempotencyKey를 1자 이상 120자 이하로 입력해주세요.",
                "INVALID_IDEMPOTENCY_KEY"
            );
        }
        for (CreateOrderItemRequest item : request.items()) {
            if (item.stockId() == null || item.quantity() == null || item.quantity() <= 0) {
                throw new CustomException(
                    HttpStatus.BAD_REQUEST,
                    40002,
                    "주문 상품 정보가 올바르지 않습니다.",
                    "stockId와 1개 이상의 quantity를 입력해주세요.",
                    "INVALID_ORDER_ITEM"
                );
            }
        }
    }

    @SuppressWarnings("unchecked")
    private StockSnapshot getStockSnapshot(Long stockId) {
        Map<String, Object> response = stockClient.getStockDetail(stockId);
        Object dataObj = response.get("data");
        if (!(dataObj instanceof Map<?, ?> data)) {
            throw new CustomException(HttpStatus.NOT_FOUND, 40401, "재고 정보를 찾을 수 없습니다.", "상품 정보를 확인해주세요.", "STOCK_NOT_FOUND");
        }
        Object stockObj = data.get("stock");
        if (!(stockObj instanceof Map<?, ?> stock)) {
            throw new CustomException(HttpStatus.NOT_FOUND, 40401, "재고 정보를 찾을 수 없습니다.", "상품 정보를 확인해주세요.", "STOCK_NOT_FOUND");
        }

        return new StockSnapshot(
            stockId,
            toLong(stock.get("productId")),
            String.valueOf(stock.get("productName")),
            toInteger(stock.get("price")),
            toInteger(stock.get("quantity"))
        );
    }

    private OutboxEvent createOrderCreatedEvent(Order order) {
        String eventId = UUID.randomUUID().toString();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventId", eventId);
        payload.put("eventType", "OrderCreated");
        payload.put("orderId", order.getId());
        payload.put("orderNumber", order.getOrderNumber());
        payload.put("userEmail", order.getUserEmail());
        payload.put("totalPrice", order.getTotalPrice());
        payload.put("idempotencyKey", order.getIdempotencyKey());
        payload.put("occurredAt", LocalDateTime.now().toString());
        payload.put("items", order.getItems().stream()
            .map(item -> Map.of(
                "stockId", item.getStockId(),
                "productId", item.getProductId(),
                "quantity", item.getQuantity(),
                "unitPrice", item.getUnitPrice()
            ))
            .toList());

        return OutboxEvent.builder()
            .eventId(eventId)
            .aggregateType("ORDER")
            .aggregateId(String.valueOf(order.getId()))
            .eventType("OrderCreated")
            .payload(toJson(payload))
            .build();
    }

    private String normalizeIdempotencyKey(String idempotencyKey) {
        return idempotencyKey.trim();
    }

    private String createRequestHash(CreateOrderRequest request) {
        String normalizedItems = request.items().stream()
            .sorted(Comparator.comparing(CreateOrderItemRequest::stockId)
                .thenComparing(CreateOrderItemRequest::quantity))
            .map(item -> item.stockId() + ":" + item.quantity())
            .reduce((left, right) -> left + "|" + right)
            .orElse("");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(normalizedItems.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private void validateIdempotentReplay(Order existingOrder, String requestHash) {
        if (!existingOrder.getRequestHash().equals(requestHash)) {
            throw new CustomException(
                HttpStatus.CONFLICT,
                40902,
                "이미 다른 주문 요청에 사용된 멱등성 키입니다.",
                "새 주문을 만들려면 새로운 idempotencyKey를 사용해주세요.",
                "IDEMPOTENCY_KEY_REUSED"
            );
        }
    }

    private String createOrderNumber() {
        return "ORD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize order event payload", ex);
        }
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private Integer toInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }
}
