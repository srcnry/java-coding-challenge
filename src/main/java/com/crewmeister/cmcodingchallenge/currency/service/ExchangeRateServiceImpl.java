package com.crewmeister.cmcodingchallenge.currency.service;

import com.crewmeister.cmcodingchallenge.currency.dto.ExchangeRateDto;
import com.crewmeister.cmcodingchallenge.currency.exception.ExchangeRateNotFoundException;
import com.crewmeister.cmcodingchallenge.currency.repository.ExchangeRateRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class ExchangeRateServiceImpl implements ExchangeRateService {

    private final ExchangeRateRepository repository;

    public ExchangeRateServiceImpl(ExchangeRateRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<ExchangeRateDto> getAllExchangeRates() {
        return repository.findAll()
                .stream()
                .map(ExchangeRateDto::from)
                .toList();
    }

    @Override
    public List<ExchangeRateDto> getExchangeRatesForDate(LocalDate date) {
        List<ExchangeRateDto> rates = repository.findByDate(date)
                .stream()
                .map(ExchangeRateDto::from)
                .toList();

        // An empty result means no trading data for this date — weekend, bank holiday,
        // or a date outside our loaded range. A 404 is more informative than an empty list.
        if (rates.isEmpty()) {
            throw new ExchangeRateNotFoundException(date);
        }
        return rates;
    }

    @Override
    public ExchangeRateDto getExchangeRateForCurrencyAndDate(String currencyCode, LocalDate date) {
        return repository.findByCurrencyCodeAndDate(currencyCode, date)
                .map(ExchangeRateDto::from)
                .orElseThrow(() -> new ExchangeRateNotFoundException(currencyCode, date));
    }
}
