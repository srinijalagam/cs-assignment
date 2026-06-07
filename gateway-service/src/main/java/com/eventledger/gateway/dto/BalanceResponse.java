package com.eventledger.gateway.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Account balance proxied from the Account Service")
public record BalanceResponse(
        @Schema(description = "Account identifier")
        String accountId,
        @Schema(description = "Current net balance")
        BigDecimal balance,
        @Schema(description = "Currency")
        String currency
) {
}
