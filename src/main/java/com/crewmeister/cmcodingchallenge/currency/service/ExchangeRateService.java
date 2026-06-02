package com.crewmeister.cmcodingchallenge.currency.service;

import com.crewmeister.cmcodingchallenge.currency.dto.ExchangeRateDto;

import java.time.LocalDate;
import java.util.List;

public interface ExchangeRateService {

    // UC2 — all rates at all dates (from H2)
    List<ExchangeRateDto> getAllExchangeRates();

    // UC3 — all currencies for one day
    List<ExchangeRateDto> getExchangeRatesForDate(LocalDate date);

    // UC3 — single currency for one day
    ExchangeRateDto getExchangeRateForCurrencyAndDate(String currencyCode, LocalDate date);
}
