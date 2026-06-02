package com.crewmeister.cmcodingchallenge.currency.controller;

import com.crewmeister.cmcodingchallenge.currency.dto.ConversionResultDto;
import com.crewmeister.cmcodingchallenge.currency.dto.ExchangeRateDto;
import com.crewmeister.cmcodingchallenge.currency.exception.ExchangeRateNotFoundException;
import com.crewmeister.cmcodingchallenge.currency.service.ExchangeRateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExchangeRateController.class)
class ExchangeRateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExchangeRateService exchangeRateService;

    // ── UC2 ────────────────────────────────────────────────────────────────────────────────────

    @Test
    void getAllExchangeRates_returns200WithCollection() throws Exception {
        when(exchangeRateService.getAllExchangeRates()).thenReturn(List.of(
                new ExchangeRateDto("USD", LocalDate.of(2024, 1, 2), new BigDecimal("1.0935")),
                new ExchangeRateDto("TRY", LocalDate.of(2024, 1, 2), new BigDecimal("32.5684"))
        ));

        mockMvc.perform(get("/api/exchange-rates"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].currencyCode").value("USD"))
                .andExpect(jsonPath("$[0].date").value("2024-01-02"))
                .andExpect(jsonPath("$[0].rate").value(1.0935));
    }

    @Test
    void getAllExchangeRates_returnsEmptyArray_whenDataNotYetLoaded() throws Exception {
        when(exchangeRateService.getAllExchangeRates()).thenReturn(List.of());

        mockMvc.perform(get("/api/exchange-rates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ── UC3 — all currencies for a date ───────────────────────────────────────────────────────

    @Test
    void getExchangeRatesForDate_returns200WithAllCurrencies() throws Exception {
        LocalDate date = LocalDate.of(2024, 3, 15);
        when(exchangeRateService.getExchangeRatesForDate(date)).thenReturn(List.of(
                new ExchangeRateDto("USD", date, new BigDecimal("1.0852")),
                new ExchangeRateDto("TRY", date, new BigDecimal("32.57"))
        ));

        mockMvc.perform(get("/api/exchange-rates/2024-03-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].currencyCode").value("USD"))
                .andExpect(jsonPath("$[0].date").value("2024-03-15"));
    }

    @Test
    void getExchangeRatesForDate_returns404_whenDateIsWeekendOrHoliday() throws Exception {
        LocalDate saturday = LocalDate.of(2024, 3, 16);
        when(exchangeRateService.getExchangeRatesForDate(saturday))
                .thenThrow(new ExchangeRateNotFoundException(saturday));

        mockMvc.perform(get("/api/exchange-rates/2024-03-16"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value(containsString("2024-03-16")));
    }

    @Test
    void getExchangeRatesForDate_returns400_whenDateFormatIsInvalid() throws Exception {
        mockMvc.perform(get("/api/exchange-rates/not-a-date"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    // ── UC4 — convert foreign amount to EUR ───────────────────────────────────────────────────

    @Test
    void convertToEur_returns200WithConversionResult() throws Exception {
        LocalDate date = LocalDate.of(2024, 3, 15);
        ConversionResultDto result = new ConversionResultDto(
                "USD", date,
                new BigDecimal("100.00"),
                new BigDecimal("92.153285"),
                new BigDecimal("1.0852")
        );
        when(exchangeRateService.convertToEur("USD", date, new BigDecimal("100.00")))
                .thenReturn(result);

        mockMvc.perform(get("/api/exchange-rates/2024-03-15/USD/convert?amount=100.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currencyCode").value("USD"))
                .andExpect(jsonPath("$.date").value("2024-03-15"))
                .andExpect(jsonPath("$.originalAmount").value(100.00))
                .andExpect(jsonPath("$.convertedAmountInEur").value(92.153285))
                .andExpect(jsonPath("$.rate").value(1.0852));
    }

    @Test
    void convertToEur_returns404_whenNoRateForCurrencyAndDate() throws Exception {
        LocalDate date = LocalDate.of(2024, 3, 16);
        when(exchangeRateService.convertToEur("USD", date, new BigDecimal("100")))
                .thenThrow(new ExchangeRateNotFoundException("USD", date));

        mockMvc.perform(get("/api/exchange-rates/2024-03-16/USD/convert?amount=100"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value(containsString("USD")));
    }

    @Test
    void convertToEur_returns400_whenAmountIsNegative() throws Exception {
        when(exchangeRateService.convertToEur("USD", LocalDate.of(2024, 3, 15), new BigDecimal("-10")))
                .thenThrow(new IllegalArgumentException("Amount must be greater than zero, got: -10"));

        mockMvc.perform(get("/api/exchange-rates/2024-03-15/USD/convert?amount=-10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("greater than zero")));
    }

    @Test
    void convertToEur_returns400_whenAmountParamIsMissing() throws Exception {
        mockMvc.perform(get("/api/exchange-rates/2024-03-15/USD/convert"))
                .andExpect(status().isBadRequest());
    }
}
