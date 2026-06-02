package com.crewmeister.cmcodingchallenge.currency.exception;

public class BundesbankClientException extends RuntimeException {

    public BundesbankClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public BundesbankClientException(String message) {
        super(message);
    }
}
