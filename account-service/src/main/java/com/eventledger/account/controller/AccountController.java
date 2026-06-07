package com.eventledger.account.controller;

import com.eventledger.account.dto.AccountDetailsResponse;
import com.eventledger.account.dto.ApplyTransactionRequest;
import com.eventledger.account.dto.BalanceResponse;
import com.eventledger.account.dto.TransactionResponse;
import com.eventledger.account.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
@Tag(name = "Accounts", description = "Account and transaction management")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/accounts/{accountId}/transactions")
    @Operation(summary = "Apply a transaction to an account")
    public ResponseEntity<TransactionResponse> applyTransaction(
            @PathVariable String accountId,
            @Valid @RequestBody ApplyTransactionRequest request) {
        TransactionResponse response = accountService.applyTransaction(accountId, request);
        HttpStatus status = response.duplicate() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(response);
    }

    @GetMapping("/accounts/{accountId}/balance")
    @Operation(summary = "Get current account balance")
    public BalanceResponse getBalance(@PathVariable String accountId) {
        return accountService.getBalance(accountId);
    }

    @GetMapping("/accounts/{accountId}")
    @Operation(summary = "Get account details and recent transactions")
    public AccountDetailsResponse getAccountDetails(@PathVariable String accountId) {
        return accountService.getAccountDetails(accountId);
    }
}
