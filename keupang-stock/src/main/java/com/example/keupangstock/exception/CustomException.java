package com.example.keupangstock.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class CustomException extends RuntimeException {
    private final HttpStatus httpStatus;
    private final int errorCode;
    private final String detail;
    private final String help;

    public CustomException(HttpStatus httpStatus, int errorCode, String detail, String help, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
        this.detail = detail;
        this.help = help;
    }
}
