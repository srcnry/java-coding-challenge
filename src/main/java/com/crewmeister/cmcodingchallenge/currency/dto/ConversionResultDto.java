package com.crewmeister.cmcodingchallenge.currency.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Result of converting a foreign currency amount to EUR")
public record ConversionResultDto(

        @Schema(description = "ISO 4217 code of the source currency", example = "USD")
        String currencyCode,

        @Schema(description = "Date the ECB reference rate was sourced from", example = "2024-03-15")
        LocalDate date,

        @Schema(description = "Amount in the source currency as supplied by the caller",
                example = "100.00")
        BigDecimal originalAmount,

        @Schema(description = "Equivalent amount in EUR — computed as originalAmount ÷ rate (6 dp, HALF_UP)",
                example = "92.153285")
        BigDecimal convertedAmountInEur,

        @Schema(description = "ECB reference rate used: units of source currency per 1 EUR",
                example = "1.0852")
        BigDecimal rate
) {}
