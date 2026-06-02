package com.crewmeister.cmcodingchallenge.currency.service;

import com.crewmeister.cmcodingchallenge.currency.dto.ExchangeRateDto;
import com.crewmeister.cmcodingchallenge.currency.model.ExchangeRate;
import com.crewmeister.cmcodingchallenge.currency.repository.ExchangeRateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExchangeRateServiceImplTest {

    @Mock
    private ExchangeRateRepository repository;

    @InjectMocks
    private ExchangeRateServiceImpl service;

    @Test
    void returnsMappedDtos() {
        ExchangeRate rate = new ExchangeRate("USD", LocalDate.of(2024, 1, 2), new BigDecimal("1.0935"));
        when(repository.findAll()).thenReturn(List.of(rate));

        List<ExchangeRateDto> result = service.getAllExchangeRates();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).currencyCode()).isEqualTo("USD");
        assertThat(result.get(0).date()).isEqualTo(LocalDate.of(2024, 1, 2));
        assertThat(result.get(0).rate()).isEqualByComparingTo(new BigDecimal("1.0935"));
    }

    @Test
    void returnsEmptyList_whenDatabaseIsEmpty() {
        when(repository.findAll()).thenReturn(List.of());

        assertThat(service.getAllExchangeRates()).isEmpty();
    }

    @Test
    void delegatesToRepository() {
        when(repository.findAll()).thenReturn(List.of());

        service.getAllExchangeRates();

        verify(repository).findAll();
    }
}
