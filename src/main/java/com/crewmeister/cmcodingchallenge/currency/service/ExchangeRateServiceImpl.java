package com.crewmeister.cmcodingchallenge.currency.service;

import com.crewmeister.cmcodingchallenge.currency.dto.ExchangeRateDto;
import com.crewmeister.cmcodingchallenge.currency.repository.ExchangeRateRepository;
import org.springframework.stereotype.Service;

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
}
