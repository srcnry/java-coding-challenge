package com.crewmeister.cmcodingchallenge.currency.service;

import com.crewmeister.cmcodingchallenge.currency.dto.ConversionResultDto;
import com.crewmeister.cmcodingchallenge.currency.dto.ExchangeRateDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ExchangeRateService {

    // UC2 — all rates at all dates (from H2)
    List<ExchangeRateDto> getAllExchangeRates();

    // UC3 — all currencies for one day
    List<ExchangeRateDto> getExchangeRatesForDate(LocalDate date);

    // UC4 — convert a foreign amount to EUR using the ECB rate on a specific day
    ConversionResultDto convertToEur(String currencyCode, LocalDate date, BigDecimal amount);
}
