package com.crewmeister.cmcodingchallenge.currency.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        name = "exchange_rates",
        // The UNIQUE constraint on (currency_code, date) creates a B-tree index automatically —
        // the database uses it for both uniqueness enforcement and point lookups.
        // A separate regular index on the same columns would be redundant.
        uniqueConstraints = @UniqueConstraint(
                name = "uq_er_currency_date", columnNames = {"currency_code", "date"}
        ),
        // A dedicated date-only index is still necessary because uq_er_currency_date has
        // currency_code as the leading column, so date-only queries (findByDate) cannot
        // use that index efficiently — the database would resort to a full table scan.
        indexes = @Index(name = "idx_er_date", columnList = "date")
)
public class ExchangeRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "currency_code", nullable = false)
    private String currencyCode;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "rate", nullable = false, precision = 19, scale = 6)
    private BigDecimal rate;

    protected ExchangeRate() {}

    public ExchangeRate(String currencyCode, LocalDate date, BigDecimal rate) {
        this.currencyCode = currencyCode;
        this.date = date;
        this.rate = rate;
    }

    public String getCurrencyCode() { return currencyCode; }
    public LocalDate getDate()      { return date; }
    public BigDecimal getRate()     { return rate; }
}
