package com.rupeex.main.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_status_history")
public class PaymentStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "payment_id")
    private Long paymentId;

    @Column(nullable = false, length = 50)
    private String status;

    @Column(nullable = false, name = "changed_at")
    private LocalDateTime changedAt;

    @Column(length = 500)
    private String remarks;

    @Column(name = "changed_by", length = 100)
    private String changedBy;

    // Constructor
    public PaymentStatusHistory() {
    }

    public PaymentStatusHistory(Long paymentId, String status, String remarks) {
        this.paymentId = paymentId;
        this.status = status;
        this.remarks = remarks;
        this.changedAt = LocalDateTime.now();
    }

    public PaymentStatusHistory(Long paymentId, String status, String remarks, String changedBy) {
        this.paymentId = paymentId;
        this.status = status;
        this.remarks = remarks;
        this.changedBy = changedBy;
        this.changedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(LocalDateTime changedAt) {
        this.changedAt = changedAt;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(String changedBy) {
        this.changedBy = changedBy;
    }

    @Override
    public String toString() {
        return "PaymentStatusHistory{" +
                "id=" + id +
                ", paymentId=" + paymentId +
                ", status='" + status + '\'' +
                ", changedAt=" + changedAt +
                ", remarks='" + remarks + '\'' +
                ", changedBy='" + changedBy + '\'' +
                '}';
    }
}

