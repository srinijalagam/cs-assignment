package com.eventledger.gateway.controller;

import com.eventledger.gateway.client.AccountServiceClient;
import com.eventledger.gateway.dto.BalanceResponse;
import com.eventledger.gateway.exception.AccountServiceUnavailableException;
import com.eventledger.gateway.exception.GlobalExceptionHandler;
import com.eventledger.gateway.metrics.EventMetrics;
import com.eventledger.gateway.metrics.MetricsInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = BalanceController.class)
@Import({GlobalExceptionHandler.class, MetricsInterceptor.class, EventMetrics.class})
class BalanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountServiceClient accountServiceClient;

    @Test
    void returnsBalanceWhenAccountServiceUp() throws Exception {
        when(accountServiceClient.getBalance("acct-1"))
                .thenReturn(new BalanceResponse("acct-1", new BigDecimal("100.00"), "USD"));

        mockMvc.perform(get("/accounts/acct-1/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(100.00));
    }

    @Test
    void returns503WhenAccountServiceUnavailable() throws Exception {
        when(accountServiceClient.getBalance("acct-1"))
                .thenThrow(new AccountServiceUnavailableException("Account service is unavailable"));

        mockMvc.perform(get("/accounts/acct-1/balance"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value("Account service is unavailable"));
    }
}
