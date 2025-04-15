package com.example.keupangreview.response;

import com.example.keupangreview.entity.Review;
import java.time.LocalDateTime;

public record ReviewResponse(Long id, String content, Long productId, int rating,
                             int likes, LocalDateTime createdAt, String userEmail, String userName) {

    public ReviewResponse(Review review) {
        this(review.getId(), review.getContent(), review.getProductId(), review.getRating(),
            review.getLikes(), review.getCreatedAt(), review.getUserEmail() , null);
    }

    public ReviewResponse(Review review, String userName) {
        this(review.getId(), review.getContent(), review.getProductId(), review.getRating(),
            review.getLikes(), review.getCreatedAt(), review.getUserEmail() , userName);
    }
}
