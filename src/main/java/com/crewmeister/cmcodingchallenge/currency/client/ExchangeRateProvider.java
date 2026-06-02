package com.crewmeister.cmcodingchallenge.currency.client;

import com.crewmeister.cmcodingchallenge.currency.model.ExchangeRate;

import java.time.LocalDate;
import java.util.List;

/**
 * Contract for fetching EUR-FX exchange rate data from an external provider.
 *
 * <p>Decoupling the service and loader layers from the concrete HTTP client means
 * switching to a different data provider (e.g. ECB direct, a paid FX feed) only
 * requires a new implementation of this interface — no changes to business logic.
 *
 * <p>Current implementation: {@link BundesbankClient}.
 */
public interface ExchangeRateProvider {

    /**
     * Returns the ISO 4217 codes of all currencies with active EUR-FX rates,
     * sorted alphabetically.
     */
    List<String> fetchAvailableCurrencies();

    /**
     * Fetches EUR-FX exchange rates starting from {@code from} (inclusive).
     *
     * @param from earliest date to include, or {@code null} to fetch the full history.
     */
    List<ExchangeRate> fetchExchangeRates(LocalDate from);
}
