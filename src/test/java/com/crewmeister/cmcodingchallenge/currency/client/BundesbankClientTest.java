package com.crewmeister.cmcodingchallenge.currency.client;

import com.crewmeister.cmcodingchallenge.currency.exception.BundesbankClientException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BundesbankClientTest {

    private static final String BASE_URL = "http://localhost";

    @Mock
    private RestTemplate restTemplate;

    private BundesbankClient client;

    @BeforeEach
    void setUp() {
        client = new BundesbankClient(restTemplate, BASE_URL);
    }

    @Test
    void returnsCurrenciesSortedAlphabetically() throws IOException {
        stubWith(loadStubJson());

        List<String> currencies = client.fetchAvailableCurrencies();

        // Stub has USD, TRY, GBP in that order — must be returned sorted
        assertThat(currencies).containsExactly("GBP", "TRY", "USD");
    }

    @Test
    void returnsEmptyList_whenCurrencyDimensionHasNoValues() {
        String jsonWithEmptyValues = """
                {
                  "data": {
                    "structure": {
                      "dimensions": {
                        "series": [
                          { "id": "BBK_STD_CURRENCY", "values": [] }
                        ]
                      }
                    }
                  }
                }
                """;
        stubWith(jsonWithEmptyValues);

        List<String> currencies = client.fetchAvailableCurrencies();

        assertThat(currencies).isEmpty();
    }

    @Test
    void throwsBundesbankClientException_whenCurrencyDimensionIsMissing() {
        String jsonWithoutCurrencyDimension = """
                {
                  "data": {
                    "structure": {
                      "dimensions": {
                        "series": [
                          { "id": "SOME_OTHER_DIMENSION", "values": [] }
                        ]
                      }
                    }
                  }
                }
                """;
        stubWith(jsonWithoutCurrencyDimension);

        assertThatThrownBy(() -> client.fetchAvailableCurrencies())
                .isInstanceOf(BundesbankClientException.class)
                .hasMessageContaining("BBK_STD_CURRENCY");
    }

    @Test
    void throwsBundesbankClientException_whenApiIsUnreachable() {
        when(restTemplate.exchange(contains("sdmx_json"), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenThrow(new ResourceAccessException("Connection refused"));

        assertThatThrownBy(() -> client.fetchAvailableCurrencies())
                .isInstanceOf(BundesbankClientException.class)
                .hasMessageContaining("Failed to fetch available currencies");
    }

    @Test
    void throwsBundesbankClientException_whenResponseIsNotValidJson() {
        stubWith("this is not valid json { {");

        assertThatThrownBy(() -> client.fetchAvailableCurrencies())
                .isInstanceOf(BundesbankClientException.class)
                .hasMessageContaining("Failed to parse");
    }

    // --- helpers ---

    @SuppressWarnings("unchecked")
    private void stubWith(String body) {
        when(restTemplate.exchange(contains("sdmx_json"), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn((ResponseEntity) ResponseEntity.ok(body));
    }

    private String loadStubJson() throws IOException {
        return Files.readString(Path.of("src/test/resources/stub-serieskeyonly.json"));
    }
}
