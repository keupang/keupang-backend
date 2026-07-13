package com.example.keupangorder.exception;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException ex) {
        return ResponseEntity.status(ex.getStatus())
            .body(new ErrorResponse(
                ex.getStatus().value(),
                ex.getCode(),
                ex.getMessage(),
                ex.getDetail(),
                ex.getHelp()
            ));
    }

    @ExceptionHandler(FeignException.Unauthorized.class)
    public ResponseEntity<ErrorResponse> handleFeignUnauthorized(FeignException.Unauthorized ex) {
        log.warn("Auth service rejected token: {}", ex.getMessage());
        return ResponseEntity.status(401)
            .body(new ErrorResponse(
                401,
                40102,
                "INVALID_TOKEN",
                "Token validation failed.",
                "Provide a valid token."
            ));
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ErrorResponse> handleFeignException(FeignException ex) {
        log.error("Downstream service call failed: {}", ex.getMessage());
        return ResponseEntity.status(502)
            .body(new ErrorResponse(
                502,
                50201,
                "DOWNSTREAM_SERVICE_ERROR",
                "A required internal service did not respond successfully.",
                "Check auth, stock, and Eureka service logs."
            ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex) {
        log.error("Unhandled order service exception", ex);
        return ResponseEntity.status(500)
            .body(new ErrorResponse(
                500,
                50001,
                "INTERNAL_SERVER_ERROR",
                "Order service failed while processing the request.",
                "Check order service logs."
            ));
    }
}
