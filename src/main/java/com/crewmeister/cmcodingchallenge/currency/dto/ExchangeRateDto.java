package com.crewmeister.cmcodingchallenge.currency.dto;

import com.crewmeister.cmcodingchallenge.currency.model.ExchangeRate;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "ECB reference rate for one currency on one trading day")
public record ExchangeRateDto(

        @Schema(description = "ISO 4217 currency code", example = "USD")
        String currencyCode,

        @Schema(description = "Trading date — weekends and bank holidays have no entry",
                example = "2024-03-15")
        LocalDate date,

        @Schema(description = "Number of currency units per 1 EUR (ECB middle rate)",
                example = "1.0852")
        BigDecimal rate
) {

    public static ExchangeRateDto from(ExchangeRate entity) {
        return new ExchangeRateDto(entity.getCurrencyCode(), entity.getDate(), entity.getRate());
    }
}
