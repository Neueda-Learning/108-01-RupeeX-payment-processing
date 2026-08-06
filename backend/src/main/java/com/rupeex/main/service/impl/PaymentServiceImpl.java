package com.rupeex.main.service.impl;

import com.rupeex.main.dto.PaymentRequest;
import com.rupeex.main.dto.PaymentResponse;
import com.rupeex.main.dto.VerificationDecisionRequest;
import com.rupeex.main.entity.Payment;
import com.rupeex.main.entity.PaymentVerification;
import com.rupeex.main.enums.PaymentStatus;
import com.rupeex.main.enums.VerificationStatus;
import com.rupeex.main.exception.InvalidPaymentException;
import com.rupeex.main.exception.PaymentNotFoundException;
import com.rupeex.main.repository.PaymentRepository;
import com.rupeex.main.repository.PaymentVerificationRepository;
import com.rupeex.main.service.IdempotencyService;
import com.rupeex.main.service.PaymentService;
import com.rupeex.main.service.PaymentValidationService;
import com.rupeex.main.service.VerificationNotificationService;
import com.rupeex.main.platform.service.TransactionLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PaymentServiceImpl
        implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentValidationService validationService;
    private final IdempotencyService idempotencyService;
    private final PaymentVerificationRepository paymentVerificationRepository;
    private final VerificationNotificationService verificationNotificationService;
    private final TransactionLogService transactionLogService;

    public PaymentServiceImpl(
            PaymentRepository paymentRepository,
            PaymentValidationService validationService,
            IdempotencyService idempotencyService,
            PaymentVerificationRepository paymentVerificationRepository,
            VerificationNotificationService verificationNotificationService,
            TransactionLogService transactionLogService) {

        this.paymentRepository = paymentRepository;
        this.validationService = validationService;
        this.idempotencyService = idempotencyService;
        this.paymentVerificationRepository = paymentVerificationRepository;
        this.verificationNotificationService = verificationNotificationService;
        this.transactionLogService = transactionLogService;
    }



    @Override
    @Transactional
    public PaymentResponse createPayment(
            PaymentRequest request){


        idempotencyService.checkDuplicate(
                request.getIdempotencyKey()
        );


        validationService.validate(request);


        Payment payment = new Payment();

        payment.setAmount(request.getAmount());
        payment.setCurrency(request.getCurrency());
        payment.setSourceAccount(request.getSourceAccount());
        payment.setDestinationAccount(request.getDestinationAccount());
        payment.setIdempotencyKey(request.getIdempotencyKey());
        payment.setPayerEmail(request.getCustomerEmail());
        payment.setPaymentReference(
                "PAY-" + UUID.randomUUID()
        );


        payment.setStatus(PaymentStatus.COMPLETED);


        Payment saved =
                paymentRepository.save(payment);

        transactionLogService.log(saved, "LegacyPaymentEngine", "Payment Created", null, PaymentStatus.COMPLETED, null);

        PaymentResponse response =
                mapToResponse(saved);

        response.setVerificationRequired(false);


        return response;

    }



    @Override
    public PaymentResponse getPaymentById(Long paymentId) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Payment not found: " + paymentId
                ));


        PaymentResponse response = mapToResponse(payment);


        paymentVerificationRepository.findByPaymentId(paymentId)
                .ifPresent(verification -> {
                    response.setVerificationRequired(
                            verification.getStatus() == VerificationStatus.PENDING
                    );
                    if (verification.getStatus() == VerificationStatus.PENDING) {
                        response.setVerificationToken(
                                verification.getVerificationToken()
                        );
                    }
                });


        return response;
    }



    @Override
    @Transactional
    public void updatePaymentStatus(Long paymentId, PaymentStatus status) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Payment not found: " + paymentId
                ));

        PaymentStatus previousStatus = payment.getStatus();
        payment.setStatus(status);
        paymentRepository.save(payment);
        transactionLogService.log(payment, "LegacyPaymentEngine", "Status Updated", previousStatus, status, null);

    }



    @Override
    @Transactional
    public PaymentResponse processVerificationDecision(
            Long paymentId,
            VerificationDecisionRequest request
    ) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Payment not found: " + paymentId
                ));


        PaymentVerification verification =
                paymentVerificationRepository
                        .findByPaymentIdAndVerificationTokenAndStatus(
                                paymentId,
                                request.getToken(),
                                VerificationStatus.PENDING
                        )
                        .orElseThrow(() -> new InvalidPaymentException(
                                "Invalid or expired verification token"
                        ));


        if(request.isApproved()){

            payment.setStatus(PaymentStatus.COMPLETED);
            verification.setStatus(VerificationStatus.APPROVED);

        } else {

            payment.markAsFailed(
                    "VERIFICATION_DECLINED",
                    "Customer declined payment verification"
            );
            payment.setStatus(PaymentStatus.DECLINED);
            verification.setStatus(VerificationStatus.DECLINED);
        }


        paymentRepository.save(payment);
        paymentVerificationRepository.save(verification);

        transactionLogService.log(payment, "LegacyPaymentEngine", "Verification Decision",
                PaymentStatus.CREATED, payment.getStatus(),
                request.isApproved() ? "Customer approved payment verification" : "Customer declined payment verification");


        PaymentResponse response = mapToResponse(payment);
        response.setVerificationRequired(false);

        return response;
    }




    private PaymentResponse mapToResponse(Payment payment) {

        PaymentResponse response =
                new PaymentResponse();

        response.setPaymentId(payment.getId());
        response.setPaymentReference(payment.getPaymentReference());
        response.setAmount(payment.getAmount());
        response.setCurrency(payment.getCurrency());
        response.setSourceAccount(payment.getSourceAccount());
        response.setDestinationAccount(payment.getDestinationAccount());
        response.setStatus(payment.getStatus());
        response.setErrorCode(payment.getErrorCode());
        response.setErrorMessage(payment.getErrorMessage());
        response.setCreatedAt(payment.getCreatedAt());
        response.setUpdatedAt(payment.getUpdatedAt());

        return response;
    }

}
