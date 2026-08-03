package com.rupeex.main.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "dead_letter_queue", indexes = {
        @Index(name = "idx_dlq_payment_id", columnList = "payment_id")
})
public class DeadLetterQueueEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_id", nullable = false)
    private Long paymentId;

    @Column(nullable = false)
    private String reason;

    @Column(name = "last_retry_count", nullable = false)
    private int lastRetryCount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public int getLastRetryCount() {
        return lastRetryCount;
    }

    public void setLastRetryCount(int lastRetryCount) {
        this.lastRetryCount = lastRetryCount;
    }
}
