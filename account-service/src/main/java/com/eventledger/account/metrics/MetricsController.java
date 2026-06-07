package com.eventledger.account.metrics;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Tag(name = "Metrics", description = "Custom service metrics")
public class MetricsController {

    private final TransactionMetrics transactionMetrics;

    public MetricsController(TransactionMetrics transactionMetrics) {
        this.transactionMetrics = transactionMetrics;
    }

    @GetMapping("/metrics/custom")
    @Operation(summary = "Custom transaction metrics")
    public Map<String, Object> customMetrics() {
        return Map.of(
                "service", "account-service",
                "transactionsApplied", transactionMetrics.getTransactionsApplied()
        );
    }
}
