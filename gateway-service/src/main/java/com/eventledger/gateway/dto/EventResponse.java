package com.eventledger.gateway.dto;

import com.eventledger.gateway.domain.EventType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Schema(description = "Stored transaction event")
public record EventResponse(
        @Schema(description = "Event identifier")
        String eventId,
        @Schema(description = "Account identifier")
        String accountId,
        @Schema(description = "Event type")
        EventType type,
        @Schema(description = "Amount")
        BigDecimal amount,
        @Schema(description = "Currency")
        String currency,
        @Schema(description = "Original event timestamp")
        Instant eventTimestamp,
        @Schema(description = "Metadata")
        Map<String, Object> metadata,
        @Schema(description = "When the gateway stored the event")
        Instant storedAt
) {
}
