package com.crewmeister.cmcodingchallenge.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    // Single named constant so callers reference the cache name without magic strings.
    public static final String CURRENCIES_CACHE = "currencies";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(CURRENCIES_CACHE);
        manager.setCaffeine(
                Caffeine.newBuilder()
                        // Currency lists change very rarely — one day is a safe TTL that
                        // avoids a live Bundesbank call on every /api/currencies request
                        // while still picking up any new ECB currencies added to the series.
                        .expireAfterWrite(1, TimeUnit.DAYS)
                        // The cached value is a single list (no-arg method key), so max 1
                        // entry is enough. A small ceiling also bounds memory use clearly.
                        .maximumSize(1)
                        .recordStats()
        );
        return manager;
    }
}
