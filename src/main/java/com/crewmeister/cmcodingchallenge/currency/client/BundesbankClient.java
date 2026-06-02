package com.crewmeister.cmcodingchallenge.currency.client;

import com.crewmeister.cmcodingchallenge.currency.exception.BundesbankClientException;
import com.crewmeister.cmcodingchallenge.currency.model.ExchangeRate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * HTTP client for the Bundesbank SDMX REST API.
 *
 * <ul>
 *   <li>UC1 currencies: {@code sdmx_json + detail=serieskeyonly} (~8 KB JSON)</li>
 *   <li>UC2 exchange rates: {@code bbk_csv_zip} — a ZIP containing one wide-format CSV
 *       with all currencies and all dates in a single request.</li>
 * </ul>
 */
@Component
public class BundesbankClient implements ExchangeRateProvider {

    private static final Logger log = LoggerFactory.getLogger(BundesbankClient.class);

    // ── UC1: currency list ─────────────────────────────────────────────────────────────────────
    private static final String SERIES_KEYS_PATH =
            "/data/BBEX3/D..EUR.BB.AC.000?format=sdmx_json&detail=serieskeyonly";
    private static final String CURRENCY_DIMENSION_ID = "BBK_STD_CURRENCY";

    // ── UC2: exchange rate history ─────────────────────────────────────────────────────────────
    // bbk_csv_zip returns one ZIP file containing a single wide-format CSV where rows are dates
    // and columns are currency series (alternating: value column, flags column).
    private static final String EXCHANGE_RATES_BASE_PATH =
            "/data/BBEX3/D..EUR.BB.AC.000?format=bbk_csv_zip";
    private static final MediaType BBK_CSV_ZIP =
            MediaType.parseMediaType("application/vnd.bbk.data+csv-zip;version=1.0.0");
    // Number of metadata rows at the top of the CSV before the first date row.
    private static final int CSV_METADATA_ROWS = 13;

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final ObjectMapper objectMapper;

