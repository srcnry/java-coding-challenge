package com.crewmeister.cmcodingchallenge.currency.exception;

import java.time.LocalDate;

public class ExchangeRateNotFoundException extends RuntimeException {

    public ExchangeRateNotFoundException(LocalDate date) {
        super("No exchange rates found for " + date
                + ". The date may be a weekend, a bank holiday, or outside the available range.");
    }

    public ExchangeRateNotFoundException(String currencyCode, LocalDate date) {
        super("No exchange rate found for " + currencyCode + " on " + date
                + ". The date may be a weekend, a bank holiday, or this currency has no ECB series.");
    }
}
