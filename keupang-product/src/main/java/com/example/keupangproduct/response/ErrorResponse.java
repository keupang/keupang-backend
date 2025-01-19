package com.example.keupangproduct.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ErrorResponse {
    private int status;
    private int code;
    private String message;
    private Content content;

    @Getter
    @Builder
    public static class Content{
        private String detail;
        private String help;
    }
}
