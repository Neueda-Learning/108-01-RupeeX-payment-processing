package com.rupeex.onboarding.service.impl;

import com.rupeex.onboarding.dto.ConsentRequest;
import com.rupeex.onboarding.dto.CreateCustomerRequest;
import com.rupeex.onboarding.dto.CustomerResponse;
import com.rupeex.onboarding.dto.CustomerStatusResponse;
import com.rupeex.onboarding.dto.CustomerSummary;
import com.rupeex.onboarding.dto.RejectRequest;
import com.rupeex.onboarding.entity.Consent;
import com.rupeex.onboarding.entity.Customer;
import com.rupeex.onboarding.enums.OnboardingStatus;
import com.rupeex.onboarding.enums.UserRole;
import com.rupeex.onboarding.exception.CustomerAlreadyExistsException;
import com.rupeex.onboarding.exception.CustomerNotFoundException;
import com.rupeex.onboarding.exception.InvalidStatusTransitionException;
import com.rupeex.onboarding.repository.ConsentRepository;
import com.rupeex.onboarding.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerOnboardingServiceImpl Tests")
class CustomerOnboardingServiceImplTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ConsentRepository consentRepository;

    @Mock
    private RestTemplate restTemplate;

    @Captor
    private ArgumentCaptor<Customer> customerCaptor;

    @Captor
    private ArgumentCaptor<Consent> consentCaptor;

    private CustomerOnboardingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CustomerOnboardingServiceImpl(customerRepository, consentRepository, restTemplate);
        ReflectionTestUtils.setField(service, "paymentServiceUrl", "http://localhost:8080");
    }

    // ===================== Helper Methods =====================

    private CreateCustomerRequest buildValidRequest() {
        CreateCustomerRequest req = new CreateCustomerRequest();
        req.setFullName("Alice Kumar");
        req.setEmail("alice@example.com");
        req.setPhone("+919876543210");
        req.setDob(LocalDate.of(1990, 5, 15));
        req.setAccountType("SAVINGS");
        req.setCurrency("INR");
        req.setCountryCode("IN");
        req.setRole(UserRole.MEMBER);
        return req;
    }

    private Customer buildCustomer(OnboardingStatus status) {
        Customer c = new Customer();
        ReflectionTestUtils.setField(c, "id", UUID.randomUUID());
        c.setFullName("Alice Kumar");
        c.setEmail("alice@example.com");
        c.setPhone("+919876543210");
        c.setDob(LocalDate.of(1990, 5, 15));
        c.setStatus(status);
        c.setAccountNumber("RUPX001234");
        c.setAccountType("SAVINGS");
        c.setCurrency("INR");
        c.setCountryCode("IN");
        c.setRole(UserRole.MEMBER);
        ReflectionTestUtils.setField(c, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(c, "updatedAt", LocalDateTime.now());
        return c;
    }

    // ===================== createCustomer =====================

    @Nested
    @DisplayName("createCustomer")
    class CreateCustomerTests {

        @Test
        @DisplayName("Should create customer successfully with valid request")
        void createCustomer_ValidRequest_ReturnsResponse() {
            CreateCustomerRequest req = buildValidRequest();
            Customer saved = buildCustomer(OnboardingStatus.DRAFT);

            when(customerRepository.existsByEmail(req.getEmail())).thenReturn(false);
            when(customerRepository.existsByPhone(req.getPhone())).thenReturn(false);
            when(customerRepository.existsByAccountNumber(anyString())).thenReturn(false);
            when(customerRepository.save(any(Customer.class))).thenReturn(saved);

            CustomerResponse response = service.createCustomer(req);

            assertThat(response).isNotNull();
            assertThat(response.getFullName()).isEqualTo("Alice Kumar");
            assertThat(response.getEmail()).isEqualTo("alice@example.com");
            assertThat(response.getStatus()).isEqualTo(OnboardingStatus.DRAFT);

            verify(customerRepository).save(customerCaptor.capture());
            Customer toSave = customerCaptor.getValue();
            assertThat(toSave.getStatus()).isEqualTo(OnboardingStatus.DRAFT);
            assertThat(toSave.getRole()).isEqualTo(UserRole.MEMBER);
            assertThat(toSave.getAccountNumber()).matches("RUPX\\d{6}");
        }

        @Test
        @DisplayName("Should default role to MEMBER when role is null in request")
        void createCustomer_NullRole_DefaultsMemberRole() {
            CreateCustomerRequest req = buildValidRequest();
            req.setRole(null);
            Customer saved = buildCustomer(OnboardingStatus.DRAFT);

            when(customerRepository.existsByEmail(req.getEmail())).thenReturn(false);
            when(customerRepository.existsByPhone(req.getPhone())).thenReturn(false);
            when(customerRepository.existsByAccountNumber(anyString())).thenReturn(false);
            when(customerRepository.save(any(Customer.class))).thenReturn(saved);

            service.createCustomer(req);

            verify(customerRepository).save(customerCaptor.capture());
            assertThat(customerCaptor.getValue().getRole()).isEqualTo(UserRole.MEMBER);
        }

        @Test
        @DisplayName("Should throw CustomerAlreadyExistsException when email is duplicate")
        void createCustomer_DuplicateEmail_ThrowsException() {
            CreateCustomerRequest req = buildValidRequest();
            when(customerRepository.existsByEmail(req.getEmail())).thenReturn(true);

            assertThatThrownBy(() -> service.createCustomer(req))
                    .isInstanceOf(CustomerAlreadyExistsException.class)
                    .hasMessageContaining("alice@example.com");

            verify(customerRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw CustomerAlreadyExistsException when phone is duplicate")
        void createCustomer_DuplicatePhone_ThrowsException() {
            CreateCustomerRequest req = buildValidRequest();
            when(customerRepository.existsByEmail(req.getEmail())).thenReturn(false);
            when(customerRepository.existsByPhone(req.getPhone())).thenReturn(true);

            assertThatThrownBy(() -> service.createCustomer(req))
                    .isInstanceOf(CustomerAlreadyExistsException.class)
                    .hasMessageContaining(req.getPhone());

            verify(customerRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should retry account number generation when collision occurs")
        void createCustomer_AccountNumberCollision_RetriesUntilUnique() {
            CreateCustomerRequest req = buildValidRequest();
            Customer saved = buildCustomer(OnboardingStatus.DRAFT);

            when(customerRepository.existsByEmail(req.getEmail())).thenReturn(false);
            when(customerRepository.existsByPhone(req.getPhone())).thenReturn(false);
            // First call: collides; second call: unique
            when(customerRepository.existsByAccountNumber(anyString()))
                    .thenReturn(true)
                    .thenReturn(false);
            when(customerRepository.save(any(Customer.class))).thenReturn(saved);

            service.createCustomer(req);

            // existsByAccountNumber should have been called at least twice
            verify(customerRepository, times(2)).existsByAccountNumber(anyString());
        }
    }

    // ===================== getCustomer =====================

    @Nested
    @DisplayName("getCustomer")
    class GetCustomerTests {

        @Test
        @DisplayName("Should return customer response for valid id")
        void getCustomer_ValidId_ReturnsResponse() {
            Customer customer = buildCustomer(OnboardingStatus.DRAFT);
            UUID id = customer.getId();
            when(customerRepository.findById(id)).thenReturn(Optional.of(customer));

            CustomerResponse response = service.getCustomer(id);

            assertThat(response.getCustomerId()).isEqualTo(id);
            assertThat(response.getEmail()).isEqualTo("alice@example.com");
        }

        @Test
        @DisplayName("Should throw CustomerNotFoundException for unknown id")
        void getCustomer_UnknownId_ThrowsException() {
            UUID id = UUID.randomUUID();
            when(customerRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getCustomer(id))
                    .isInstanceOf(CustomerNotFoundException.class)
                    .hasMessageContaining(id.toString());
        }
    }

    // ===================== getStatus =====================

    @Nested
    @DisplayName("getStatus")
    class GetStatusTests {

        @Test
        @DisplayName("Should return eligible=true when status is APPROVED")
        void getStatus_ApprovedCustomer_EligibleForPayments() {
            Customer customer = buildCustomer(OnboardingStatus.APPROVED);
            when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));

            CustomerStatusResponse status = service.getStatus(customer.getId());

            assertThat(status.getStatus()).isEqualTo(OnboardingStatus.APPROVED);
            assertThat(status.isEligibleForPayments()).isTrue();
        }

        @Test
        @DisplayName("Should return eligible=false when status is DRAFT")
        void getStatus_DraftCustomer_NotEligibleForPayments() {
            Customer customer = buildCustomer(OnboardingStatus.DRAFT);
            when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));

            CustomerStatusResponse status = service.getStatus(customer.getId());

            assertThat(status.getStatus()).isEqualTo(OnboardingStatus.DRAFT);
            assertThat(status.isEligibleForPayments()).isFalse();
        }

        @Test
        @DisplayName("Should return eligible=false when status is PENDING_REVIEW")
        void getStatus_PendingReview_NotEligibleForPayments() {
            Customer customer = buildCustomer(OnboardingStatus.PENDING_REVIEW);
            when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));

            CustomerStatusResponse status = service.getStatus(customer.getId());

            assertThat(status.isEligibleForPayments()).isFalse();
        }

        @Test
        @DisplayName("Should throw CustomerNotFoundException for unknown id")
        void getStatus_UnknownId_ThrowsException() {
            UUID id = UUID.randomUUID();
            when(customerRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getStatus(id))
                    .isInstanceOf(CustomerNotFoundException.class);
        }
    }

    // ===================== listCustomers =====================

    @Nested
    @DisplayName("listCustomers")
    class ListCustomersTests {

        @Test
        @DisplayName("Should return list of customer summaries")
        void listCustomers_ReturnsAll() {
            Customer c1 = buildCustomer(OnboardingStatus.DRAFT);
            Customer c2 = buildCustomer(OnboardingStatus.APPROVED);
            when(customerRepository.findAll()).thenReturn(List.of(c1, c2));

            List<CustomerSummary> summaries = service.listCustomers();

            assertThat(summaries).hasSize(2);
            assertThat(summaries).extracting(CustomerSummary::getEmail)
                    .containsExactlyInAnyOrder("alice@example.com", "alice@example.com");
        }

        @Test
        @DisplayName("Should return empty list when no customers exist")
        void listCustomers_Empty_ReturnsEmptyList() {
            when(customerRepository.findAll()).thenReturn(List.of());

            List<CustomerSummary> summaries = service.listCustomers();

            assertThat(summaries).isEmpty();
        }
    }

    // ===================== addConsent =====================

    @Nested
    @DisplayName("addConsent")
    class AddConsentTests {

        @Test
        @DisplayName("Should save consent for existing customer")
        void addConsent_ValidCustomer_SavesConsent() {
            Customer customer = buildCustomer(OnboardingStatus.DRAFT);
            ConsentRequest req = new ConsentRequest();
            req.setConsentType("TERMS_AND_CONDITIONS");
            req.setVersion("1.0");
            req.setAccepted(true);

            when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));

            service.addConsent(customer.getId(), req);

            verify(consentRepository).save(consentCaptor.capture());
            Consent saved = consentCaptor.getValue();
            assertThat(saved.getConsentType()).isEqualTo("TERMS_AND_CONDITIONS");
            assertThat(saved.getConsentVersion()).isEqualTo("1.0");
            assertThat(saved.isAccepted()).isTrue();
            assertThat(saved.getCustomer()).isEqualTo(customer);
        }

        @Test
        @DisplayName("Should save declined consent (accepted=false)")
        void addConsent_DeclinedConsent_SavesWithFalse() {
            Customer customer = buildCustomer(OnboardingStatus.DRAFT);
            ConsentRequest req = new ConsentRequest();
            req.setConsentType("MARKETING");
            req.setVersion("2.0");
            req.setAccepted(false);

            when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));

            service.addConsent(customer.getId(), req);

            verify(consentRepository).save(consentCaptor.capture());
            assertThat(consentCaptor.getValue().isAccepted()).isFalse();
        }

        @Test
        @DisplayName("Should throw CustomerNotFoundException when customer not found")
        void addConsent_UnknownCustomer_ThrowsException() {
            UUID id = UUID.randomUUID();
            when(customerRepository.findById(id)).thenReturn(Optional.empty());

            ConsentRequest req = new ConsentRequest();
            req.setConsentType("TERMS");
            req.setVersion("1.0");

            assertThatThrownBy(() -> service.addConsent(id, req))
                    .isInstanceOf(CustomerNotFoundException.class);

            verify(consentRepository, never()).save(any());
        }
    }

    // ===================== submitForReview =====================

    @Nested
    @DisplayName("submitForReview")
    class SubmitForReviewTests {

        @Test
        @DisplayName("Should transition DRAFT customer to PENDING_REVIEW when consent exists")
        void submitForReview_DraftWithConsent_TransitionsToPendingReview() {
            Customer customer = buildCustomer(OnboardingStatus.DRAFT);
            UUID id = customer.getId();

            when(customerRepository.findById(id)).thenReturn(Optional.of(customer));
            when(consentRepository.existsByCustomer_IdAndAcceptedTrue(id)).thenReturn(true);
            when(customerRepository.save(any(Customer.class))).thenReturn(customer);

            service.submitForReview(id);

            verify(customerRepository).save(customerCaptor.capture());
            assertThat(customerCaptor.getValue().getStatus()).isEqualTo(OnboardingStatus.PENDING_REVIEW);
        }

        @Test
        @DisplayName("Should throw InvalidStatusTransitionException when customer not in DRAFT status")
        void submitForReview_NotDraft_ThrowsInvalidTransition() {
            Customer customer = buildCustomer(OnboardingStatus.PENDING_REVIEW);
            when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));

            assertThatThrownBy(() -> service.submitForReview(customer.getId()))
                    .isInstanceOf(InvalidStatusTransitionException.class)
                    .hasMessageContaining("DRAFT");

            verify(customerRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw InvalidStatusTransitionException when no accepted consent exists")
        void submitForReview_NoAcceptedConsent_ThrowsException() {
            Customer customer = buildCustomer(OnboardingStatus.DRAFT);
            UUID id = customer.getId();

            when(customerRepository.findById(id)).thenReturn(Optional.of(customer));
            when(consentRepository.existsByCustomer_IdAndAcceptedTrue(id)).thenReturn(false);

            assertThatThrownBy(() -> service.submitForReview(id))
                    .isInstanceOf(InvalidStatusTransitionException.class)
                    .hasMessageContaining("consent");

            verify(customerRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw CustomerNotFoundException for unknown id")
        void submitForReview_UnknownId_ThrowsException() {
            UUID id = UUID.randomUUID();
            when(customerRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.submitForReview(id))
                    .isInstanceOf(CustomerNotFoundException.class);
        }
    }

    // ===================== approve =====================

    @Nested
    @DisplayName("approve")
    class ApproveTests {

        @Test
        @DisplayName("Should approve PENDING_REVIEW customer and call payment service")
        void approve_PendingReviewCustomer_ApprovesAndCreatesAccount() {
            Customer customer = buildCustomer(OnboardingStatus.PENDING_REVIEW);
            UUID id = customer.getId();

            when(customerRepository.findById(id)).thenReturn(Optional.of(customer));
            when(customerRepository.save(any(Customer.class))).thenReturn(customer);
            when(restTemplate.postForObject(anyString(), any(), eq(java.util.Map.class))).thenReturn(null);

            service.approve(id);

            verify(customerRepository).save(customerCaptor.capture());
            assertThat(customerCaptor.getValue().getStatus()).isEqualTo(OnboardingStatus.APPROVED);
            verify(restTemplate).postForObject(anyString(), any(), eq(java.util.Map.class));
        }

        @Test
        @DisplayName("Should throw InvalidStatusTransitionException when customer is not PENDING_REVIEW")
        void approve_NotPendingReview_ThrowsInvalidTransition() {
            Customer customer = buildCustomer(OnboardingStatus.DRAFT);
            when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));

            assertThatThrownBy(() -> service.approve(customer.getId()))
                    .isInstanceOf(InvalidStatusTransitionException.class)
                    .hasMessageContaining("PENDING_REVIEW");

            verify(customerRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw RuntimeException when payment service call fails")
        void approve_PaymentServiceFails_ThrowsRuntimeException() {
            Customer customer = buildCustomer(OnboardingStatus.PENDING_REVIEW);
            UUID id = customer.getId();

            when(customerRepository.findById(id)).thenReturn(Optional.of(customer));
            when(customerRepository.save(any(Customer.class))).thenReturn(customer);
            when(restTemplate.postForObject(anyString(), any(), eq(java.util.Map.class)))
                    .thenThrow(new RestClientException("Connection refused"));

            assertThatThrownBy(() -> service.approve(id))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("payment service failed");
        }

        @Test
        @DisplayName("Should throw CustomerNotFoundException for unknown id")
        void approve_UnknownId_ThrowsException() {
            UUID id = UUID.randomUUID();
            when(customerRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.approve(id))
                    .isInstanceOf(CustomerNotFoundException.class);
        }
    }

    // ===================== reject =====================

    @Nested
    @DisplayName("reject")
    class RejectTests {

        @Test
        @DisplayName("Should reject PENDING_REVIEW customer")
        void reject_PendingReviewCustomer_SetsRejectedStatus() {
            Customer customer = buildCustomer(OnboardingStatus.PENDING_REVIEW);
            UUID id = customer.getId();
            RejectRequest req = new RejectRequest();
            req.setReason("Incomplete KYC documentation");

            when(customerRepository.findById(id)).thenReturn(Optional.of(customer));
            when(customerRepository.save(any(Customer.class))).thenReturn(customer);

            service.reject(id, req);

            verify(customerRepository).save(customerCaptor.capture());
            assertThat(customerCaptor.getValue().getStatus()).isEqualTo(OnboardingStatus.REJECTED);
        }

        @Test
        @DisplayName("Should throw InvalidStatusTransitionException when customer is not PENDING_REVIEW")
        void reject_NotPendingReview_ThrowsInvalidTransition() {
            Customer customer = buildCustomer(OnboardingStatus.DRAFT);
            RejectRequest req = new RejectRequest();
            req.setReason("Wrong status");

            when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));

            assertThatThrownBy(() -> service.reject(customer.getId(), req))
                    .isInstanceOf(InvalidStatusTransitionException.class)
                    .hasMessageContaining("PENDING_REVIEW");

            verify(customerRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw CustomerNotFoundException for unknown id")
        void reject_UnknownId_ThrowsException() {
            UUID id = UUID.randomUUID();
            RejectRequest req = new RejectRequest();
            req.setReason("N/A");
            when(customerRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.reject(id, req))
                    .isInstanceOf(CustomerNotFoundException.class);
        }
    }
}
