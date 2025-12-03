package com.example.keupangproduct.repository;

import com.example.keupangproduct.domain.Category;
import com.example.keupangproduct.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);

    List<Product> findByNameContainingIgnoreCaseAndCategory(String search, Category category);

    List<Product> findByNameContainingIgnoreCase(String search);

    List<Product> findByCategory(Category category);
}
