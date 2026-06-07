package com.eventledger.gateway.service;

import com.eventledger.gateway.client.AccountServiceClient;
import com.eventledger.gateway.client.AccountTransactionRequest;
import com.eventledger.gateway.config.GatewayProperties;
import com.eventledger.gateway.domain.EventEntity;
import com.eventledger.gateway.dto.EventRequest;
import com.eventledger.gateway.dto.EventResponse;
import com.eventledger.gateway.exception.EventNotFoundException;
import com.eventledger.gateway.exception.InvalidEventException;
import com.eventledger.gateway.metrics.EventMetrics;
import com.eventledger.gateway.repository.EventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class EventService {

    private static final String USD = "USD";

    private final EventRepository eventRepository;
    private final AccountServiceClient accountServiceClient;
    private final ObjectMapper objectMapper;
    private final GatewayProperties gatewayProperties;
    private final EventMetrics eventMetrics;

    public EventService(EventRepository eventRepository,
                        AccountServiceClient accountServiceClient,
                        ObjectMapper objectMapper,
                        GatewayProperties gatewayProperties,
                        EventMetrics eventMetrics) {
        this.eventRepository = eventRepository;
        this.accountServiceClient = accountServiceClient;
        this.objectMapper = objectMapper;
        this.gatewayProperties = gatewayProperties;
        this.eventMetrics = eventMetrics;
    }

    @Transactional
    public EventSubmissionResult submitEvent(EventRequest request) {
        validateEvent(request);

        return eventRepository.findByEventId(request.eventId())
                .map(existing -> new EventSubmissionResult(toResponse(existing), true))
                .orElseGet(() -> {
                    accountServiceClient.applyTransaction(
                            request.accountId(),
                            AccountTransactionRequest.from(request)
                    );

                    EventEntity saved = eventRepository.save(new EventEntity(
                            request.eventId(),
                            request.accountId(),
                            request.type(),
                            request.amount(),
                            request.currency(),
                            request.eventTimestamp(),
                            serializeMetadata(request.metadata()),
                            Instant.now()
                    ));
                    eventMetrics.incrementSubmitted();
                    return new EventSubmissionResult(toResponse(saved), false);
                });
    }

    @Transactional(readOnly = true)
    public EventResponse getEvent(String eventId) {
        return eventRepository.findByEventId(eventId)
                .map(this::toResponse)
                .orElseThrow(() -> new EventNotFoundException(eventId));
    }

    @Transactional(readOnly = true)
    public List<EventResponse> listEventsByAccount(String accountId) {
        return eventRepository
                .findByAccountIdOrderByEventTimestampDesc(accountId,
                        PageRequest.of(0, gatewayProperties.getRecentEventLimit()))
                .stream()
                .sorted(Comparator.comparing(EventEntity::getEventTimestamp))
                .map(this::toResponse)
                .toList();
    }

    private void validateEvent(EventRequest request) {
        if (request.eventId() == null || request.eventId().isBlank()) {
            throw new InvalidEventException("eventId", "eventId is required");
        }
        if (request.accountId() == null || request.accountId().isBlank()) {
            throw new InvalidEventException("accountId", "accountId is required");
        }
        if (request.type() == null) {
            throw new InvalidEventException("type", "type is required and must be CREDIT or DEBIT");
        }
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidEventException("amount",
                    "amount must be greater than 0, but was: " + request.amount());
        }
        if (!USD.equalsIgnoreCase(request.currency())) {
            throw new InvalidEventException("currency",
                    "currency must be USD, but was: " + request.currency());
        }
        if (request.eventTimestamp() == null) {
            throw new InvalidEventException("eventTimestamp", "eventTimestamp is required");
        }
    }

    private EventResponse toResponse(EventEntity entity) {
        return new EventResponse(
                entity.getEventId(),
                entity.getAccountId(),
                entity.getType(),
                entity.getAmount(),
                entity.getCurrency(),
                entity.getEventTimestamp(),
                deserializeMetadata(entity.getMetadataJson()),
                entity.getCreatedAt()
        );
    }

    private String serializeMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            throw new InvalidEventException("metadata", "metadata must be a valid JSON object");
        }
    }

    private Map<String, Object> deserializeMetadata(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(metadataJson, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }

    public record EventSubmissionResult(EventResponse event, boolean duplicate) {
    }
}