    public BundesbankClient(RestTemplate restTemplate,
                            @Value("${bundesbank.api.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.objectMapper = new ObjectMapper();
    }

    // ══════════════════════════════════════════════════════════════════════════════════════════
    // UC1 — Available currencies (SDMX-JSON, series keys only)
    // ══════════════════════════════════════════════════════════════════════════════════════════

    /**
     * Returns the ISO 4217 codes of all currencies with an active ECB daily reference rate
     * series on the Bundesbank, sorted alphabetically.
     */
    public List<String> fetchAvailableCurrencies() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + SERIES_KEYS_PATH, HttpMethod.GET, request, String.class);
            return parseSeriesKeysJson(response.getBody());
        } catch (RestClientException e) {
            throw new BundesbankClientException(
                    "Failed to fetch available currencies from Bundesbank", e);
        }
    }

    private List<String> parseSeriesKeysJson(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode seriesDimensions = root
                    .path("data").path("structure").path("dimensions").path("series");

            for (JsonNode dimension : seriesDimensions) {
                if (CURRENCY_DIMENSION_ID.equals(dimension.path("id").asText())) {
                    List<String> codes = new ArrayList<>();
                    for (JsonNode value : dimension.path("values")) {
                        String code = value.path("id").asText();
                        if (!code.isBlank()) codes.add(code);
                    }
                    codes.sort(Comparator.naturalOrder());
                    log.info("Fetched {} available currencies from Bundesbank", codes.size());
                    return codes;
                }
            }
            throw new BundesbankClientException(
                    "Dimension '" + CURRENCY_DIMENSION_ID + "' not found in SDMX-JSON response");

        } catch (BundesbankClientException e) {
            throw e;
        } catch (Exception e) {
            throw new BundesbankClientException(
                    "Failed to parse Bundesbank SDMX-JSON response", e);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════════════════
    // UC2 — Exchange rate history (bbk_csv_zip)
    // ══════════════════════════════════════════════════════════════════════════════════════════

    /**
     * Fetches EUR-FX exchange rates as a compressed ZIP file containing one wide-format CSV.
     *
     * @param from start date (inclusive). Pass {@code null} for the full history from 1999.
     */
    public List<ExchangeRate> fetchExchangeRates(LocalDate from) {
        String url = baseUrl + EXCHANGE_RATES_BASE_PATH;
        if (from != null) {
            url += "&startPeriod=" + from;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(BBK_CSV_ZIP));
        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    url, HttpMethod.GET, request, byte[].class);
            return extractAndParseCsv(response.getBody());
        } catch (RestClientException e) {
            throw new BundesbankClientException(
                    "Failed to fetch exchange rates from Bundesbank", e);
        }
    }

    private List<ExchangeRate> extractAndParseCsv(byte[] zipBytes) {
        if (zipBytes == null || zipBytes.length == 0) {
            throw new BundesbankClientException("Empty response from Bundesbank exchange rate endpoint");
        }
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().endsWith(".csv")) {
                    String csv = new String(zis.readAllBytes(), StandardCharsets.UTF_8);
                    log.debug("Extracted CSV entry: {} ({} bytes)", entry.getName(), csv.length());
                    return parseBbkCsv(csv);
                }
            }
        } catch (IOException e) {
            throw new BundesbankClientException("Failed to read ZIP response from Bundesbank", e);
        }
        throw new BundesbankClientException("No CSV file found inside Bundesbank ZIP response");
    }

    /**
     * Parses the BBK wide-format CSV.
     *
     * <p>Format summary:
     * <ul>
     *   <li>Row 1: header — {@code "";BBEX3.D.AUD.EUR.BB.AC.000;BBEX3.D.AUD.EUR.BB.AC.000_FLAGS;...}
     *       Currency codes sit at odd column indices (1, 3, 5…); even indices are FLAGS columns.</li>
     *   <li>Rows 2–{@value #CSV_METADATA_ROWS}: metadata (descriptions, units, sources — skip).</li>
     *   <li>Rows 14+: data — {@code 2024-01-02;1,6147;;1,9558;;…}
     *       Column 0 = date; alternating value / flag pairs; German comma decimals; {@code .} = missing.</li>
     * </ul>
     */
    private List<ExchangeRate> parseBbkCsv(String csv) {
        String[] lines = csv.split("\\R", -1);
        if (lines.length == 0) return List.of();

        // --- Parse header row to build ordered list of currency codes ---
        String[] headers = lines[0].split(";", -1);
        List<String> currencyCodes = new ArrayList<>();
        // Odd indices (1, 3, 5…) are the rate columns; even (2, 4…) are FLAGS columns.
        for (int i = 1; i < headers.length; i += 2) {
            String col = headers[i].replace("\"", "").trim();
            if (col.isBlank()) continue;
            // Series key format: "BBEX3.D.AUD.EUR.BB.AC.000" — third segment is the currency code.
            String[] segments = col.split("\\.");
            if (segments.length >= 3) {
                currencyCodes.add(segments[2]);
            }
        }

        if (currencyCodes.isEmpty()) {
            log.warn("No currency codes found in BBK CSV header");
            return List.of();
        }

        // --- Parse data rows (skip the metadata block at the top) ---
        List<ExchangeRate> rates = new ArrayList<>();
        for (int rowIdx = 1; rowIdx < lines.length; rowIdx++) {
            String line = lines[rowIdx];
            if (line.isBlank()) continue;

            String[] cols = line.split(";", -1);
            String dateStr = cols[0].replace("\"", "").trim();

            // Data rows start with ISO dates (YYYY-MM-DD); metadata rows do not.
            if (!dateStr.matches("\\d{4}-\\d{2}-\\d{2}")) continue;

            LocalDate date;
            try {
                date = LocalDate.parse(dateStr);
            } catch (Exception ignored) {
                continue;
            }

            for (int j = 0; j < currencyCodes.size(); j++) {
                int valueColIdx = 1 + j * 2;
                if (valueColIdx >= cols.length) continue;

                String valueStr = cols[valueColIdx].replace("\"", "").trim();

                // "." means no rate published for this date (weekend, holiday, discontinued currency).
                if (valueStr.isEmpty() || ".".equals(valueStr)) continue;

                try {
                    // BBK CSVs use German locale — comma is the decimal separator.
                    BigDecimal rate = new BigDecimal(valueStr.replace(',', '.'));
                    rates.add(new ExchangeRate(currencyCodes.get(j), date, rate));
                } catch (NumberFormatException ignored) {
                    // Skip any row the API returns with an unparseable value.
                }
            }
        }

        return rates;
    }
}
