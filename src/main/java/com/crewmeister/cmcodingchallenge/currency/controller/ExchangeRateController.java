package com.crewmeister.cmcodingchallenge.currency.controller;

import com.crewmeister.cmcodingchallenge.currency.dto.ExchangeRateDto;
import com.crewmeister.cmcodingchallenge.currency.service.ExchangeRateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
                          "available history (~1999 onward). Data is loaded from H2 (populated " +
                          "asynchronously at startup from the Bundesbank bbk_csv_zip feed and " +
                          "refreshed daily). Returns an empty array while the initial load is still running."
    )
    @ApiResponse(responseCode = "200", description = "Collection of all exchange rates")
    @GetMapping("/exchange-rates")
    public ResponseEntity<List<ExchangeRateDto>> getAllExchangeRates() {
        return ResponseEntity.ok(exchangeRateService.getAllExchangeRates());
    }
}
