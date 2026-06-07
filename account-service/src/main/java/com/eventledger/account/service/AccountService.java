package com.eventledger.account.service;

import com.eventledger.account.config.AccountProperties;
import com.eventledger.account.domain.TransactionEntity;
import com.eventledger.account.dto.AccountDetailsResponse;
import com.eventledger.account.dto.ApplyTransactionRequest;
import com.eventledger.account.dto.BalanceResponse;
import com.eventledger.account.dto.TransactionResponse;
import com.eventledger.account.exception.AccountNotFoundException;
import com.eventledger.account.exception.InvalidTransactionException;
import com.eventledger.account.metrics.TransactionMetrics;
import com.eventledger.account.repository.TransactionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class AccountService {

    private static final String USD = "USD";

    private final TransactionRepository transactionRepository;
    private final ObjectMapper objectMapper;
    private final AccountProperties accountProperties;
    private final TransactionMetrics transactionMetrics;

    public AccountService(TransactionRepository transactionRepository,
                          ObjectMapper objectMapper,
                          AccountProperties accountProperties,
                          TransactionMetrics transactionMetrics) {
        this.transactionRepository = transactionRepository;
        this.objectMapper = objectMapper;
        this.accountProperties = accountProperties;
        this.transactionMetrics = transactionMetrics;
    }

    @Transactional
    public TransactionResponse applyTransaction(String accountId, ApplyTransactionRequest request) {
        validateRequest(accountId, request);

        return transactionRepository.findByEventId(request.eventId())
                .map(existing -> toResponse(existing, true))
                .orElseGet(() -> {
                    String metadataJson = serializeMetadata(request.metadata());
                    TransactionEntity saved = transactionRepository.save(new TransactionEntity(
                            request.eventId(),
                            accountId,
                            request.type(),
                            request.amount(),
                            request.currency(),
                            request.eventTimestamp(),
                            metadataJson
                    ));
                    transactionMetrics.incrementApplied();
                    return toResponse(saved, false);
                });
    }

    @Transactional(readOnly = true)
    public BalanceResponse getBalance(String accountId) {
        ensureAccountExists(accountId);
        BigDecimal balance = transactionRepository.computeBalance(accountId);
        return new BalanceResponse(accountId, balance, USD);
    }

    @Transactional(readOnly = true)
    public AccountDetailsResponse getAccountDetails(String accountId) {
        ensureAccountExists(accountId);
        BigDecimal balance = transactionRepository.computeBalance(accountId);
        List<TransactionResponse> recent = transactionRepository
                .findByAccountIdOrderByEventTimestampDesc(accountId,
                        PageRequest.of(0, accountProperties.getRecentTransactionLimit()))
                .stream()
                .sorted(Comparator.comparing(TransactionEntity::getEventTimestamp))
                .map(entity -> toResponse(entity, false))
                .toList();
        return new AccountDetailsResponse(accountId, balance, USD, recent);
    }

    private void ensureAccountExists(String accountId) {
        if (!transactionRepository.existsByAccountId(accountId)) {
            throw new AccountNotFoundException(accountId);
        }
    }

    private void validateRequest(String accountId, ApplyTransactionRequest request) {
        if (accountId == null || accountId.isBlank()) {
            throw new InvalidTransactionException("accountId", "accountId is required");
        }
        if (request.eventId() == null || request.eventId().isBlank()) {
            throw new InvalidTransactionException("eventId", "eventId is required");
        }
        if (request.type() == null) {
            throw new InvalidTransactionException("type", "type is required and must be CREDIT or DEBIT");
        }
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("amount",
                    "amount must be greater than 0, but was: " + request.amount());
        }
        if (!USD.equalsIgnoreCase(request.currency())) {
            throw new InvalidTransactionException("currency",
                    "currency must be USD, but was: " + request.currency());
        }
        if (request.eventTimestamp() == null) {
            throw new InvalidTransactionException("eventTimestamp", "eventTimestamp is required");
        }
    }

    private TransactionResponse toResponse(TransactionEntity entity, boolean duplicate) {
        return new TransactionResponse(
                entity.getEventId(),
                entity.getAccountId(),
                entity.getType(),
                entity.getAmount(),
                entity.getCurrency(),
                entity.getEventTimestamp(),
                deserializeMetadata(entity.getMetadataJson()),
                duplicate
        );
    }

    private String serializeMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            throw new InvalidTransactionException("metadata", "metadata must be a valid JSON object");
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
}
