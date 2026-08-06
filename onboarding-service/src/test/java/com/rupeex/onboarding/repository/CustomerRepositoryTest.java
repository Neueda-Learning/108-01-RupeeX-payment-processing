package com.rupeex.onboarding.repository;

import com.rupeex.onboarding.entity.Consent;
import com.rupeex.onboarding.entity.Customer;
import com.rupeex.onboarding.enums.OnboardingStatus;
import com.rupeex.onboarding.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("CustomerRepository Tests")
class CustomerRepositoryTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ConsentRepository consentRepository;

    private Customer savedCustomer;

    @BeforeEach
    void setUp() {
        consentRepository.deleteAll();
        customerRepository.deleteAll();
        savedCustomer = customerRepository.save(buildCustomer("alice@example.com", "+919876543210", "RUPX000001"));
    }

    // ===================== Helper Methods =====================

    private Customer buildCustomer(String email, String phone, String accountNumber) {
        Customer c = new Customer();
        c.setFullName("Alice Kumar");
        c.setEmail(email);
        c.setPhone(phone);
        c.setDob(LocalDate.of(1990, 5, 15));
        c.setAccountType("SAVINGS");
        c.setCurrency("INR");
        c.setCountryCode("IN");
        c.setRole(UserRole.MEMBER);
        c.setStatus(OnboardingStatus.DRAFT);
        c.setAccountNumber(accountNumber);
        return c;
    }

    // ===================== existsByEmail =====================

    @Test
    @DisplayName("Should return true when email exists")
    void existsByEmail_ExistingEmail_ReturnsTrue() {
        assertThat(customerRepository.existsByEmail("alice@example.com")).isTrue();
    }

    @Test
    @DisplayName("Should return false when email does not exist")
    void existsByEmail_NonExistentEmail_ReturnsFalse() {
        assertThat(customerRepository.existsByEmail("nobody@example.com")).isFalse();
    }

    // ===================== existsByPhone =====================

    @Test
    @DisplayName("Should return true when phone exists")
    void existsByPhone_ExistingPhone_ReturnsTrue() {
        assertThat(customerRepository.existsByPhone("+919876543210")).isTrue();
    }

    @Test
    @DisplayName("Should return false when phone does not exist")
    void existsByPhone_NonExistentPhone_ReturnsFalse() {
        assertThat(customerRepository.existsByPhone("+919999999999")).isFalse();
    }

    // ===================== existsByAccountNumber =====================

    @Test
    @DisplayName("Should return true when account number exists")
    void existsByAccountNumber_ExistingAccountNumber_ReturnsTrue() {
        assertThat(customerRepository.existsByAccountNumber("RUPX000001")).isTrue();
    }

    @Test
    @DisplayName("Should return false when account number does not exist")
    void existsByAccountNumber_NonExistentAccountNumber_ReturnsFalse() {
        assertThat(customerRepository.existsByAccountNumber("RUPX999999")).isFalse();
    }

    // ===================== findByEmail =====================

    @Test
    @DisplayName("Should find customer by email")
    void findByEmail_ExistingEmail_ReturnsCustomer() {
        Optional<Customer> result = customerRepository.findByEmail("alice@example.com");
        assertThat(result).isPresent();
        assertThat(result.get().getFullName()).isEqualTo("Alice Kumar");
    }

    @Test
    @DisplayName("Should return empty when email not found")
    void findByEmail_UnknownEmail_ReturnsEmpty() {
        Optional<Customer> result = customerRepository.findByEmail("unknown@example.com");
        assertThat(result).isEmpty();
    }

    // ===================== findById =====================

    @Test
    @DisplayName("Should find customer by id")
    void findById_ExistingId_ReturnsCustomer() {
        Optional<Customer> result = customerRepository.findById(savedCustomer.getId());
        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    @DisplayName("Should return empty for non-existent id")
    void findById_UnknownId_ReturnsEmpty() {
        Optional<Customer> result = customerRepository.findById(UUID.randomUUID());
        assertThat(result).isEmpty();
    }

    // ===================== findAll =====================

    @Test
    @DisplayName("Should return all customers")
    void findAll_ReturnsAllCustomers() {
        customerRepository.save(buildCustomer("bob@example.com", "+911234567890", "RUPX000002"));
        List<Customer> all = customerRepository.findAll();
        assertThat(all).hasSize(2);
    }

    // ===================== save & timestamps =====================

    @Test
    @DisplayName("Should set createdAt and updatedAt on persist")
    void save_SetsTimestamps() {
        assertThat(savedCustomer.getCreatedAt()).isNotNull();
        assertThat(savedCustomer.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should set default status to DRAFT on persist")
    void save_DefaultStatus_IsDraft() {
        Customer c = buildCustomer("draft@example.com", "+910000000001", "RUPX000010");
        c.setStatus(null); // ensure @PrePersist kicks in
        Customer saved = customerRepository.save(c);
        assertThat(saved.getStatus()).isEqualTo(OnboardingStatus.DRAFT);
    }

    // ===================== ConsentRepository =====================

    @Test
    @DisplayName("Should return false when no accepted consent exists for customer")
    void existsByCustomer_IdAndAcceptedTrue_NoConsent_ReturnsFalse() {
        assertThat(consentRepository.existsByCustomer_IdAndAcceptedTrue(savedCustomer.getId())).isFalse();
    }

    @Test
    @DisplayName("Should return false when only declined consents exist for customer")
    void existsByCustomer_IdAndAcceptedTrue_OnlyDeclined_ReturnsFalse() {
        Consent declined = new Consent();
        declined.setCustomer(savedCustomer);
        declined.setConsentType("MARKETING");
        declined.setConsentVersion("1.0");
        declined.setAccepted(false);
        consentRepository.save(declined);

        assertThat(consentRepository.existsByCustomer_IdAndAcceptedTrue(savedCustomer.getId())).isFalse();
    }

    @Test
    @DisplayName("Should return true when at least one accepted consent exists for customer")
    void existsByCustomer_IdAndAcceptedTrue_WithAcceptedConsent_ReturnsTrue() {
        Consent accepted = new Consent();
        accepted.setCustomer(savedCustomer);
        accepted.setConsentType("TERMS_AND_CONDITIONS");
        accepted.setConsentVersion("1.0");
        accepted.setAccepted(true);
        consentRepository.save(accepted);

        assertThat(consentRepository.existsByCustomer_IdAndAcceptedTrue(savedCustomer.getId())).isTrue();
    }

    @Test
    @DisplayName("Should return false for another customer's consents")
    void existsByCustomer_IdAndAcceptedTrue_OtherCustomer_ReturnsFalse() {
        // Add accepted consent for savedCustomer
        Consent accepted = new Consent();
        accepted.setCustomer(savedCustomer);
        accepted.setConsentType("TERMS");
        accepted.setConsentVersion("1.0");
        accepted.setAccepted(true);
        consentRepository.save(accepted);

        // A different customer should return false
        Customer other = customerRepository.save(buildCustomer("other@example.com", "+910000000099", "RUPX000099"));
        assertThat(consentRepository.existsByCustomer_IdAndAcceptedTrue(other.getId())).isFalse();
    }
}
