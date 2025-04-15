package com.example.keupangstock.client;

import com.example.keupangstock.response.ReviewResponse;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "review-service", url = "https://api.keupang.store/api/review")
public interface ReviewClient {
    @GetMapping("/{productId}")
    List<ReviewResponse> getReviewsByProductId(@PathVariable("productId") Long productId);
}
