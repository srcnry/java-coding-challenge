package com.crewmeister.cmcodingchallenge.currency.service;

import com.crewmeister.cmcodingchallenge.currency.client.BundesbankClient;
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
    private BundesbankClient bundesbankClient;

    @InjectMocks
    private CurrencyServiceImpl service;

    @Test
    void returnsWhateverTheClientProvides() {
        List<String> expected = List.of("GBP", "TRY", "USD");
        when(bundesbankClient.fetchAvailableCurrencies()).thenReturn(expected);

        List<String> result = service.getAvailableCurrencies();

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void delegatesToTheClient() {
        when(bundesbankClient.fetchAvailableCurrencies()).thenReturn(List.of());

        service.getAvailableCurrencies();

        verify(bundesbankClient).fetchAvailableCurrencies();
    }

    @Test
    void letsBundesbankClientExceptionBubbleUp() {
        when(bundesbankClient.fetchAvailableCurrencies())
                .thenThrow(new BundesbankClientException("upstream is down"));

        assertThatThrownBy(() -> service.getAvailableCurrencies())
                .isInstanceOf(BundesbankClientException.class)
                .hasMessage("upstream is down");
    }
}
