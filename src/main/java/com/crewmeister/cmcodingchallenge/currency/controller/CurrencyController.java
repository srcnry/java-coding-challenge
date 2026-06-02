package com.crewmeister.cmcodingchallenge.currency.controller;

import com.crewmeister.cmcodingchallenge.currency.service.CurrencyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Currencies", description = "Lists all currencies that have an active ECB reference rate series on the Bundesbank")
@RestController
@RequestMapping("/api")
public class CurrencyController {

    private final CurrencyService currencyService;

    public CurrencyController(CurrencyService currencyService) {
        this.currencyService = currencyService;
    }

    @Operation(
            summary = "List all available currencies",
            description = "Returns the ISO 4217 codes of every currency for which the Bundesbank " +
                          "publishes a daily ECB reference rate (series type BB, middle rate). " +
                          "Derived from the BBEX3 series catalogue via detail=serieskeyonly — " +
                          "no observation data is transferred."
    )
    @ApiResponse(responseCode = "200", description = "Sorted list of ISO 4217 currency codes",
            content = @Content(array = @ArraySchema(schema = @Schema(type = "string", example = "USD"))))
    @ApiResponse(responseCode = "502", description = "Bundesbank API is unreachable")
    @GetMapping("/currencies")
    public ResponseEntity<List<String>> getCurrencies() {
        return ResponseEntity.ok(currencyService.getAvailableCurrencies());
    }
}
