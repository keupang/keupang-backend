package com.example.keupangproduct.controller;

import com.example.keupangproduct.domain.Category;
import com.example.keupangproduct.domain.Product;
import com.example.keupangproduct.exception.CustomException;
import com.example.keupangproduct.service.ProductService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
        @RequestBody List<Long> ids
    ){
        List<Product> productsByIds = productService.getProductsByIds(ids);
        return ResponseEntity.ok(productsByIds);
    }

    @Hidden
    @PostMapping("/search")
    public List<Product> getProductBySearch(
        @RequestParam(required = false) String search,
        @RequestParam(required = false) Category category
    ){
        return productService.searchProducts(search, category);
    }

    @Hidden
    @PostMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "상품 등록")
    public ResponseEntity<?> registerProduct(
        @RequestParam String name,
        @RequestParam Category category,
        @RequestPart MultipartFile image
    ){
        try {
            Product savedProduct = productService.createProduct(name, category, image);
            Map<String, Object> content = new HashMap<>();
            content.put("detail", "상품 등록에 성공했습니다.");

            Map<String, Object> data = new HashMap<>();
            data.put("product", savedProduct);

            Map<String, Object> response = new HashMap<>();
            response.put("status", 201);
            response.put("code", 20101);
            response.put("message", "SUCCESS_PRODUCT_REGISTERED");
            response.put("content", content);
            response.put("data", data);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("상품 등록 중 오류 발생: {}", e.getMessage());
            throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, 50001,
                "상품 등록에 실패했습니다.", "상품 정보를 확인하고 다시 시도해주세요.", "PRODUCT_REGISTRATION_FAILED");
        }
    }
}
