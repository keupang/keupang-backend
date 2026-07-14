package com.example.keupangorder.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "keupang.order")
public record OrderPolicyProperties(
    boolean createEnabled,
    int maxItemsPerOrder,
    StockReservationMode stockReservationMode
) {

    public OrderPolicyProperties {
        if (maxItemsPerOrder <= 0) {
            maxItemsPerOrder = 10;
        }
        if (stockReservationMode == null) {
            stockReservationMode = StockReservationMode.SYNC;
        }
    }

    public OrderPolicyProperties() {
        this(true, 10, StockReservationMode.SYNC);
    }

    public enum StockReservationMode {
        SYNC,
        ASYNC
    }
}
