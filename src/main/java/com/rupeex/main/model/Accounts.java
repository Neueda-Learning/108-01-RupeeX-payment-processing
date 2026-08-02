//package com.rupeex.main.model;
//
//import jakarta.persistence.*;
//import java.time.LocalDateTime;
//
//@Entity
//@Table(name = "accounts")
//public class Accounts {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @Column(nullable = false, unique = true, length = 100)
//    private String accountNumber;
//
//    @Column(nullable = false, length = 255)
//    private String accountHolder;
//
//    @Column(nullable = false, length = 50)
//    private String accountType;
//
//    @Column(nullable = false, length = 3)
//    private String currency;
//
//    @Column(length = 100)
//    private String bankName;
//
//    @Column(length = 20)
//    private String bankCode;
//
//    @Column(length = 50)
//    private String ifscCode;
//
//    @Column(length = 50)
//    private String swiftCode;
//
//    @Column(nullable = false, length = 50)
//    private String status;
//
//    @Column(length = 500)
//    private String metadata;
//
//    @Column(nullable = false, name = "created_at", updatable = false)
//    private LocalDateTime createdAt;
//
//    @Column(nullable = false, name = "updated_at")
//    private LocalDateTime updatedAt;
//
//    // Constructor
//    public Accounts() {
//    }
//
//    public Accounts(String accountNumber, String accountHolder, String accountType,
//                    String currency, String bankName) {
//        this.accountNumber = accountNumber;
//        this.accountHolder = accountHolder;
//        this.accountType = accountType;
//        this.currency = currency;
//        this.bankName = bankName;
//        this.status = "ACTIVE";
//        this.createdAt = LocalDateTime.now();
//        this.updatedAt = LocalDateTime.now();
//    }
//
//    // Getters and Setters
//    public Long getId() {
//        return id;
//    }
//
//    public void setId(Long id) {
//        this.id = id;
//    }
//
//    public String getAccountNumber() {
//        return accountNumber;
//    }
//
//    public void setAccountNumber(String accountNumber) {
//        this.accountNumber = accountNumber;
//    }
//
//    public String getAccountHolder() {
//        return accountHolder;
//    }
//
//    public void setAccountHolder(String accountHolder) {
//        this.accountHolder = accountHolder;
//    }
//
//    public String getAccountType() {
//        return accountType;
//    }
//
//    public void setAccountType(String accountType) {
//        this.accountType = accountType;
//    }
//
//    public String getCurrency() {
//        return currency;
//    }
//
//    public void setCurrency(String currency) {
//        this.currency = currency;
//    }
//
//    public String getBankName() {
//        return bankName;
//    }
//
//    public void setBankName(String bankName) {
//        this.bankName = bankName;
//    }
//
//    public String getBankCode() {
//        return bankCode;
//    }
//
//    public void setBankCode(String bankCode) {
//        this.bankCode = bankCode;
//    }
//
//    public String getIfscCode() {
//        return ifscCode;
//    }
//
//    public void setIfscCode(String ifscCode) {
//        this.ifscCode = ifscCode;
//    }
//
//    public String getSwiftCode() {
//        return swiftCode;
//    }
//
//    public void setSwiftCode(String swiftCode) {
//        this.swiftCode = swiftCode;
//    }
//
//    public String getStatus() {
//        return status;
//    }
//
//    public void setStatus(String status) {
//        this.status = status;
//        this.updatedAt = LocalDateTime.now();
//    }
//
//    public String getMetadata() {
//        return metadata;
//    }
//
//    public void setMetadata(String metadata) {
//        this.metadata = metadata;
//    }
//
//    public LocalDateTime getCreatedAt() {
//        return createdAt;
//    }
//
//    public void setCreatedAt(LocalDateTime createdAt) {
//        this.createdAt = createdAt;
//    }
//
//    public LocalDateTime getUpdatedAt() {
//        return updatedAt;
//    }
//
//    public void setUpdatedAt(LocalDateTime updatedAt) {
//        this.updatedAt = updatedAt;
//    }
//
//    @Override
//    public String toString() {
//        return "Accounts{" +
//                "id=" + id +
//                ", accountNumber='" + accountNumber + '\'' +
//                ", accountHolder='" + accountHolder + '\'' +
//                ", accountType='" + accountType + '\'' +
//                ", currency='" + currency + '\'' +
//                ", bankName='" + bankName + '\'' +
//                ", bankCode='" + bankCode + '\'' +
//                ", ifscCode='" + ifscCode + '\'' +
//                ", swiftCode='" + swiftCode + '\'' +
//                ", status='" + status + '\'' +
//                ", metadata='" + metadata + '\'' +
//                ", createdAt=" + createdAt +
//                ", updatedAt=" + updatedAt +
//                '}';
//    }
//}
//
