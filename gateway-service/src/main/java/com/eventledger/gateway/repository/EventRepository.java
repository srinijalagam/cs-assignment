package com.eventledger.gateway.repository;

import com.eventledger.gateway.domain.EventEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<EventEntity, Long> {

    Optional<EventEntity> findByEventId(String eventId);

    List<EventEntity> findByAccountIdOrderByEventTimestampDesc(String accountId, Pageable pageable);
}
