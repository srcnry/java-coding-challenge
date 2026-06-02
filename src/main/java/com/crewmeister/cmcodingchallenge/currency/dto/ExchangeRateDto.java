package com.crewmeister.cmcodingchallenge.currency.dto;

import com.crewmeister.cmcodingchallenge.currency.model.ExchangeRate;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExchangeRateDto(String currencyCode, LocalDate date, BigDecimal rate) {

    public static ExchangeRateDto from(ExchangeRate entity) {
        return new ExchangeRateDto(entity.getCurrencyCode(), entity.getDate(), entity.getRate());
    }
}
