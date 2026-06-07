package com.eventledger.account.repository;

import com.eventledger.account.domain.TransactionEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {

    Optional<TransactionEntity> findByEventId(String eventId);

    boolean existsByAccountId(String accountId);

    List<TransactionEntity> findByAccountIdOrderByEventTimestampDesc(String accountId, Pageable pageable);

    @Query("""
            SELECT COALESCE(SUM(
                CASE WHEN t.type = com.eventledger.account.domain.TransactionType.CREDIT THEN t.amount
                     ELSE -t.amount END
            ), 0)
            FROM TransactionEntity t
            WHERE t.accountId = :accountId
            """)
    BigDecimal computeBalance(@Param("accountId") String accountId);
}
