package com.rupeex.main.service.impl;

import com.rupeex.main.exception.DuplicatePaymentException;
import com.rupeex.main.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("IdempotencyServiceImpl Tests")
class IdempotencyServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private IdempotencyServiceImpl idempotencyService;


    @Test
    @DisplayName("Should allow payment with unique idempotency key")
    void checkDuplicate_UniqueKey_Success() {
        String uniqueKey = UUID.randomUUID().toString();
        when(paymentRepository.existsByIdempotencyKey(uniqueKey)).thenReturn(false);
        assertThatNoException().isThrownBy(() -> idempotencyService.checkDuplicate(uniqueKey));
    }

    @Test
    @DisplayName("Should throw exception when idempotency key already exists")
    void checkDuplicate_DuplicateKey_ThrowsException() {
        String duplicateKey = UUID.randomUUID().toString();
        when(paymentRepository.existsByIdempotencyKey(duplicateKey)).thenReturn(true);
        assertThatThrownBy(() -> idempotencyService.checkDuplicate(duplicateKey))
                .isInstanceOf(DuplicatePaymentException.class)
                .hasMessage("Payment already exists");
    }

    @ParameterizedTest
    @ValueSource(strings = {"key-1", "key-2", "key-3"})
    @DisplayName("Should reject all duplicate keys")
    void checkDuplicate_VariousDuplicateFormats_ThrowsException(String duplicateKey) {
        when(paymentRepository.existsByIdempotencyKey(duplicateKey)).thenReturn(true);
        assertThatThrownBy(() -> idempotencyService.checkDuplicate(duplicateKey))
                .isInstanceOf(DuplicatePaymentException.class);
    }

    @Test
    @DisplayName("Should handle very long idempotency key")
    void checkDuplicate_VeryLongKey_Success() {
        String longKey = "K".repeat(500);
        when(paymentRepository.existsByIdempotencyKey(longKey)).thenReturn(false);
        assertThatNoException().isThrownBy(() -> idempotencyService.checkDuplicate(longKey));
    }

    @Test
    @DisplayName("Should treat keys as case-sensitive")
    void checkDuplicate_KeyCaseSensitivity() {
        String lowerKey = "payment-key-123";
        String upperKey = "PAYMENT-KEY-123";
        when(paymentRepository.existsByIdempotencyKey(lowerKey)).thenReturn(false);
        when(paymentRepository.existsByIdempotencyKey(upperKey)).thenReturn(false);
        assertThatNoException().isThrownBy(() -> idempotencyService.checkDuplicate(lowerKey));
        assertThatNoException().isThrownBy(() -> idempotencyService.checkDuplicate(upperKey));
    }

    @Test
    @DisplayName("Should call repository method once per check")
    void checkDuplicate_RepositoryCallCount_CalledOnce() {
        String key = UUID.randomUUID().toString();
        when(paymentRepository.existsByIdempotencyKey(key)).thenReturn(false);
        idempotencyService.checkDuplicate(key);
        verify(paymentRepository, times(1)).existsByIdempotencyKey(key);
    }
}

