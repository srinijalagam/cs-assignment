package com.eventledger.gateway.resiliency;

import com.eventledger.gateway.domain.EventType;
import com.eventledger.gateway.dto.EventRequest;
import com.eventledger.gateway.exception.AccountServiceUnavailableException;
import com.eventledger.gateway.service.EventService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@TestPropertySource(properties = "test.context=circuit-breaker")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CircuitBreakerIntegrationTest {

    @Autowired
    private EventService eventService;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        // Point the gateway at an unreachable port so every Account Service call fails.
        registry.add("gateway.account-service.base-url", () -> "http://localhost:59999");
    }

    @BeforeEach
    void resetBreaker() {
        circuitBreakerRegistry.circuitBreaker("accountService").reset();
    }

    @Test
    void circuitOpensAfterRepeatedAccountServiceFailures() {
        CircuitBreaker breaker = circuitBreakerRegistry.circuitBreaker("accountService");

        for (int i = 0; i < 5; i++) {
            String eventId = "evt-cb-" + i;
            assertThatThrownBy(() -> eventService.submitEvent(sampleRequest(eventId)))
                    .isInstanceOf(AccountServiceUnavailableException.class);
        }

        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // Once open, further calls are short-circuited but still surface a 503-mapped error.
        assertThatThrownBy(() -> eventService.submitEvent(sampleRequest("evt-cb-after-open")))
                .isInstanceOf(AccountServiceUnavailableException.class);
    }

    private EventRequest sampleRequest(String eventId) {
        return new EventRequest(
                eventId,
                "acct-cb",
                EventType.CREDIT,
                new BigDecimal("10.00"),
                "USD",
                Instant.parse("2026-05-15T14:00:00Z"),
                Map.of()
        );
    }
}
