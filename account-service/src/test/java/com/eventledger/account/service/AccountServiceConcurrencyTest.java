package com.eventledger.account.service;

import com.eventledger.account.config.AccountProperties;
import com.eventledger.account.domain.TransactionEntity;
import com.eventledger.account.domain.TransactionType;
import com.eventledger.account.dto.ApplyTransactionRequest;
import com.eventledger.account.dto.TransactionResponse;
import com.eventledger.account.metrics.TransactionMetrics;
import com.eventledger.account.repository.TransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class AccountServiceConcurrencyTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private AccountProperties accountProperties;

    @Mock
    private TransactionMetrics transactionMetrics;

    @InjectMocks
    private AccountService accountService;

    @Test
    void resolvesConcurrentDuplicateToExistingTransaction() {
        ApplyTransactionRequest request = new ApplyTransactionRequest(
                "evt-race", TransactionType.CREDIT, new BigDecimal("10.00"),
                "USD", Instant.parse("2026-05-15T14:00:00Z"), Map.of()
        );
        TransactionEntity entity = new TransactionEntity(
                "evt-race", "acct-1", TransactionType.CREDIT, new BigDecimal("10.00"),
                "USD", Instant.parse("2026-05-15T14:00:00Z"), null
        );
        // First lookup misses; unique constraint rejects the save; catch-block lookup hits.
        when(transactionRepository.findByEventId("evt-race"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(entity));
        when(transactionRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("duplicate event_id"));

        TransactionResponse response = accountService.applyTransaction("acct-1", request);

        assertThat(response.duplicate()).isTrue();
        assertThat(response.eventId()).isEqualTo("evt-race");
        verify(transactionMetrics, never()).incrementApplied();
    }
}
