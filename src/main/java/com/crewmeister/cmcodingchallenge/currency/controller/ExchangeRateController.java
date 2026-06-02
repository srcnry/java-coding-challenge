package com.crewmeister.cmcodingchallenge.currency.controller;

import com.crewmeister.cmcodingchallenge.currency.dto.ConversionResultDto;
import com.crewmeister.cmcodingchallenge.currency.dto.ExchangeRateDto;
import com.crewmeister.cmcodingchallenge.currency.service.ExchangeRateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Tag(name = "Exchange Rates", description = "EUR-FX daily reference rates sourced from the Bundesbank")
@RestController
@RequestMapping("/api")
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    public ExchangeRateController(ExchangeRateService exchangeRateService) {
        this.exchangeRateService = exchangeRateService;
    }

    @Operation(
            summary = "Get all EUR-FX exchange rates at all available dates",
            description = "Returns every ECB daily reference rate for every currency from the full " +
                          "available history. Data is served from H2, populated at startup from the " +
                          "Bundesbank bbk_csv_zip feed. Returns an empty array while loading."
    )
    @ApiResponse(responseCode = "200", description = "Complete collection of exchange rates")
    @GetMapping("/exchange-rates")
    public ResponseEntity<List<ExchangeRateDto>> getAllExchangeRates() {
        return ResponseEntity.ok(exchangeRateService.getAllExchangeRates());
    }

    @Operation(
            summary = "Get all EUR-FX rates for a specific date",
            description = "Returns every currency's ECB reference rate for the given trading day. " +
                          "Returns 404 for weekends, bank holidays, and dates outside the loaded range."
    )
    @ApiResponse(responseCode = "200", description = "All rates for the requested date")
    @ApiResponse(responseCode = "404", description = "No rates for this date (weekend / holiday / out of range)")
    @ApiResponse(responseCode = "400", description = "Date is not in yyyy-MM-dd format")
    @GetMapping("/exchange-rates/{date}")
    public ResponseEntity<List<ExchangeRateDto>> getExchangeRatesForDate(
            @Parameter(description = "Trading date in yyyy-MM-dd format", example = "2024-03-15")
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(exchangeRateService.getExchangeRatesForDate(date));
    }

    @Operation(
            summary = "Convert a foreign amount to EUR on a specific date",
            description = "Looks up the ECB reference rate for the given currency and date from H2, " +
                          "then computes: convertedEur = amount / rate. " +
                          "The BBEX3 rate is expressed as units of foreign currency per 1 EUR, " +
                          "so dividing inverts the direction correctly (e.g. rate 1.0852 USD/EUR → " +
                          "100 USD / 1.0852 ≈ 92.15 EUR)."
    )
    @ApiResponse(responseCode = "200", description = "Conversion result including the rate used")
    @ApiResponse(responseCode = "404", description = "No rate for this currency on this date")
    @ApiResponse(responseCode = "400", description = "Invalid date format or amount ≤ 0")
    @GetMapping("/exchange-rates/{date}/{currency}/convert")
    public ResponseEntity<ConversionResultDto> convertToEur(
            @Parameter(description = "Trading date in yyyy-MM-dd format", example = "2024-03-15")
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "ISO 4217 currency code", example = "USD")
            @PathVariable String currency,
            @Parameter(description = "Amount in the foreign currency to convert", example = "100.00")
            @RequestParam BigDecimal amount) {
        return ResponseEntity.ok(exchangeRateService.convertToEur(currency, date, amount));
    }
}
