package com.example.keupangproduct.request;

import com.example.keupangproduct.domain.Category;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;

public record ProductRequest (
    String name, Integer price, Category category, String image
){}
