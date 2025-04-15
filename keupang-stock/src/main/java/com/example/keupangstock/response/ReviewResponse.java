package com.example.keupangstock.response;

import java.time.LocalDateTime;

public record ReviewResponse(Long id, String content, Long productId, int rating,
                             int likes, LocalDateTime createdAt, String userEmail, String userName){}
