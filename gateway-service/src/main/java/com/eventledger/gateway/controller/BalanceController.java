package com.eventledger.gateway.controller;

import com.eventledger.gateway.client.AccountServiceClient;
import com.eventledger.gateway.dto.BalanceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Balance", description = "Account balance queries proxied to the Account Service")
public class BalanceController {

    private final AccountServiceClient accountServiceClient;

    public BalanceController(AccountServiceClient accountServiceClient) {
        this.accountServiceClient = accountServiceClient;
    }

    @GetMapping("/accounts/{accountId}/balance")
    @Operation(summary = "Get account balance; returns 503 when the Account Service is unreachable")
    public BalanceResponse getBalance(@PathVariable String accountId) {
        return accountServiceClient.getBalance(accountId);
    }
}
