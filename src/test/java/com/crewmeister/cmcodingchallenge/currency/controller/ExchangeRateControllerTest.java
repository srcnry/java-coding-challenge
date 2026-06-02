package com.crewmeister.cmcodingchallenge.currency.controller;

import com.crewmeister.cmcodingchallenge.currency.dto.ExchangeRateDto;
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
                .andExpect(jsonPath("$[0].rate").value(1.0935))
                .andExpect(jsonPath("$[1].currencyCode").value("TRY"))
                .andExpect(jsonPath("$[1].rate").value(32.5684));
    }

    @Test
    void getAllExchangeRates_returnsEmptyArray_whenDataNotYetLoaded() throws Exception {
        when(exchangeRateService.getAllExchangeRates()).thenReturn(List.of());

        mockMvc.perform(get("/api/exchange-rates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
