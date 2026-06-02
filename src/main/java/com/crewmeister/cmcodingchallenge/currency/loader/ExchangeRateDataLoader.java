package com.crewmeister.cmcodingchallenge.currency.loader;

import com.crewmeister.cmcodingchallenge.currency.client.ExchangeRateProvider;
import com.crewmeister.cmcodingchallenge.currency.model.ExchangeRate;
import com.crewmeister.cmcodingchallenge.currency.repository.ExchangeRateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.Date;
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
 *
 * <p>Persistence uses {@link JdbcTemplate#batchUpdate} instead of JPA {@code saveAll()} to
 * bypass the ORM entity lifecycle (dirty checking, L1 cache, flush) for a write-once bulk
 * operation. This is significantly faster for the ~180 K rows of the initial full load.
 */
@Component
public class ExchangeRateDataLoader {

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateDataLoader.class);

    // H2's native upsert syntax. MERGE inserts a new row when (currency_code, date) is not
    // present and silently updates it when it already exists — making the operation idempotent.
    // This replaces PostgreSQL-style ON CONFLICT which H2 2.2 does not support.
    private static final String INSERT_SQL =
            "MERGE INTO exchange_rates (currency_code, date, rate) " +
            "KEY (currency_code, date) VALUES (?, ?, ?)";

    private final ExchangeRateProvider exchangeRateProvider;
    private final ExchangeRateRepository repository;
    private final JdbcTemplate jdbcTemplate;

    // Field initializer acts as a safe default when Spring's @Value is not active (e.g. tests).
    @Value("${loader.history.start-date:1999-01-01}")
    private String historyStartDateStr = "1999-01-01";

    @Value("${loader.jdbc.batch-size:1000}")
    private int jdbcBatchSize = 1000;

    public ExchangeRateDataLoader(ExchangeRateProvider exchangeRateProvider,
                                  ExchangeRateRepository repository,
                                  JdbcTemplate jdbcTemplate) {
        this.exchangeRateProvider = exchangeRateProvider;
        this.repository = repository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Async("startupLoader")
    public void onApplicationReady() {
        try {
            loadIncrementally();
        } catch (Exception e) {
            log.error("Startup exchange rate load failed", e);
        }
    }

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
                maxDate.isEmpty() ? null : from
        );

        if (rates.isEmpty()) {
            log.info("No new exchange rates to persist");
            return;
        }

        bulkInsert(rates);

        long elapsed = System.currentTimeMillis() - start;
        log.info("Persisted {} exchange rate records in {}ms", rates.size(), elapsed);
    }

    /**
     * Inserts exchange rates via raw JDBC in configurable batches.
     *
     * <p>Why not JPA {@code saveAll()}?
     * <ul>
     *   <li>JPA runs every record through dirty checking, L1 cache writes, and flush — pure
     *       overhead for a write-once bulk operation that never reads the entities back.</li>
     *   <li>JDBC {@code batchUpdate} sends {@value #INSERT_SQL} statements in groups of
     *       {@code loader.jdbc.batch-size} rows per round-trip, cutting round-trips from
     *       ~180K to ~180 for a full history load.</li>
     * </ul>
     *
     * <p>The {@code ON CONFLICT DO NOTHING} clause makes the insert idempotent — if the
     * scheduler fires while a startup load is still in progress, duplicate rows are silently
     * skipped rather than causing a constraint violation.
     */
    private void bulkInsert(List<ExchangeRate> rates) {
        jdbcTemplate.batchUpdate(INSERT_SQL, rates, jdbcBatchSize, (ps, rate) -> {
            ps.setString(1, rate.getCurrencyCode());
            ps.setDate(2, Date.valueOf(rate.getDate()));
            ps.setBigDecimal(3, rate.getRate());
        });
    }
}
