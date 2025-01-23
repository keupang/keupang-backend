package com.example.keupangproduct.request;

import com.example.keupangproduct.domain.Category;
import org.springframework.web.multipart.MultipartFile;

public record ProductRequest(
    String name, Integer price, Category category, MultipartFile imageUrl
) {

}
