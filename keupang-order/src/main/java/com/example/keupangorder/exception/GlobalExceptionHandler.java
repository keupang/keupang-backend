package com.example.keupangorder.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
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
}
