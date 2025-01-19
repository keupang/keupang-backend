package com.example.keupangproduct.domain;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Id;

    private String name;
    private Integer price;
    private Category category;

    @Builder
    public Product(String name, Integer price, Category category){
        this.name = name;
        this.price = price;
        this.category = category;
    }
}
