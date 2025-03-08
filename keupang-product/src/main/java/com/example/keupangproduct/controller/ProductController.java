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
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
    @GetMapping()
    @Operation(summary = "상품 목록 조회")
    public ResponseEntity<?> getProducts(
        @RequestParam(defaultValue = "0") Integer page,
        @RequestParam(defaultValue = "10") Integer size,
        @RequestParam String search
    ) {
        if(search == null || search.isEmpty()){
            throw new CustomException(
                HttpStatus.BAD_REQUEST,
                40001,
                "검색어가 비어 있습니다.", //detail
                "검색어를 입력한 후 다시 시도해주세요.", //help
                "EMPTY_SEARCH_KEYWORD" //message
            );
        }
        Page<Product> productPage = productService.getProducts(search, page, size);
        List<Product> products = productPage.getContent();

        Map<String, Object> pagination = new HashMap<>();
        pagination.put("current_page", page + 1);
        pagination.put("total_pages", productPage.getTotalPages());
        pagination.put("total_items", productPage.getTotalElements());
        pagination.put("hasNext", productPage.hasNext());

        Map<String, Object> content = new HashMap<>();
        content.put("detail", "상품 목록 조회에 성공했습니다.");

        Map<String, Object> data = new HashMap<>();
        data.put("products", products);
        data.put("pagination", pagination);

        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("code", 20008);
        response.put("message", "SUCCESS_READ_PRODUCT");
        response.put("content", content);
        response.put("data", data);

        return ResponseEntity.ok(response);
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
