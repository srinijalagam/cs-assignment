package com.eventledger.account.controller;

import com.eventledger.account.domain.TransactionType;
import com.eventledger.account.dto.AccountDetailsResponse;
import com.eventledger.account.dto.BalanceResponse;
import com.eventledger.account.dto.TransactionResponse;
import com.eventledger.account.exception.AccountNotFoundException;
import com.eventledger.account.exception.GlobalExceptionHandler;
import com.eventledger.account.service.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AccountController.class)
@Import(GlobalExceptionHandler.class)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountService accountService;

    @Test
    void applyTransactionReturns201ForNewTransaction() throws Exception {
        when(accountService.applyTransaction(eq("acct-1"), any()))
                .thenReturn(transaction("evt-1", false));

        mockMvc.perform(post("/accounts/acct-1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventId": "evt-1",
                                  "type": "CREDIT",
                                  "amount": 10.00,
                                  "currency": "USD",
                                  "eventTimestamp": "2026-05-15T14:00:00Z"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventId").value("evt-1"))
                .andExpect(jsonPath("$.duplicate").value(false));
    }

    @Test
    void applyTransactionReturns200ForDuplicate() throws Exception {
        when(accountService.applyTransaction(eq("acct-1"), any()))
                .thenReturn(transaction("evt-1", true));

        mockMvc.perform(post("/accounts/acct-1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventId": "evt-1",
                                  "type": "CREDIT",
                                  "amount": 10.00,
                                  "currency": "USD",
                                  "eventTimestamp": "2026-05-15T14:00:00Z"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duplicate").value(true));
    }

    @Test
    void getBalanceReturns200WithBalance() throws Exception {
        when(accountService.getBalance("acct-1"))
                .thenReturn(new BalanceResponse("acct-1", new BigDecimal("100.00"), "USD"));

        mockMvc.perform(get("/accounts/acct-1/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value("acct-1"))
                .andExpect(jsonPath("$.balance").value(100.00))
                .andExpect(jsonPath("$.currency").value("USD"));
    }

    @Test
    void getBalanceReturns404ForUnknownAccount() throws Exception {
        when(accountService.getBalance("acct-missing"))
                .thenThrow(new AccountNotFoundException("acct-missing"));

        mockMvc.perform(get("/accounts/acct-missing/balance"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAccountDetailsReturns200WithRecentTransactions() throws Exception {
        AccountDetailsResponse details = new AccountDetailsResponse(
                "acct-1", new BigDecimal("60.00"), "USD",
                List.of(transaction("evt-1", false), transaction("evt-2", false))
        );
        when(accountService.getAccountDetails("acct-1")).thenReturn(details);

        mockMvc.perform(get("/accounts/acct-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value("acct-1"))
                .andExpect(jsonPath("$.balance").value(60.00))
                .andExpect(jsonPath("$.recentTransactions.length()").value(2));
    }

    private TransactionResponse transaction(String eventId, boolean duplicate) {
        return new TransactionResponse(
                eventId, "acct-1", TransactionType.CREDIT, new BigDecimal("10.00"),
                "USD", Instant.parse("2026-05-15T14:00:00Z"), Map.of(), duplicate
        );
    }
}
