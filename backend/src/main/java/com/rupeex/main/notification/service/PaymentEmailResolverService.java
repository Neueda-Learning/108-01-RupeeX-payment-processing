package com.rupeex.main.notification.service;

import com.rupeex.main.entity.Account;
import com.rupeex.main.entity.Payment;
import com.rupeex.main.repository.AccountsRepository;
import com.rupeex.main.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolves customer email address for payment notifications.
 * Uses multi-level fallback strategy:
 * 1. Payment.payerEmail (primary)
 * 2. Account.email lookup via sourceAccount (fallback)
 * 3. Return null if both unavailable
 * 
 * Phase 3: Email Lookup & Enrichment Service
 * Date: August 5, 2026
 */
@Service
public class PaymentEmailResolverService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentEmailResolverService.class);

    private final PaymentRepository paymentRepository;
    private final AccountsRepository accountsRepository;

    public PaymentEmailResolverService(PaymentRepository paymentRepository,
                                      AccountsRepository accountsRepository) {
        this.paymentRepository = paymentRepository;
        this.accountsRepository = accountsRepository;
    }

    /**
     * Resolves payer email for a payment using multi-strategy approach.
     * Priority order:
     * 1. Payment.payerEmail (if provided at creation)
     * 2. Account.email (via sourceAccount lookup)
     * 3. null (with warning log)
     *
     * @param paymentId The payment ID to resolve email for
     * @return The resolved email address, or null if not found
     */
    public String resolvePayerEmail(Long paymentId) {
        try {
            // Strategy 1: Try Payment.payerEmail first
            Payment payment = paymentRepository.findById(paymentId).orElse(null);
            if (payment == null) {
                logger.warn("Payment not found for ID: {}", paymentId);
                return null;
            }

            if (payment.getPayerEmail() != null && !payment.getPayerEmail().trim().isEmpty()) {
                logger.debug("Resolved payer email from Payment entity for paymentId={}: {}", 
                        paymentId, maskEmail(payment.getPayerEmail()));
                return payment.getPayerEmail();
            }

            // Strategy 2: Fallback to Account.email via sourceAccount
            String sourceAccountNumber = payment.getSourceAccount();
            if (sourceAccountNumber != null && !sourceAccountNumber.trim().isEmpty()) {
                Account sourceAccount = accountsRepository.findByAccountNumber(sourceAccountNumber).orElse(null);
                if (sourceAccount != null && sourceAccount.getEmail() != null && 
                    !sourceAccount.getEmail().trim().isEmpty()) {
                    logger.debug("Resolved payer email from Account entity for paymentId={}: {}", 
                            paymentId, maskEmail(sourceAccount.getEmail()));
                    return sourceAccount.getEmail();
                }
            }

            // Strategy 3: Email not found
            logger.warn("Could not resolve email for paymentId={}. Payment.payerEmail is null and " +
                    "sourceAccount {} has no email. Payment will not receive email notification.", 
                    paymentId, sourceAccountNumber);
            return null;

        } catch (Exception e) {
            logger.error("Error resolving payer email for paymentId={}", paymentId, e);
            return null;
        }
    }

    /**
     * Resolves recipient (destination account holder) email for a payment.
     * Used for credit notifications.
     * 
     * @param paymentId The payment ID to resolve recipient email for
     * @return The resolved recipient email address, or null if not found
     */
    public String resolveRecipientEmail(Long paymentId) {
        try {
            Payment payment = paymentRepository.findById(paymentId).orElse(null);
            if (payment == null) {
                logger.warn("Payment not found for ID: {}", paymentId);
                return null;
            }

            String destinationAccountNumber = payment.getDestinationAccount();
            if (destinationAccountNumber == null || destinationAccountNumber.trim().isEmpty()) {
                logger.warn("No destination account for paymentId={}. Cannot send credit notification.", paymentId);
                return null;
            }

            Account destAccount = accountsRepository.findByAccountNumber(destinationAccountNumber).orElse(null);
            if (destAccount != null && destAccount.getEmail() != null && 
                !destAccount.getEmail().trim().isEmpty()) {
                logger.debug("Resolved recipient email from Account for paymentId={}: {}", 
                        paymentId, maskEmail(destAccount.getEmail()));
                return destAccount.getEmail();
            }

            logger.warn("Could not resolve recipient email for paymentId={}. Destination account {} has no email.", 
                    paymentId, destinationAccountNumber);
            return null;

        } catch (Exception e) {
            logger.error("Error resolving recipient email for paymentId={}", paymentId, e);
            return null;
        }
    }

    /**
     * Resolves source account holder email for debit notifications.
     * Uses multi-level fallback strategy:
     * 1. Account.email lookup via sourceAccount (primary)
     * 2. Payment.payerEmail (fallback for API-created accounts)
     * 3. Return null if both unavailable
     * 
     * @param paymentId The payment ID to resolve source account holder email for
     * @return The resolved source account holder email, or null if not found
     */
    public String resolveSourceAccountHolderEmail(Long paymentId) {
        try {
            Payment payment = paymentRepository.findById(paymentId).orElse(null);
            if (payment == null) {
                logger.warn("Payment not found for ID: {}", paymentId);
                return null;
            }

            String sourceAccountNumber = payment.getSourceAccount();
            if (sourceAccountNumber == null || sourceAccountNumber.trim().isEmpty()) {
                logger.warn("No source account for paymentId={}. Cannot send debit notification.", paymentId);
                return null;
            }

            // Strategy 1: Try Account.email first
            Account sourceAccount = accountsRepository.findByAccountNumber(sourceAccountNumber).orElse(null);
            if (sourceAccount != null && sourceAccount.getEmail() != null && 
                !sourceAccount.getEmail().trim().isEmpty()) {
                logger.debug("Resolved source account holder email from Account for paymentId={}: {}", 
                        paymentId, maskEmail(sourceAccount.getEmail()));
                return sourceAccount.getEmail();
            }

            // Strategy 2: Fallback to Payment.payerEmail (for API-created accounts without stored email)
            if (payment.getPayerEmail() != null && !payment.getPayerEmail().trim().isEmpty()) {
                logger.debug("Resolved source account holder email from Payment.payerEmail for paymentId={}: {}", 
                        paymentId, maskEmail(payment.getPayerEmail()));
                return payment.getPayerEmail();
            }

            // Strategy 3: Email not found
            logger.warn("Could not resolve source account holder email for paymentId={}. Account {} has no email and Payment.payerEmail is null.", 
                    paymentId, sourceAccountNumber);
            return null;

        } catch (Exception e) {
            logger.error("Error resolving source account holder email for paymentId={}", paymentId, e);
            return null;
        }
    }

    /**
     * Resolves destination account holder email for credit notifications.
     * Alias for resolveRecipientEmail for clarity.
     * 
     * @param paymentId The payment ID
     * @return The resolved destination account holder email, or null if not found
     */
    public String resolveDestinationAccountHolderEmail(Long paymentId) {
        return resolveRecipientEmail(paymentId);
    }

    /**
     * Retrieves the account holder name for a given source account number.
     * 
     * @param sourceAccountNumber The source account number
     * @return The account holder name, or "Valued Customer" as fallback
     */
    public String resolveSourceAccountHolder(String sourceAccountNumber) {
        try {
            if (sourceAccountNumber == null || sourceAccountNumber.trim().isEmpty()) {
                return "Valued Customer";
            }
            
            Account account = accountsRepository.findByAccountNumber(sourceAccountNumber).orElse(null);
            return (account != null && account.getAccountHolder() != null) 
                    ? account.getAccountHolder() 
                    : "Valued Customer";
        } catch (Exception e) {
            logger.error("Error resolving source account holder for account: {}", sourceAccountNumber, e);
            return "Valued Customer";
        }
    }

    /**
     * Retrieves the account holder name for a given destination account number.
     * 
     * @param destinationAccountNumber The destination account number
     * @return The account holder name, or "Valued Customer" as fallback
     */
    public String resolveDestinationAccountHolder(String destinationAccountNumber) {
        try {
            if (destinationAccountNumber == null || destinationAccountNumber.trim().isEmpty()) {
                return "Valued Customer";
            }
            
            Account account = accountsRepository.findByAccountNumber(destinationAccountNumber).orElse(null);
            return (account != null && account.getAccountHolder() != null) 
                    ? account.getAccountHolder() 
                    : "Valued Customer";
        } catch (Exception e) {
            logger.error("Error resolving destination account holder for account: {}", destinationAccountNumber, e);
            return "Valued Customer";
        }
    }

    /**
     * Masks email for secure logging
     */
    private String maskEmail(String email) {
        if (email == null || email.length() < 3) {
            return "***";
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return "***";
        }
        return email.substring(0, 1) + "***" + email.substring(atIndex);
    }
}

