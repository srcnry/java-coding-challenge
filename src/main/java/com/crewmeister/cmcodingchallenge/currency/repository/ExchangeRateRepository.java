package com.crewmeister.cmcodingchallenge.currency.repository;

import com.crewmeister.cmcodingchallenge.currency.model.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.Optional;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

    // Used by the incremental loader to decide how far back to fetch.
    @Query("SELECT MAX(e.date) FROM ExchangeRate e")
    Optional<LocalDate> findMaxDate();
}
