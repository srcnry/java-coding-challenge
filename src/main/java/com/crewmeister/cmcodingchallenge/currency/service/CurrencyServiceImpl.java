package com.crewmeister.cmcodingchallenge.currency.service;

import com.crewmeister.cmcodingchallenge.currency.client.BundesbankClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CurrencyServiceImpl implements CurrencyService {

    private final BundesbankClient bundesbankClient;

    public CurrencyServiceImpl(BundesbankClient bundesbankClient) {
        this.bundesbankClient = bundesbankClient;
    }

    @Override
    public List<String> getAvailableCurrencies() {
        return bundesbankClient.fetchAvailableCurrencies();
    }
}
