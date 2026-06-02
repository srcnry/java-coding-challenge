package com.crewmeister.cmcodingchallenge.exception;

import com.crewmeister.cmcodingchallenge.currency.exception.BundesbankClientException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BundesbankClientException.class)
    public ResponseEntity<Map<String, String>> handleBundesbankClientException(BundesbankClientException ex) {
        // 502 Bad Gateway signals that our service is fine but the upstream Bundesbank API
        // could not be reached or returned something we couldn't make sense of.
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("error", ex.getMessage()));
    }
}
