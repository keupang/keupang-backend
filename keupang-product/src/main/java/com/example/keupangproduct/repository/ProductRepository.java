package com.example.keupangproduct.repository;

import com.example.keupangproduct.domain.Category;
import com.example.keupangproduct.domain.Product;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
    

    List<Product> findByNameContainingIgnoreCaseAndCategory(String search, Category category);

    List<Product> findByNameContainingIgnoreCase(String search);

    List<Product> findByCategory(Category category);
}
