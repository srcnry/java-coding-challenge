package com.crewmeister.cmcodingchallenge.exception;

import com.crewmeister.cmcodingchallenge.currency.exception.BundesbankClientException;
import com.crewmeister.cmcodingchallenge.currency.exception.ExchangeRateNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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

    @ExceptionHandler(ExchangeRateNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleExchangeRateNotFound(ExchangeRateNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        // Fires when a path variable cannot be converted to its target type —
        // e.g. "not-a-date" where yyyy-MM-dd is expected.
        String message = String.format("Invalid value '%s' for parameter '%s'. Expected format: yyyy-MM-dd",
                ex.getValue(), ex.getName());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", message));
    }
}
