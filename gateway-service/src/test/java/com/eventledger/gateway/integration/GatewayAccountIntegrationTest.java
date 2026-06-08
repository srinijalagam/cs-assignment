package com.eventledger.gateway.integration;

import com.eventledger.gateway.client.AccountServiceClient;
import com.eventledger.gateway.domain.EventType;
import com.eventledger.gateway.dto.EventRequest;
import com.eventledger.gateway.dto.EventResponse;
import com.eventledger.gateway.exception.DownstreamClientException;
import com.eventledger.gateway.service.EventService;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
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
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    private AccountServiceClient accountServiceClient;

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

    private EventRequest orderRequest(String eventId, String timestamp) {
        return new EventRequest(
                eventId,
                "acct-order",
                EventType.CREDIT,
                new BigDecimal("1.00"),
                "USD",
                Instant.parse(timestamp),
                Map.of()
        );
    }

    @Test
    void propagatesTraceIdToAccountService() {
        WIRE_MOCK.stubFor(post(urlEqualTo("/accounts/acct-trace/transactions"))
                .willReturn(aResponse().withStatus(201)));

        eventService.submitEvent(new EventRequest(
                "evt-trace-1",
                "acct-trace",
                EventType.CREDIT,
                new BigDecimal("5.00"),
                "USD",
                Instant.parse("2026-05-15T14:00:00Z"),
                Map.of()
        ));

        WIRE_MOCK.verify(postRequestedFor(urlEqualTo("/accounts/acct-trace/transactions"))
                .withHeader("X-Trace-Id", matching(".+")));
    }

    @Test
    void listsEventsChronologicallyRegardlessOfArrivalOrder() {
        WIRE_MOCK.stubFor(post(urlEqualTo("/accounts/acct-order/transactions"))
                .willReturn(aResponse().withStatus(201)));

        // Submit out of chronological order: middle, earliest, latest.
        eventService.submitEvent(orderRequest("evt-mid", "2026-05-15T15:00:00Z"));
        eventService.submitEvent(orderRequest("evt-early", "2026-05-15T14:00:00Z"));
        eventService.submitEvent(orderRequest("evt-late", "2026-05-15T16:00:00Z"));

        List<EventResponse> events = eventService.listEventsByAccount("acct-order");

        assertThat(events).extracting(EventResponse::eventId)
                .containsExactly("evt-early", "evt-mid", "evt-late");
        assertThat(events).extracting(EventResponse::eventTimestamp)
                .isSorted();
    }

    @Test
    void downstream404SurfacesAsDownstreamClientExceptionNotUnavailable() {
        WIRE_MOCK.stubFor(get(urlEqualTo("/accounts/acct-missing/balance"))
                .willReturn(aResponse().withStatus(404)));

        assertThatThrownBy(() -> accountServiceClient.getBalance("acct-missing"))
                .isInstanceOf(DownstreamClientException.class)
                .extracting(ex -> ((DownstreamClientException) ex).getStatus())
                .isEqualTo(404);

        // A definitive 4xx must not trip the breaker.
        assertThat(circuitBreakerRegistry.circuitBreaker("accountService").getState())
                .isEqualTo(CircuitBreaker.State.CLOSED);
    }
}
