package com.example.keupangstock.controller;

import com.example.keupangproduct.domain.Category;
import com.example.keupangproduct.exception.CustomException;
import com.example.keupangstock.client.ProductClient;
import com.example.keupangstock.domain.Stock;
import com.example.keupangstock.response.StockWithProductResponse;
import com.example.keupangstock.service.StockService;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stock")
@Slf4j
public class StockController {
    private final StockService stockService;

    @PostMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> registerStock(
        @RequestParam Optional<Long> productId,
        @RequestParam Integer price,
        @RequestParam String name,
        @RequestParam Category category,
        @RequestParam MultipartFile image,
        @RequestParam MultipartFile detailImage,
        @RequestParam Integer quantity
    ){

        try {
            //첫 상품이면 상품 등록
            log.info("productId = {}", productId);
            Long finalProductId = productId.orElseGet(() -> stockService.createProduct(image, name, category));
            log.info("finalProductId = {}", finalProductId);
            //재고 등록
            Stock stock = stockService.createStoke(finalProductId, price, detailImage, quantity);

            Map<String, Object> content = new HashMap<>();

            content.put("detail", "재고 등록에 성공했습니다.");

            Map<String, Object> data = new HashMap<>();
            data.put("stock", stock);
            Map<String, Object> response = new HashMap<>();
            response.put("status", 201);
            response.put("code", 20102);
            response.put("message", "SUCCESS_STOCK_REGISTERED");
            response.put("content", content);
            response.put("data", data);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception ex) {
            log.error("재고 등록 중 오류 발생: {}", ex.getMessage());
            throw new CustomException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                50002,
                "재고 등록에 실패했습니다.",
                "재고 정보를 확인하고 다시 시도해주세요.",
                "STOCK_REGISTRATION_FAILED"
            );
        }

    }

    @GetMapping
    public ResponseEntity<?> getStocks(
        @RequestParam(required = false) String search,
        @RequestParam(required = false) Category category,
        @RequestParam(required = false) Integer minPrice,
        @RequestParam(required = false) Integer maxPrice,
        @RequestParam(defaultValue = "name") String sortBy,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size){
        Page<StockWithProductResponse> stockPage = stockService.getStocksWithProductInfo(search, category, minPrice, maxPrice, sortBy, page, size);

        Map<String, Object> pagination = Map.of(
            "current_page", page + 1,
            "total_pages", stockPage.getTotalPages(),
            "total_items", stockPage.getTotalElements(),
            "hasNext", stockPage.hasNext()
        );

        Map<String, Object> content = Map.of("detail", "상품 목록 조회에 성공했습니다.");
        Map<String, Object> data = Map.of(
            "stocks", stockPage.getContent(),
            "pagination", pagination
        );

        Map<String, Object> response = Map.of(
            "status", 200,
            "code", 20008,
            "message", "SUCCESS_READ_PRODUCT",
            "content", content,
            "data", data
        );

        return ResponseEntity.ok(response);
    }
}
