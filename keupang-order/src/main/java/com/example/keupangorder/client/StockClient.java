package com.example.keupangorder.client;

import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "stock", path = "/api/stock")
public interface StockClient {
    @GetMapping("/{stockId}")
    Map<String, Object> getStockDetail(@PathVariable("stockId") Long stockId);
}
