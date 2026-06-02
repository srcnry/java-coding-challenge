package com.crewmeister.cmcodingchallenge.currency.service;

import com.crewmeister.cmcodingchallenge.currency.dto.ExchangeRateDto;
import com.crewmeister.cmcodingchallenge.currency.exception.ExchangeRateNotFoundException;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExchangeRateServiceImplTest {

    @Mock
    private ExchangeRateRepository repository;

    @InjectMocks
    private ExchangeRateServiceImpl service;

    // ── UC2 ────────────────────────────────────────────────────────────────────────────────────

    @Test
    void getAllExchangeRates_returnsMappedDtos() {
        ExchangeRate rate = new ExchangeRate("USD", LocalDate.of(2024, 1, 2), new BigDecimal("1.0935"));
        when(repository.findAll()).thenReturn(List.of(rate));

        List<ExchangeRateDto> result = service.getAllExchangeRates();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).currencyCode()).isEqualTo("USD");
        assertThat(result.get(0).rate()).isEqualByComparingTo(new BigDecimal("1.0935"));
    }

    @Test
    void getAllExchangeRates_returnsEmpty_whenDatabaseIsEmpty() {
        when(repository.findAll()).thenReturn(List.of());

        assertThat(service.getAllExchangeRates()).isEmpty();
    }

    // ── UC3 — all currencies for a date ───────────────────────────────────────────────────────

    @Test
    void getExchangeRatesForDate_returnsAllRatesForThatDay() {
        LocalDate date = LocalDate.of(2024, 3, 15);
        ExchangeRate usd = new ExchangeRate("USD", date, new BigDecimal("1.0935"));
        ExchangeRate try_ = new ExchangeRate("TRY", date, new BigDecimal("32.57"));
        when(repository.findByDate(date)).thenReturn(List.of(usd, try_));

        List<ExchangeRateDto> result = service.getExchangeRatesForDate(date);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(ExchangeRateDto::currencyCode)
                .containsExactlyInAnyOrder("USD", "TRY");
    }

    @Test
    void getExchangeRatesForDate_throws404_whenDateIsWeekendOrHoliday() {
        LocalDate saturday = LocalDate.of(2024, 3, 16);
        when(repository.findByDate(saturday)).thenReturn(List.of());

        assertThatThrownBy(() -> service.getExchangeRatesForDate(saturday))
                .isInstanceOf(ExchangeRateNotFoundException.class)
                .hasMessageContaining("2024-03-16");
    }

    @Test
    void getExchangeRatesForDate_delegatesToRepository() {
        LocalDate date = LocalDate.of(2024, 3, 15);
        when(repository.findByDate(date)).thenReturn(List.of(
                new ExchangeRate("USD", date, new BigDecimal("1.0935"))));

        service.getExchangeRatesForDate(date);

        verify(repository).findByDate(date);
    }
}
