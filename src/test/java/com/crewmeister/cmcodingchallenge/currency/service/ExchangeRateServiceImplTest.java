package com.crewmeister.cmcodingchallenge.currency.service;

import com.crewmeister.cmcodingchallenge.currency.dto.ConversionResultDto;
import com.crewmeister.cmcodingchallenge.currency.dto.ExchangeRateDto;
import com.crewmeister.cmcodingchallenge.currency.exception.ExchangeRateNotFoundException;
import com.crewmeister.cmcodingchallenge.currency.model.ExchangeRate;
import com.crewmeister.cmcodingchallenge.currency.repository.ExchangeRateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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
    void getAllExchangeRates_returnsMappedDtosInPage() {
        Pageable pageable = PageRequest.of(0, 20);
        ExchangeRate rate = new ExchangeRate("USD", LocalDate.of(2024, 1, 2), new BigDecimal("1.0935"));
        when(repository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(rate), pageable, 1));

        Page<ExchangeRateDto> result = service.getAllExchangeRates(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).currencyCode()).isEqualTo("USD");
        assertThat(result.getContent().get(0).rate()).isEqualByComparingTo(new BigDecimal("1.0935"));
    }

    @Test
    void getAllExchangeRates_returnsEmptyPage_whenDatabaseIsEmpty() {
        Pageable pageable = PageRequest.of(0, 20);
        when(repository.findAll(pageable)).thenReturn(Page.empty(pageable));

        Page<ExchangeRateDto> result = service.getAllExchangeRates(pageable);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
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

    // ── UC4 — convert foreign amount to EUR ───────────────────────────────────────────────────

    @Test
    void convertToEur_returnsCorrectConversionResult() {
        LocalDate date = LocalDate.of(2024, 3, 15);
        // Rate: 1 EUR = 1.0852 USD → 100 USD / 1.0852 ≈ 92.15 EUR
        when(repository.findByCurrencyCodeAndDate("USD", date))
                .thenReturn(Optional.of(new ExchangeRate("USD", date, new BigDecimal("1.0852"))));

        ConversionResultDto result = service.convertToEur("USD", date, new BigDecimal("100.00"));

        assertThat(result.currencyCode()).isEqualTo("USD");
        assertThat(result.date()).isEqualTo(date);
        assertThat(result.originalAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(result.rate()).isEqualByComparingTo(new BigDecimal("1.0852"));
        // 100 / 1.0852 = 92.15... rounded to 6dp
        assertThat(result.convertedAmountInEur()).isEqualByComparingTo(
                new BigDecimal("100.00").divide(new BigDecimal("1.0852"), 6, java.math.RoundingMode.HALF_UP));
    }

    @Test
    void convertToEur_throws404_whenNoRateForCurrencyAndDate() {
        LocalDate date = LocalDate.of(2024, 3, 16);
        when(repository.findByCurrencyCodeAndDate("USD", date)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.convertToEur("USD", date, new BigDecimal("100")))
                .isInstanceOf(ExchangeRateNotFoundException.class)
                .hasMessageContaining("USD")
                .hasMessageContaining("2024-03-16");
    }

    @Test
    void convertToEur_throws400_whenAmountIsZero() {
        assertThatThrownBy(() -> service.convertToEur("USD", LocalDate.now(), BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("greater than zero");
    }

    @Test
    void convertToEur_throws400_whenAmountIsNegative() {
        assertThatThrownBy(() -> service.convertToEur("USD", LocalDate.now(), new BigDecimal("-50")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("greater than zero");
    }
}
