package com.example.keupangreview.controller;

import com.example.keupangreview.entity.Review;
import com.example.keupangreview.request.ReviewRequest;
import com.example.keupangreview.response.ReviewResponse;
import com.example.keupangreview.service.ReviewService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/review")
@Slf4j
public class ReviewController {
    private final ReviewService reviewService;

    @PostMapping("/{productId}")
    public ResponseEntity<?> createReview(
        @RequestHeader("Authorization") String token,
        @PathVariable Long productId,
        @RequestBody ReviewRequest request
    ) {
        Review review = reviewService.createReview(token, productId, request);

        Map<String, Object> response = new HashMap<>();
        response.put("status", 201);
        response.put("code", 20101);
        response.put("message", "SUCCESS_REVIEW_REGISTERED");
        response.put("content", Map.of("detail", "리뷰 등록에 성공했습니다."));
        response.put("data", Map.of("review", new ReviewResponse(review)));

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Hidden
    @GetMapping("/{productId}")
    public List<ReviewResponse> getReviewsByProduct(@PathVariable Long productId) {
        return reviewService.getReviewsByProductId(productId);
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<?> deleteReview(@PathVariable Long reviewId) {
        reviewService.deleteReview(reviewId);

        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("code", 20002);
        response.put("message", "SUCCESS_REVIEW_DELETED");
        response.put("content", Map.of("detail", "리뷰 삭제 성공"));
        response.put("data", null);

        return ResponseEntity.ok(response);
    }
}
