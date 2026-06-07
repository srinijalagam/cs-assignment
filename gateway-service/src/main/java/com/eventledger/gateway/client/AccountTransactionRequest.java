package com.eventledger.gateway.client;

import com.eventledger.gateway.domain.EventType;
import com.eventledger.gateway.dto.EventRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public record AccountTransactionRequest(
        String eventId,
        EventType type,
        BigDecimal amount,
        String currency,
        Instant eventTimestamp,
        Map<String, Object> metadata
) {
    public static AccountTransactionRequest from(EventRequest request) {
        return new AccountTransactionRequest(
                request.eventId(),
                request.type(),
                request.amount(),
                request.currency(),
                request.eventTimestamp(),
                request.metadata()
        );
    }
}
