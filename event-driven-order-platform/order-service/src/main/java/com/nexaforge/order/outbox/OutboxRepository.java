package com.nexaforge.order.outbox;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {

    @Query("SELECT o FROM OutboxEvent o WHERE o.sent = false ORDER BY o.createdAt ASC")
    List<OutboxEvent> findUnsentEvents(Pageable pageable);

    long countBySent(boolean sent);
}
