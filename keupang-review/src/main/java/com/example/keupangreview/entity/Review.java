package com.example.keupangreview.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Getter
@NoArgsConstructor
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String content;

    private Long productId;

    @CreationTimestamp
    private LocalDateTime createdAt; //최신순 정렬

    private int rating;
    private int likes;
    private String userEmail;

    @Builder
    public Review(String content, Long productId, int rating, String userEmail){
        this.content = content;
        this.productId = productId;
        this.rating = rating;
        this.likes = 0;
        this.userEmail = userEmail;
    }

    public void update(String content, int rating) {
        this.content = content;
        this.rating = rating;
    }
}
