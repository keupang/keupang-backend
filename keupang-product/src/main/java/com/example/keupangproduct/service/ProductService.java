package com.example.keupangproduct.service;

import com.example.keupangproduct.domain.Product;
import com.example.keupangproduct.repository.ProductRepository;
import com.example.keupangproduct.request.ProductRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {
    private final ProductRepository productRepository;
    public Page<Product> getProducts(String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return productRepository.findByNameContainingIgnoreCase(search, pageable);
    }

    public Product saveProduct(ProductRequest product) {
        Product newProduct = Product.builder()
            .price(product.price())
            .name(product.name())
            .category(product.category())
            .image(product.image())
            .build();
        return productRepository.save(newProduct);
    }

}
