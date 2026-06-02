package com.crewmeister.cmcodingchallenge.currency.service;

import com.crewmeister.cmcodingchallenge.currency.dto.ConversionResultDto;
import com.crewmeister.cmcodingchallenge.currency.dto.ExchangeRateDto;
import com.crewmeister.cmcodingchallenge.currency.exception.ExchangeRateNotFoundException;
import com.crewmeister.cmcodingchallenge.currency.model.ExchangeRate;
import com.crewmeister.cmcodingchallenge.currency.repository.ExchangeRateRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
public class ExchangeRateServiceImpl implements ExchangeRateService {

    private final ExchangeRateRepository repository;

    public ExchangeRateServiceImpl(ExchangeRateRepository repository) {
        this.repository = repository;
    }

    @Override
    public Page<ExchangeRateDto> getAllExchangeRates(Pageable pageable) {
        // JpaRepository.findAll(Pageable) issues a count query + a data query;
        // both are fast because H2 keeps everything in memory.
        return repository.findAll(pageable).map(ExchangeRateDto::from);
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
    public ConversionResultDto convertToEur(String currencyCode, LocalDate date, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero, got: " + amount);
        }

        ExchangeRate exchangeRate = repository.findByCurrencyCodeAndDate(currencyCode, date)
                .orElseThrow(() -> new ExchangeRateNotFoundException(currencyCode, date));

        // The BBEX3 rate is expressed as "units of foreign currency per 1 EUR",
        // so dividing the foreign amount by the rate gives the EUR equivalent.
        // e.g. rate = 1.0852 USD/EUR → 100 USD / 1.0852 ≈ 92.15 EUR
        BigDecimal convertedAmount = amount.divide(
                exchangeRate.getRate(), 6, RoundingMode.HALF_UP);

        return new ConversionResultDto(
                currencyCode, date, amount, convertedAmount, exchangeRate.getRate());
    }
}
