package com.crewmeister.cmcodingchallenge.currency.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ConversionResultDto(
        String currencyCode,
        LocalDate date,
        BigDecimal originalAmount,
        BigDecimal convertedAmountInEur,
        BigDecimal rate
) {}
