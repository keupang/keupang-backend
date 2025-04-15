package com.example.keupangreview.service;

import com.example.keupangreview.client.AuthClient;
import com.example.keupangreview.client.UserClient;
import com.example.keupangreview.entity.Review;
import com.example.keupangreview.exception.CustomException;
import com.example.keupangreview.repository.ReviewRepository;
import com.example.keupangreview.request.ReviewRequest;
import com.example.keupangreview.response.ReviewResponse;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final AuthClient authClient;
    private final UserClient userClient;

    public Review createReview(String token, Long productId, ReviewRequest request) {
        Map<String, Object> authResponse = authClient.validateToken(token);
        String role = (String) authResponse.get("role"); // role 가져오기
        String email = (String) authResponse.get("email"); // email 가져오기

        if (!"USER".equals(role) && !"ADMIN".equals(role)) { // 허용된 역할만 접근 가능
            throw new CustomException(
                HttpStatus.UNAUTHORIZED,
                40181,
                "접근 권한이 없습니다.",
                "유효한 역할이 필요합니다.",
                "FORBIDDEN_ACCESS_TOKEN"
            );
        }
        Review review = Review.builder()
            .content(request.content())
            .rating(request.rating())
            .productId(productId)
            .userEmail(email)
            .build();
        return reviewRepository.save(review);
    }

    public List<ReviewResponse> getReviewsByProductId(Long productId) {
        return reviewRepository.findByProductId(productId).stream()
            .map(review -> {
                String userName = userClient.getName(review.getUserEmail());
                return new ReviewResponse(review, userName);
            })
            .collect(Collectors.toList());
    }

    public void deleteReview(Long reviewId) {
        reviewRepository.deleteById(reviewId);
    }

}
