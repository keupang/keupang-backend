package com.example.keupangstock.client;

import com.example.keupangstock.domain.product.Category;
import com.example.keupangstock.domain.product.Product;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@FeignClient(name = "product-service", url = "https://api.keupang.store/api/product")
public interface ProductClient {
    @PostMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<Product> registerProduct(
        @RequestParam("name") String name,
        @RequestParam("category") Category category,
        @RequestPart("image") MultipartFile image,
        @RequestParam("keywords") List<String> keywords
    );


    @PostMapping("/batch")
    List<Product> getProductInfoList(@RequestBody List<Long> ids);


    @PostMapping("/search")
    List<Product> getProductBySearch(
        @RequestParam(required = false) String search,
        @RequestParam(required = false) Category category
    );

    @GetMapping
    Product getProductById(@RequestParam Long productId);

    @PostMapping("/keywords")
    Map<Long, List<String>> getProductKeywords(@RequestBody List<Long> productIds);
}
