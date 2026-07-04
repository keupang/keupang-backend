package com.example.keupangorder.exception;

public record ErrorResponse(
    int status,
    int code,
    String message,
    String detail,
    String help
) {
}
