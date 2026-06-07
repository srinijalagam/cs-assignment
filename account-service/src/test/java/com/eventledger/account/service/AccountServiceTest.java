package com.eventledger.account.service;

import com.eventledger.account.config.AccountProperties;
import com.eventledger.account.domain.TransactionType;
import com.eventledger.account.dto.ApplyTransactionRequest;
import com.eventledger.account.metrics.TransactionMetrics;
import com.eventledger.account.repository.TransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({AccountService.class, AccountProperties.class, TransactionMetrics.class, ObjectMapper.class})
class AccountServiceTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private TransactionRepository transactionRepository;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
    }

    @Test
    void appliesCreditAndDebitInAnyOrder() {
        accountService.applyTransaction("acct-1", request("evt-2", TransactionType.DEBIT, "50.00",
                Instant.parse("2026-05-15T15:00:00Z")));
        accountService.applyTransaction("acct-1", request("evt-1", TransactionType.CREDIT, "150.00",
                Instant.parse("2026-05-15T14:00:00Z")));

        assertThat(accountService.getBalance("acct-1").balance())
                .isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    void duplicateEventIsIdempotent() {
        ApplyTransactionRequest request = request("evt-dup", TransactionType.CREDIT, "25.00",
                Instant.parse("2026-05-15T14:00:00Z"));

        var first = accountService.applyTransaction("acct-1", request);
        var second = accountService.applyTransaction("acct-1", request);

        assertThat(first.duplicate()).isFalse();
        assertThat(second.duplicate()).isTrue();
        assertThat(transactionRepository.count()).isEqualTo(1);
        assertThat(accountService.getBalance("acct-1").balance())
                .isEqualByComparingTo(new BigDecimal("25.00"));
    }

    private ApplyTransactionRequest request(String eventId, TransactionType type, String amount, Instant timestamp) {
        return new ApplyTransactionRequest(
                eventId,
                type,
                new BigDecimal(amount),
                "USD",
                timestamp,
                Map.of("source", "test")
        );
    }
}
