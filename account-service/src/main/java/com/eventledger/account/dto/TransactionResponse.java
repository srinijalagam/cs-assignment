package com.eventledger.account.dto;

import com.eventledger.account.domain.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Schema(description = "Applied transaction details")
public record TransactionResponse(
        @Schema(description = "Event identifier")
        String eventId,
        @Schema(description = "Account identifier")
        String accountId,
        @Schema(description = "Transaction type")
        TransactionType type,
        @Schema(description = "Amount")
        BigDecimal amount,
        @Schema(description = "Currency")
        String currency,
        @Schema(description = "Event timestamp")
        Instant eventTimestamp,
        @Schema(description = "Metadata")
        Map<String, Object> metadata,
        @Schema(description = "True if this was a duplicate submission")
        boolean duplicate
) {
}
