package com.example.keupangstock.controller;

import com.example.keupangstock.domain.Stock;
import com.example.keupangstock.domain.product.Category;
import com.example.keupangstock.domain.product.Product;
import com.example.keupangstock.exception.CustomException;
import com.example.keupangstock.response.ReviewResponse;
import com.example.keupangstock.response.StockDetailResponse;
import com.example.keupangstock.response.StockWithProductResponse;
import com.example.keupangstock.service.StockService;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
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
            @RequestPart MultipartFile image,
            @RequestPart MultipartFile[] detailImages,
            @RequestParam Integer quantity,
            @RequestParam(required = false) List<String> keywords) {

        try {
            // 첫 상품이면 상품 등록
            log.info("productId = {}", productId);
            Product product;
            if (productId.isEmpty()) {
                product = stockService.createProduct(image, name, price, category,
                        keywords != null ? keywords : List.of());
            } else {
                product = stockService.getProductById(productId.get());
            }

            // 재고 등록
            Stock stock = stockService.createStoke(product, price, detailImages, quantity);
            // 리뷰는 등록 시점에는 없으므로 빈 리스트 전달
            List<ReviewResponse> emptyReviews = List.of();

            // StockDetailResponse 활용
            StockDetailResponse stockResponse = StockDetailResponse.of(
                    stock,
                    product,
                    stock.getDetailImages(), // 등록 시 반환된 Stock 엔티티에 detailImages 포함됨
                    emptyReviews);
            Map<String, Object> content = new HashMap<>();

            content.put("detail", "재고 등록에 성공했습니다.");

            Map<String, Object> data = new HashMap<>();
            data.put("stock", stockResponse);
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
                    "STOCK_REGISTRATION_FAILED");
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
            @RequestParam(defaultValue = "10") int size) {
        Page<StockWithProductResponse> stockPage = stockService.getStocksWithProductInfo(search, category, minPrice,
                maxPrice, sortBy, page, size);

        Map<String, Object> pagination = Map.of(
                "current_page", page + 1,
                "total_pages", stockPage.getTotalPages(),
                "total_items", stockPage.getTotalElements(),
                "hasNext", stockPage.hasNext());

        Map<String, Object> content = Map.of("detail", "상품 목록 조회에 성공했습니다.");
        Map<String, Object> data = Map.of(
                "stocks", stockPage.getContent(),
                "pagination", pagination);

        Map<String, Object> response = Map.of(
                "status", 200,
                "code", 20008,
                "message", "SUCCESS_READ_PRODUCT",
                "content", content,
                "data", data);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{stockId}")
    public ResponseEntity<?> getStockDetail(@PathVariable Long stockId) {
        StockDetailResponse detail = stockService.getStockDetail(stockId);

        Map<String, Object> content = Map.of("detail", "재고 상세 조회 성공");
        Map<String, Object> data = Map.of("stock", detail);
        Map<String, Object> response = Map.of(
                "status", 200,
                "code", 20009,
                "message", "SUCCESS_READ_STOCK_DETAIL",
                "content", content,
                "data", data);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/admin/reindex")
    public ResponseEntity<Void> reindexAll() {
        stockService.migrateExistingStocksToES();
        return ResponseEntity.ok().build();
    }
}
