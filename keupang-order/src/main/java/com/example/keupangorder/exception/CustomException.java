package com.example.keupangorder.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class CustomException extends RuntimeException {
    private final HttpStatus status;
    private final int code;
    private final String detail;
    private final String help;

    public CustomException(HttpStatus status, int code, String detail, String help, String message) {
        super(message);
        this.status = status;
        this.code = code;
        this.detail = detail;
        this.help = help;
    }
}
