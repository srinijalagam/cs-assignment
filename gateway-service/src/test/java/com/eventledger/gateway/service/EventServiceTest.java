package com.eventledger.gateway.service;

import com.eventledger.gateway.client.AccountServiceClient;
import com.eventledger.gateway.config.GatewayProperties;
import com.eventledger.gateway.domain.EventType;
import com.eventledger.gateway.dto.EventRequest;
import com.eventledger.gateway.exception.AccountServiceUnavailableException;
import com.eventledger.gateway.metrics.EventMetrics;
import com.eventledger.gateway.repository.EventRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private AccountServiceClient accountServiceClient;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private GatewayProperties gatewayProperties;

    @Mock
    private EventMetrics eventMetrics;

    @InjectMocks
    private EventService eventService;

    @Test
    void doesNotStoreEventWhenAccountServiceFails() {
        EventRequest request = sampleRequest("evt-fail");
        when(eventRepository.findByEventId("evt-fail")).thenReturn(Optional.empty());
        doThrow(new AccountServiceUnavailableException("down"))
                .when(accountServiceClient).applyTransaction(eq("acct-1"), any());

        assertThatThrownBy(() -> eventService.submitEvent(request))
                .isInstanceOf(AccountServiceUnavailableException.class);

        verify(eventRepository, never()).save(any());
    }

    @Test
    void returnsExistingEventForDuplicateSubmission() {
        EventRequest request = sampleRequest("evt-dup");
        var entity = new com.eventledger.gateway.domain.EventEntity(
                "evt-dup", "acct-1", EventType.CREDIT, new BigDecimal("10.00"),
                "USD", Instant.parse("2026-05-15T14:00:00Z"), null, Instant.now()
        );
        when(eventRepository.findByEventId("evt-dup")).thenReturn(Optional.of(entity));

        var result = eventService.submitEvent(request);

        assertThat(result.duplicate()).isTrue();
        verify(accountServiceClient, never()).applyTransaction(any(), any());
    }

    private EventRequest sampleRequest(String eventId) {
        return new EventRequest(
                eventId,
                "acct-1",
                EventType.CREDIT,
                new BigDecimal("10.00"),
                "USD",
                Instant.parse("2026-05-15T14:00:00Z"),
                Map.of()
        );
    }
}
