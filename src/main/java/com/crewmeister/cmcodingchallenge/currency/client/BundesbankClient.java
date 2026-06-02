package com.crewmeister.cmcodingchallenge.currency.client;

import com.crewmeister.cmcodingchallenge.currency.exception.BundesbankClientException;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * HTTP client for the Bundesbank SDMX REST API.
 *
 * Uses {@code format=sdmx_json&detail=serieskeyonly} on a wildcard query to retrieve
 * the complete list of active ECB reference rate currencies as compact JSON (~8 KB),
 * without transferring any observation data.
 */
@Component
public class BundesbankClient {

    private static final Logger log = LoggerFactory.getLogger(BundesbankClient.class);

    // SDMX-JSON with serieskeyonly: all currency dimension values in one small response.
    // JSON is lighter than the equivalent XML (~8 KB vs larger), has no namespace complexity,
    // and all currency codes are available in a single flat array.
    private static final String SERIES_KEYS_PATH =
            "/data/BBEX3/D..EUR.BB.AC.000?format=sdmx_json&detail=serieskeyonly";

    // ID of the dimension that holds the ISO 4217 currency code in the SDMX-JSON structure.
    private static final String CURRENCY_DIMENSION_ID = "BBK_STD_CURRENCY";

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final ObjectMapper objectMapper;

    public BundesbankClient(RestTemplate restTemplate,
                            @Value("${bundesbank.api.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        // ObjectMapper is thread-safe once configured — safe to share as a field.
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Returns the ISO 4217 codes of all currencies that have an active ECB daily
     * reference rate series on the Bundesbank, sorted alphabetically.
     *
     * <p>The SDMX-JSON response structure places all currency codes in one flat array:
     * {@code data.structure.dimensions.series[n].values[*].id}
     * where {@code n} is the index of the {@code BBK_STD_CURRENCY} dimension.
     * No per-series iteration is needed — a single array lookup is sufficient.
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

            // Navigate to the series dimensions array:
            // data → structure → dimensions → series → [...dimension objects...]
            JsonNode seriesDimensions = root
                    .path("data")
                    .path("structure")
                    .path("dimensions")
                    .path("series");

            // Find the BBK_STD_CURRENCY dimension by id rather than by index position,
            // so the parser stays correct if the API ever reorders or adds dimensions.
            for (JsonNode dimension : seriesDimensions) {
                if (CURRENCY_DIMENSION_ID.equals(dimension.path("id").asText())) {
                    List<String> codes = new ArrayList<>();
                    for (JsonNode value : dimension.path("values")) {
                        String code = value.path("id").asText();
                        if (!code.isBlank()) {
                            codes.add(code);
                        }
                    }
                    // The API returns codes in series-key order; sort for stable output.
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
}
