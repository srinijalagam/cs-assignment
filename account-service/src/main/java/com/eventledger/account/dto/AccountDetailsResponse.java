package com.eventledger.account.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Account details with recent transactions")
public record AccountDetailsResponse(
        @Schema(description = "Account identifier")
        String accountId,
        @Schema(description = "Current balance")
        BigDecimal balance,
        @Schema(description = "Currency")
        String currency,
        @Schema(description = "Recent transactions ordered by event timestamp")
        List<TransactionResponse> recentTransactions
) {
}
