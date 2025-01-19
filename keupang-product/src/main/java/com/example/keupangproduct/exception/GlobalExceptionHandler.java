package com.example.keupangproduct.exception;

import com.example.keupanguser.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    //커스텀 예외처리
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException ex) {
        log.error("CustomException: {}", ex.getMessage());

        ErrorResponse response = ErrorResponse.builder()
            .status(ex.getHttpStatus().value())
            .code(ex.getErrorCode())
            .message(ex.getMessage())
            .content(ErrorResponse.Content.builder()
                .detail(ex.getDetail())
                .help(ex.getHelp())
                .build())
            .build();

        return new ResponseEntity<>(response, ex.getHttpStatus());
    }

    //그 외 모든 예외
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex){
        log.error("Exception: {}", ex.getMessage());

        ErrorResponse response = ErrorResponse.builder()
            .status(500)
            .code(0)
            .message("Internal Server Error")
            .content(ErrorResponse.Content.builder()
                .detail(ex.getMessage())
                .help("Please contact support.")
                .build())
            .build();
        return ResponseEntity.status(500).body(response);
    }
}
