package com.crewmeister.cmcodingchallenge.currency.controller;

import com.crewmeister.cmcodingchallenge.currency.dto.ConversionResultDto;
import com.crewmeister.cmcodingchallenge.currency.dto.ErrorResponse;
import com.crewmeister.cmcodingchallenge.currency.dto.ExchangeRateDto;
import com.crewmeister.cmcodingchallenge.currency.service.ExchangeRateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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

@Tag(name = "Exchange Rates",
     description = "EUR-FX daily ECB reference rates loaded from the Bundesbank and served from H2")
@RestController
@RequestMapping("/api")
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    public ExchangeRateController(ExchangeRateService exchangeRateService) {
        this.exchangeRateService = exchangeRateService;
    }

    @Operation(
            summary = "Get all EUR-FX exchange rates (paginated)",
            description = "Returns ECB daily reference rates from H2, one page at a time. " +
                          "Default: 20 most-recent records sorted by date descending. " +
                          "Use `?page=N&size=M&sort=field,dir` to control paging and ordering. " +
                          "Returns an empty page while the initial startup load is still running."
    )
    @ApiResponse(responseCode = "200", description = "Page of exchange rates with pagination metadata")
    @GetMapping("/exchange-rates")
    public ResponseEntity<Page<ExchangeRateDto>> getAllExchangeRates(
            @ParameterObject
            @PageableDefault(size = 20, sort = "date", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(exchangeRateService.getAllExchangeRates(pageable));
    }

    @Operation(
            summary = "Get all EUR-FX rates for a specific trading day",
            description = "Returns every currency's ECB reference rate for the given date from H2. " +
                          "Returns 404 for weekends, bank holidays, and dates outside the loaded range."
    )
    @ApiResponse(
            responseCode = "200",
            description = "All rates for the requested date",
            content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = ExchangeRateDto.class))
            )
    )
    @ApiResponse(
            responseCode = "404",
            description = "No rates for this date — weekend, bank holiday, or outside loaded range",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @ApiResponse(
            responseCode = "400",
            description = "Date is not in yyyy-MM-dd format",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @GetMapping("/exchange-rates/{date}")
    public ResponseEntity<List<ExchangeRateDto>> getExchangeRatesForDate(
            @Parameter(description = "Trading date in yyyy-MM-dd format", example = "2024-03-15", required = true)
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(exchangeRateService.getExchangeRatesForDate(date));
    }

    @Operation(
            summary = "Convert a foreign currency amount to EUR on a specific date",
            description = "Looks up the ECB reference rate for the given currency and date, " +
                          "then computes: `convertedEur = amount / rate`. " +
                          "The BBEX3 rate is expressed as units of foreign currency per 1 EUR — " +
                          "dividing inverts the direction correctly " +
                          "(e.g. rate 1.0852 USD/EUR → 100 USD / 1.0852 ≈ 92.15 EUR)."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Conversion result including the original amount, EUR equivalent, and rate used",
            content = @Content(schema = @Schema(implementation = ConversionResultDto.class))
    )
    @ApiResponse(
            responseCode = "404",
            description = "No rate for this currency on this date",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid date format, missing amount parameter, or amount ≤ 0",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @GetMapping("/exchange-rates/{date}/{currency}/convert")
    public ResponseEntity<ConversionResultDto> convertToEur(
            @Parameter(description = "Trading date in yyyy-MM-dd format", example = "2024-03-15", required = true)
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "ISO 4217 currency code", example = "USD", required = true)
            @PathVariable String currency,
            @Parameter(description = "Amount in the foreign currency to convert (must be > 0)",
                       example = "100.00", required = true)
            @RequestParam BigDecimal amount) {
        return ResponseEntity.ok(exchangeRateService.convertToEur(currency, date, amount));
    }
}
