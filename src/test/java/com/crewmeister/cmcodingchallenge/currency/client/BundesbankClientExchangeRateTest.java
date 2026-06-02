package com.crewmeister.cmcodingchallenge.currency.client;

import com.crewmeister.cmcodingchallenge.currency.exception.BundesbankClientException;
import com.crewmeister.cmcodingchallenge.currency.model.ExchangeRate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BundesbankClientExchangeRateTest {

    private static final String BASE_URL = "http://localhost";

    // Minimal BBK CSV matching the actual format observed from the Bundesbank API.
    // Header: alternating value/flags columns; metadata rows 2-13; data from row 14.
    private static final String STUB_CSV = """
            "";BBEX3.D.USD.EUR.BB.AC.000;BBEX3.D.USD.EUR.BB.AC.000_FLAGS;BBEX3.D.TRY.EUR.BB.AC.000;BBEX3.D.TRY.EUR.BB.AC.000_FLAGS
            "";US dollar;;Turkish lira;
            Dezimalstellen;4;;4;
            Dimension;Eins;;Eins;
            Einheit;USD;;TRY;
            Format der Zeitangabe;P1D;;P1D;
            Kategorie;WEDE;;WEDE;
            Kommentar;comment;;comment;
            Kommentar (englisch);comment;;comment;
            Quelle;ECB;;ECB;
            Quelle (englisch);ECB;;ECB;
            Stand vom;02.06.2026;;02.06.2026;
            2024-01-01;.;Kein Wert vorhanden;.;Kein Wert vorhanden
            2024-01-02;1,0935;;32,5684;
            2024-01-03;1,0879;;32,1234;
            """;

    @Mock
    private RestTemplate restTemplate;

    private BundesbankClient client;

    @BeforeEach
    void setUp() {
        client = new BundesbankClient(restTemplate, BASE_URL);
    }

    @Test
    void parsesAllTradingDayRatesForAllCurrencies() throws IOException {
        stubWith(buildZip(STUB_CSV));

        List<ExchangeRate> rates = client.fetchExchangeRates(null);

        // Stub has 2 trading days × 2 currencies = 4 records (2024-01-01 has no value)
        assertThat(rates).hasSize(4);
    }

    @Test
    void parsesCurrencyCodeDateAndRateCorrectly() throws IOException {
        stubWith(buildZip(STUB_CSV));

        List<ExchangeRate> rates = client.fetchExchangeRates(null);

        ExchangeRate usdJan2 = rates.stream()
                .filter(r -> "USD".equals(r.getCurrencyCode())
                        && r.getDate().equals(LocalDate.of(2024, 1, 2)))
                .findFirst()
                .orElseThrow();

        assertThat(usdJan2.getRate()).isEqualByComparingTo(new BigDecimal("1.0935"));
    }

    @Test
    void skipsMissingValuesMarkedWithDot() throws IOException {
        stubWith(buildZip(STUB_CSV));

        List<ExchangeRate> rates = client.fetchExchangeRates(null);

        // 2024-01-01 has "." for all currencies — must not appear
        assertThat(rates)
                .extracting(ExchangeRate::getDate)
                .doesNotContain(LocalDate.of(2024, 1, 1));
    }

    @Test
    void convertsGermanCommaDecimalsToStandardBigDecimal() throws IOException {
        stubWith(buildZip(STUB_CSV));

        List<ExchangeRate> rates = client.fetchExchangeRates(null);

        ExchangeRate tryJan2 = rates.stream()
                .filter(r -> "TRY".equals(r.getCurrencyCode())
                        && r.getDate().equals(LocalDate.of(2024, 1, 2)))
                .findFirst()
                .orElseThrow();

        // "32,5684" in the CSV must become 32.5684 as a BigDecimal
        assertThat(tryJan2.getRate()).isEqualByComparingTo(new BigDecimal("32.5684"));
    }

    @Test
    void appendsStartPeriodToUrl_whenFromDateIsProvided() throws IOException {
        LocalDate from = LocalDate.of(2024, 1, 1);
        // Verify the URL contains the startPeriod by asserting on the mock matcher
        when(restTemplate.exchange(
                contains("startPeriod=2024-01-01"), eq(HttpMethod.GET), any(), eq(byte[].class)))
                .thenReturn(ResponseEntity.ok(buildZip(STUB_CSV)));

        List<ExchangeRate> rates = client.fetchExchangeRates(from);

        assertThat(rates).isNotEmpty();
    }

    @Test
    void throwsBundesbankClientException_whenApiIsUnreachable() {
        when(restTemplate.exchange(contains("bbk_csv_zip"), eq(HttpMethod.GET), any(), eq(byte[].class)))
                .thenThrow(new RestClientException("Connection refused"));

        assertThatThrownBy(() -> client.fetchExchangeRates(null))
                .isInstanceOf(BundesbankClientException.class)
                .hasMessageContaining("Failed to fetch exchange rates");
    }

    @Test
    void throwsBundesbankClientException_whenZipContainsNoCsvEntry() throws IOException {
        // Build a ZIP with a non-CSV file
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            zos.putNextEntry(new ZipEntry("readme.txt"));
            zos.write("not a csv".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        stubWith(baos.toByteArray());

        assertThatThrownBy(() -> client.fetchExchangeRates(null))
                .isInstanceOf(BundesbankClientException.class)
                .hasMessageContaining("No CSV file found");
    }

    // --- helpers ---

    @SuppressWarnings("unchecked")
    private void stubWith(byte[] zipBytes) {
        when(restTemplate.exchange(
                contains("bbk_csv_zip"), eq(HttpMethod.GET), any(), eq(byte[].class)))
                .thenReturn((ResponseEntity) ResponseEntity.ok(zipBytes));
    }

    private byte[] buildZip(String csvContent) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            zos.putNextEntry(new ZipEntry("BBEX3.D(01).csv"));
            zos.write(csvContent.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        return baos.toByteArray();
    }
}
