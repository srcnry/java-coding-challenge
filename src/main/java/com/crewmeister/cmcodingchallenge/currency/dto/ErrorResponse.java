package com.crewmeister.cmcodingchallenge.currency.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Error response body returned by all failed requests")
public record ErrorResponse(

        @Schema(description = "Human-readable description of what went wrong",
                example = "No exchange rates found for 2024-03-16.")
        String error
) {}
