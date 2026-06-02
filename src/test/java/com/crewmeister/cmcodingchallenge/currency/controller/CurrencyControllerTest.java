package com.crewmeister.cmcodingchallenge.currency.controller;

import com.crewmeister.cmcodingchallenge.currency.exception.BundesbankClientException;
import com.crewmeister.cmcodingchallenge.currency.service.CurrencyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Loads only the MVC layer — controller and exception handler — without a full app context.
// The service is replaced with a mock so tests stay focused on HTTP behaviour.
@WebMvcTest(CurrencyController.class)
class CurrencyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CurrencyService currencyService;

    @Test
    void getCurrencies_returns200WithSortedCurrencyCodes() throws Exception {
        when(currencyService.getAvailableCurrencies()).thenReturn(List.of("GBP", "TRY", "USD"));

        mockMvc.perform(get("/api/currencies"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0]").value("GBP"))
                .andExpect(jsonPath("$[1]").value("TRY"))
                .andExpect(jsonPath("$[2]").value("USD"));
    }

    @Test
    void getCurrencies_returnsEmptyArray_whenNoCurrenciesAvailable() throws Exception {
        when(currencyService.getAvailableCurrencies()).thenReturn(List.of());

        mockMvc.perform(get("/api/currencies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getCurrencies_returns502_whenBundesbankIsUnreachable() throws Exception {
        // 502 signals that our service is healthy but the upstream Bundesbank API failed.
        when(currencyService.getAvailableCurrencies())
                .thenThrow(new BundesbankClientException("Bundesbank unreachable"));

        mockMvc.perform(get("/api/currencies"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("Bundesbank unreachable"));
    }
}
