package com.eventledger.account.metrics;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class TransactionMetrics {

    private final AtomicLong transactionsApplied = new AtomicLong();

    public void incrementApplied() {
        transactionsApplied.incrementAndGet();
    }

    public long getTransactionsApplied() {
        return transactionsApplied.get();
    }
}
