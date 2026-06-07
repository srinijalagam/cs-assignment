package com.eventledger.gateway.integration;

import com.eventledger.gateway.domain.EventType;
import com.eventledger.gateway.dto.EventRequest;
import com.eventledger.gateway.dto.EventResponse;
import com.eventledger.gateway.service.EventService;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = "test.context=gateway-account-integration")
class GatewayAccountIntegrationTest {

    // Started in a static initializer so the port is available when @DynamicPropertySource resolves at context load.
    private static final WireMockServer WIRE_MOCK = new WireMockServer(0);

    static {
        WIRE_MOCK.start();
    }

    @Autowired
    private EventService eventService;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("gateway.account-service.base-url", () -> "http://localhost:" + WIRE_MOCK.port());
    }

    @AfterAll
    static void stopWireMock() {
        WIRE_MOCK.stop();
    }

    @BeforeEach
    void resetState() {
        circuitBreakerRegistry.circuitBreaker("accountService").reset();
        WIRE_MOCK.resetAll();
    }

    @Test
    void fullGatewayToAccountFlow() {
        WIRE_MOCK.stubFor(post(urlEqualTo("/accounts/acct-int/transactions"))
                .willReturn(aResponse().withStatus(201)));

        EventRequest request = new EventRequest(
                "evt-int-1",
                "acct-int",
                EventType.CREDIT,
                new BigDecimal("75.00"),
                "USD",
                Instant.parse("2026-05-15T14:00:00Z"),
                Map.of("source", "integration-test")
        );

        EventService.EventSubmissionResult result = eventService.submitEvent(request);

        assertThat(result.duplicate()).isFalse();
        EventResponse stored = eventService.getEvent("evt-int-1");
        assertThat(stored.amount()).isEqualByComparingTo(new BigDecimal("75.00"));

        WIRE_MOCK.verify(postRequestedFor(urlEqualTo("/accounts/acct-int/transactions")));
    }
}
