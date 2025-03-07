package com.example.keupangstock.client;

import com.example.keupangproduct.domain.Category;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@FeignClient(name = "product-service", url = "https://api.keupang.store/api/product")
public interface ProductClient {
    @PostMapping(value = "/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<Map<String, Object>> registerProduct(
        @RequestParam("name") String name,
        @RequestParam("category") Category category,
        @RequestPart("image") MultipartFile image
    );
}
