package com.crewmeister.cmcodingchallenge.currency.repository;

import com.crewmeister.cmcodingchallenge.currency.model.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

    // Used by the incremental loader to decide how far back to fetch.
    @Query("SELECT MAX(e.date) FROM ExchangeRate e")
    Optional<LocalDate> findMaxDate();

    // UC3 — all currencies on a specific date.
    // Uses idx_er_date; returns empty list for weekends / holidays / unknown dates.
    List<ExchangeRate> findByDate(LocalDate date);

    // UC3 — single currency on a specific date.
    // Uses idx_er_currency_date (leading column = currency_code).
    Optional<ExchangeRate> findByCurrencyCodeAndDate(String currencyCode, LocalDate date);
}
