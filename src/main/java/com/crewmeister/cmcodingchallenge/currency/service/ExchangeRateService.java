package com.crewmeister.cmcodingchallenge.currency.service;

import com.crewmeister.cmcodingchallenge.currency.dto.ExchangeRateDto;

import java.util.List;

public interface ExchangeRateService {
    List<ExchangeRateDto> getAllExchangeRates();
}
