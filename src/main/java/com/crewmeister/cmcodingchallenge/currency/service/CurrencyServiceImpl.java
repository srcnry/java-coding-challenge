package com.crewmeister.cmcodingchallenge.currency.service;

import com.crewmeister.cmcodingchallenge.config.CacheConfig;
import com.crewmeister.cmcodingchallenge.currency.client.ExchangeRateProvider;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CurrencyServiceImpl implements CurrencyService {

    private final ExchangeRateProvider exchangeRateProvider;

    public CurrencyServiceImpl(ExchangeRateProvider exchangeRateProvider) {
        this.exchangeRateProvider = exchangeRateProvider;
    }

    @Override
    // The available currency list changes only when the ECB adds or removes a currency
    // from its reference rate series — at most a few times per decade. Caching for one
    // day avoids a live Bundesbank HTTP call on every /api/currencies request.
    @Cacheable(CacheConfig.CURRENCIES_CACHE)
    public List<String> getAvailableCurrencies() {
        return exchangeRateProvider.fetchAvailableCurrencies();
    }
}
