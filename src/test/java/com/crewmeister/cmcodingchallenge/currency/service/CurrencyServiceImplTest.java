package com.crewmeister.cmcodingchallenge.currency.service;

import com.crewmeister.cmcodingchallenge.currency.client.ExchangeRateProvider;
import com.crewmeister.cmcodingchallenge.currency.exception.BundesbankClientException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrencyServiceImplTest {

    @Mock
    private ExchangeRateProvider exchangeRateProvider;

    @InjectMocks
    private CurrencyServiceImpl service;

    @Test
    void returnsWhateverTheProviderProvides() {
        List<String> expected = List.of("GBP", "TRY", "USD");
        when(exchangeRateProvider.fetchAvailableCurrencies()).thenReturn(expected);

        List<String> result = service.getAvailableCurrencies();

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void delegatesToTheProvider() {
        when(exchangeRateProvider.fetchAvailableCurrencies()).thenReturn(List.of());

        service.getAvailableCurrencies();

        verify(exchangeRateProvider).fetchAvailableCurrencies();
    }

    @Test
    void letsProviderExceptionBubbleUp() {
        when(exchangeRateProvider.fetchAvailableCurrencies())
                .thenThrow(new BundesbankClientException("upstream is down"));

        assertThatThrownBy(() -> service.getAvailableCurrencies())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("upstream is down");
    }
}
