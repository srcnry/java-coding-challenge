package com.crewmeister.cmcodingchallenge.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class BundesbankClientConfig {

    @Value("${bundesbank.api.connect-timeout-ms}")
    private int connectTimeoutMs;

    @Value("${bundesbank.api.read-timeout-ms}")
    private int readTimeoutMs;

    @Bean
    public RestTemplate restTemplate() {
        // Without explicit timeouts, a slow or unresponsive Bundesbank server would
        // stall the calling thread for as long as the OS defaults allow — potentially minutes.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
        return new RestTemplate(factory);
    }
}
