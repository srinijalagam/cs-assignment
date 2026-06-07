package com.eventledger.gateway.dto;

import com.eventledger.gateway.domain.EventType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Schema(description = "Transaction event submitted by upstream systems")
public record EventRequest(
        @NotBlank(message = "eventId is required")
        @Schema(description = "Unique event identifier", example = "evt-001")
        String eventId,

        @NotBlank(message = "accountId is required")
        @Schema(description = "Account identifier", example = "acct-123")
        String accountId,

        @NotNull(message = "type is required")
        @Schema(description = "Event type", example = "CREDIT")
        EventType type,

        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be greater than 0")
        @Schema(description = "Transaction amount", example = "150.00")
        BigDecimal amount,

        @NotBlank(message = "currency is required")
        @Schema(description = "Currency code", example = "USD")
        String currency,

        @NotNull(message = "eventTimestamp is required")
        @Schema(description = "When the event originally occurred")
        Instant eventTimestamp,

        @Schema(description = "Optional metadata")
        Map<String, Object> metadata
) {
}
