package com.crewmeister.cmcodingchallenge.currency.service;

import com.crewmeister.cmcodingchallenge.currency.client.ExchangeRateProvider;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CurrencyServiceImpl implements CurrencyService {

    private final ExchangeRateProvider exchangeRateProvider;

    public CurrencyServiceImpl(ExchangeRateProvider exchangeRateProvider) {
        this.exchangeRateProvider = exchangeRateProvider;
    }

    @Override
    public List<String> getAvailableCurrencies() {
        return exchangeRateProvider.fetchAvailableCurrencies();
    }
}
