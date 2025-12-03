package com.example.keupangproduct.controller;

import com.example.keupangproduct.domain.Category;
import com.example.keupangproduct.domain.Product;
import com.example.keupangproduct.domain.ProductKeyword;
import com.example.keupangproduct.exception.CustomException;
import com.example.keupangproduct.service.ProductService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/product")
public class ProductController {
    private final ProductService productService;

    @Hidden
    @PostMapping("/batch")
    public ResponseEntity<List<Product>> getProductsByIds(
            @RequestBody List<Long> ids) {
        List<Product> productsByIds = productService.getProductsByIds(ids);
        return ResponseEntity.ok(productsByIds);
    }

    @Hidden
    @PostMapping("/search")
    public List<Product> getProductBySearch(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Category category) {
        return productService.searchProducts(search, category);
    }

    @Hidden
    @PostMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "상품 등록")
    public ResponseEntity<Product> registerProduct(
            @RequestParam String name,
            @RequestParam Integer price,
            @RequestParam(required = false) String description,
            @RequestParam Category category,
            @RequestPart MultipartFile image,
            @RequestParam(required = false) List<String> keywords) {
        try {
            Product savedProduct = productService.createProduct(name, price, description, category, image,
                    keywords);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedProduct);
        } catch (Exception e) {
            log.error("상품 등록 중 오류 발생: {}", e.getMessage());
            throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, 50001,
                    "상품 등록에 실패했습니다.", "상품 정보를 확인하고 다시 시도해주세요.", "PRODUCT_REGISTRATION_FAILED");
        }
    }

    @GetMapping
    public ResponseEntity<?> autocomplete(@RequestParam("keyword") String keyword) {
        List<String> suggestions = productService.autocomplete(keyword);
        Map<String, Object> response = Map.of(
                "status", 200,
                "code", 20010,
                "message", "SUCCESS_AUTOCOMPLETE",
                "data", Map.of("keywords", suggestions));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/keywords")
    public Map<Long, List<String>> getProductKeywords(@RequestBody List<Long> productIds) {
        List<ProductKeyword> keywords = productService.findKeywordsByProductIdIn(productIds);
        return keywords.stream().collect(Collectors.groupingBy(
                pk -> pk.getProduct().getId(),
                Collectors.mapping(ProductKeyword::getKeyword, Collectors.toList())));
    }
}
