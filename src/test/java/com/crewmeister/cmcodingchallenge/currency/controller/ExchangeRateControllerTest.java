package com.crewmeister.cmcodingchallenge.currency.controller;

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
}
