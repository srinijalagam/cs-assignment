package com.eventledger.account.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Map;

@RestController
@Tag(name = "Health", description = "Service health checks")
public class HealthController implements HealthIndicator {

    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/health")
    @Operation(summary = "Health check with database connectivity")
    public Map<String, Object> healthCheck() {
        Health health = health();
        return Map.of(
                "status", health.getStatus().getCode(),
                "service", "account-service",
                "database", health.getDetails().getOrDefault("database", "UNKNOWN")
        );
    }

    @Override
    public Health health() {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(2)) {
                return Health.up().withDetail("database", "UP").build();
            }
            return Health.down().withDetail("database", "INVALID").build();
        } catch (Exception ex) {
            return Health.down().withDetail("database", "DOWN").withException(ex).build();
        }
    }
}
