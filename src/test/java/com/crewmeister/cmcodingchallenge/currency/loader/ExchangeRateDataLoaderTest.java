package com.crewmeister.cmcodingchallenge.currency.loader;

import com.crewmeister.cmcodingchallenge.currency.client.ExchangeRateProvider;
import com.crewmeister.cmcodingchallenge.currency.model.ExchangeRate;
import com.crewmeister.cmcodingchallenge.currency.repository.ExchangeRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ParameterizedPreparedStatementSetter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExchangeRateDataLoaderTest {

    @Mock private ExchangeRateProvider exchangeRateProvider;
    @Mock private ExchangeRateRepository repository;
    @Mock private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private ExchangeRateDataLoader loader;

    @BeforeEach
    void setUp() {
        // Default: empty DB → full load path
        when(repository.findMaxDate()).thenReturn(Optional.empty());
    }

    @Test
    void fullLoad_whenDatabaseIsEmpty() {
        when(exchangeRateProvider.fetchExchangeRates(null)).thenReturn(List.of(
                new ExchangeRate("USD", LocalDate.of(2024, 1, 2), new BigDecimal("1.0935"))
        ));

        loader.loadIncrementally();

        // Calling directly bypasses @Async so the test is synchronous and deterministic.
        verify(exchangeRateProvider).fetchExchangeRates(null);
        verify(jdbcTemplate).batchUpdate(
                contains("MERGE INTO exchange_rates"),
                anyList(), anyInt(), any(ParameterizedPreparedStatementSetter.class));
    }

    @Test
    void incrementalLoad_fetchesOnlyFromDayAfterMaxDate() {
        LocalDate maxLoaded = LocalDate.of(2024, 1, 10);
        when(repository.findMaxDate()).thenReturn(Optional.of(maxLoaded));
        when(exchangeRateProvider.fetchExchangeRates(maxLoaded.plusDays(1))).thenReturn(List.of(
                new ExchangeRate("USD", maxLoaded.plusDays(1), new BigDecimal("1.0900"))
        ));

        loader.loadIncrementally();

        verify(exchangeRateProvider).fetchExchangeRates(eq(maxLoaded.plusDays(1)));
    }

    @Test
    void skipsInsert_whenAlreadyUpToDate() {
        when(repository.findMaxDate()).thenReturn(Optional.of(LocalDate.now()));

        loader.loadIncrementally();

        verify(exchangeRateProvider, never()).fetchExchangeRates(any());
        verify(jdbcTemplate, never()).batchUpdate(anyString(), anyList(), anyInt(), any());
    }

    @Test
    void skipsInsert_whenProviderReturnsNoRates() {
        when(exchangeRateProvider.fetchExchangeRates(any())).thenReturn(List.of());

        loader.loadIncrementally();

        verify(jdbcTemplate, never()).batchUpdate(anyString(), anyList(), anyInt(), any());
    }
}
