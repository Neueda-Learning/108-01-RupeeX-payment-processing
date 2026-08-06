package com.rupeex.main.notification;

import com.rupeex.main.entity.Account;
import com.rupeex.main.entity.Payment;
import com.rupeex.main.notification.service.PaymentEmailResolverService;
import com.rupeex.main.repository.AccountsRepository;
import com.rupeex.main.repository.PaymentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentEmailResolverService Tests")
class PaymentEmailResolverServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private AccountsRepository accountsRepository;

    @InjectMocks
    private PaymentEmailResolverService resolver;

    @Test
    @DisplayName("Should resolve email from Payment.payerEmail when present")
    void resolvePayerEmail_PayerEmailOnPayment_ReturnsThat() {
        Payment payment = new Payment();
        payment.setPayerEmail("payer@example.com");
        payment.setSourceAccount("ACC-001");

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        String email = resolver.resolvePayerEmail(1L);

        assertThat(email).isEqualTo("payer@example.com");
        verifyNoInteractions(accountsRepository);
    }

    @Test
    @DisplayName("Should fall back to Account.email when payerEmail is null")
    void resolvePayerEmail_PayerEmailNull_FallsBackToAccountEmail() {
        Payment payment = new Payment();
        payment.setPayerEmail(null);
        payment.setSourceAccount("ACC-001");

        Account account = new Account();
        account.setEmail("account@example.com");

        when(paymentRepository.findById(2L)).thenReturn(Optional.of(payment));
        when(accountsRepository.findByAccountNumber("ACC-001")).thenReturn(Optional.of(account));

        String email = resolver.resolvePayerEmail(2L);

        assertThat(email).isEqualTo("account@example.com");
    }

    @Test
    @DisplayName("Should fall back to Account.email when payerEmail is blank")
    void resolvePayerEmail_PayerEmailBlank_FallsBackToAccountEmail() {
        Payment payment = new Payment();
        payment.setPayerEmail("   ");
        payment.setSourceAccount("ACC-001");

        Account account = new Account();
        account.setEmail("account@example.com");

        when(paymentRepository.findById(3L)).thenReturn(Optional.of(payment));
        when(accountsRepository.findByAccountNumber("ACC-001")).thenReturn(Optional.of(account));

        String email = resolver.resolvePayerEmail(3L);

        assertThat(email).isEqualTo("account@example.com");
    }

    @Test
    @DisplayName("Should return null when payment not found")
    void resolvePayerEmail_PaymentNotFound_ReturnsNull() {
        when(paymentRepository.findById(99L)).thenReturn(Optional.empty());

        String email = resolver.resolvePayerEmail(99L);

        assertThat(email).isNull();
    }

    @Test
    @DisplayName("Should return null when both payerEmail and account email are absent")
    void resolvePayerEmail_NoEmailAnywhere_ReturnsNull() {
        Payment payment = new Payment();
        payment.setPayerEmail(null);
        payment.setSourceAccount("ACC-001");

        Account account = new Account();
        account.setEmail(null);

        when(paymentRepository.findById(4L)).thenReturn(Optional.of(payment));
        when(accountsRepository.findByAccountNumber("ACC-001")).thenReturn(Optional.of(account));

        String email = resolver.resolvePayerEmail(4L);

        assertThat(email).isNull();
    }

    @Test
    @DisplayName("Should return null when account is not found in repository")
    void resolvePayerEmail_AccountNotFound_ReturnsNull() {
        Payment payment = new Payment();
        payment.setPayerEmail(null);
        payment.setSourceAccount("UNKNOWN-ACC");

        when(paymentRepository.findById(5L)).thenReturn(Optional.of(payment));
        when(accountsRepository.findByAccountNumber("UNKNOWN-ACC")).thenReturn(Optional.empty());

        String email = resolver.resolvePayerEmail(5L);

        assertThat(email).isNull();
    }
}
