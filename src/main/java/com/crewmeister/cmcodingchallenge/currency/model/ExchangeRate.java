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
        // Prevents duplicate rows if the loader or scheduler ever overlap on the same day.
        uniqueConstraints = @UniqueConstraint(
                name = "uq_er_currency_date", columnNames = {"currency_code", "date"}
        ),
        indexes = {
                @Index(name = "idx_er_currency_date", columnList = "currency_code, date"),
                // Dedicated date index — the composite above can't serve date-only queries
                // because currency_code is the leading column.
                @Index(name = "idx_er_date", columnList = "date")
        }
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
