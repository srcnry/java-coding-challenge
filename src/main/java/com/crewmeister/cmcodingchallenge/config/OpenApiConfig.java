package com.crewmeister.cmcodingchallenge.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("EUR-FX Exchange Rate Service")
                        .description("""
                                Foreign exchange rate service backed by the German Bundesbank's \
                                public SDMX API. Provides ECB daily reference rates (middle rate, \
                                series type BB) for all active currencies from 1999 onward.

                                Exchange rate data is loaded asynchronously at startup and stored \
                                in a file-based H2 database. The /api/currencies endpoint fetches \
                                live from Bundesbank and caches results for 24 hours.""")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Crewmeister")
                                .url("https://www.crewmeister.com")));
    }
}
