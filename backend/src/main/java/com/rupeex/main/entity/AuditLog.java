package com.rupeex.main.entity;

import com.rupeex.main.enums.PaymentStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_payment_id", columnList = "payment_id"),
        @Index(name = "idx_audit_created_at", columnList = "created_at")
})
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_id")
    private Long paymentId;

    @Column(nullable = false)
    private String service;

    @Column(nullable = false)
    private String action;

    @Enumerated(EnumType.STRING)
    @Column(name = "before_state", length = 50)
    private PaymentStatus beforeState;

    @Enumerated(EnumType.STRING)
    @Column(name = "after_state", length = 50)
    private PaymentStatus afterState;

    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    private String reason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
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

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public PaymentStatus getBeforeState() {
        return beforeState;
    }

    public void setBeforeState(PaymentStatus beforeState) {
        this.beforeState = beforeState;
    }

    public PaymentStatus getAfterState() {
        return afterState;
    }

    public void setAfterState(PaymentStatus afterState) {
        this.afterState = afterState;
    }

    public Long getProcessingTimeMs() {
        return processingTimeMs;
    }

    public void setProcessingTimeMs(Long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
