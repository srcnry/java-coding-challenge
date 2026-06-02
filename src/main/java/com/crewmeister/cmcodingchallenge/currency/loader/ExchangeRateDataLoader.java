package com.crewmeister.cmcodingchallenge.currency.loader;

import com.crewmeister.cmcodingchallenge.currency.client.ExchangeRateProvider;
import com.crewmeister.cmcodingchallenge.currency.model.ExchangeRate;
import com.crewmeister.cmcodingchallenge.currency.repository.ExchangeRateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Loads EUR-FX exchange rate history into H2 at startup and keeps it fresh via a daily cron job.
 *
 * <p>Strategy:
 * <ol>
 *   <li>On startup ({@code ApplicationReadyEvent}): check the latest date already in the DB.
 *       If empty → fetch full history from {@code loader.history.start-date}.
 *       If partial → fetch only the gap from {@code maxDate + 1} to today.</li>
 *   <li>Daily at 17:00 CET (Mon–Fri): run the same incremental check to pick up rates
 *       published by the ECB earlier that afternoon.</li>
 * </ol>
 */
@Component
public class ExchangeRateDataLoader {

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateDataLoader.class);

    private final ExchangeRateProvider exchangeRateProvider;
    private final ExchangeRateRepository repository;

    // Field initializer acts as a safe default when Spring's @Value is not active (e.g. tests).
    @Value("${loader.history.start-date:1999-01-01}")
    private String historyStartDateStr = "1999-01-01";

    public ExchangeRateDataLoader(ExchangeRateProvider exchangeRateProvider,
                                  ExchangeRateRepository repository) {
        this.exchangeRateProvider = exchangeRateProvider;
        this.repository = repository;
    }

    // Fires once the application is fully started — HTTP server is already accepting requests
    // while the data loads in the background.
    @EventListener(ApplicationReadyEvent.class)
    @Async("startupLoader")
    public void onApplicationReady() {
        try {
            loadIncrementally();
        } catch (Exception e) {
            log.error("Startup exchange rate load failed", e);
        }
    }

    // ECB publishes new rates at ~16:00 CET; running at 17:00 provides a safe margin.
    // zone = "Europe/Berlin" handles CET/CEST (daylight saving) automatically.
    @Scheduled(cron = "0 0 17 * * MON-FRI", zone = "Europe/Berlin")
    public void scheduledDailyRefresh() {
        log.info("Running scheduled daily exchange rate refresh...");
        try {
            loadIncrementally();
        } catch (Exception e) {
            log.error("Scheduled exchange rate refresh failed", e);
        }
    }

    /**
     * Fetches and persists only the exchange rates that are not yet in the database.
     * Called by both the startup listener and the daily scheduler.
     */
    public void loadIncrementally() {
        Optional<LocalDate> maxDate = repository.findMaxDate();
        LocalDate from = maxDate
                .map(d -> d.plusDays(1))
                .orElseGet(() -> LocalDate.parse(historyStartDateStr));

        if (from.isAfter(LocalDate.now())) {
            log.info("Exchange rates are already up to date (latest: {})", maxDate.orElse(null));
            return;
        }

        log.info("Loading exchange rates from {} to today...", from);
        long start = System.currentTimeMillis();

        List<ExchangeRate> rates = exchangeRateProvider.fetchExchangeRates(
                // Pass null for the very first load so the API returns the full history.
                // For incremental runs, restrict to what's missing.
                maxDate.isEmpty() ? null : from
        );

        if (rates.isEmpty()) {
            log.info("No new exchange rates to persist");
            return;
        }

        try {
            repository.saveAll(rates);
            long elapsed = System.currentTimeMillis() - start;
            log.info("Persisted {} exchange rate records in {}ms", rates.size(), elapsed);
        } catch (DataIntegrityViolationException e) {
            // Can happen if the scheduler fires while a startup load is still running.
            // The unique constraint on (currency_code, date) prevents actual duplicates.
            log.warn("Some exchange rates were already present — skipping duplicates");
        }
    }
}
