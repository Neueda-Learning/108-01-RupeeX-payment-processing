package com.rupeex.onboarding.controller;

import com.rupeex.onboarding.dto.ConsentRequest;
import com.rupeex.onboarding.dto.CreateCustomerRequest;
import com.rupeex.onboarding.dto.CustomerResponse;
import com.rupeex.onboarding.dto.CustomerStatusResponse;
import com.rupeex.onboarding.dto.CustomerSummary;
import com.rupeex.onboarding.dto.RejectRequest;
import com.rupeex.onboarding.enums.OnboardingStatus;
import com.rupeex.onboarding.enums.UserRole;
import com.rupeex.onboarding.exception.CustomerAlreadyExistsException;
import com.rupeex.onboarding.exception.CustomerNotFoundException;
import com.rupeex.onboarding.exception.InvalidStatusTransitionException;
import com.rupeex.onboarding.service.CustomerOnboardingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerOnboardingController.class)
@DisplayName("CustomerOnboardingController Tests")
class CustomerOnboardingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CustomerOnboardingService customerOnboardingService;

    private UUID customerId;
    private CustomerResponse customerResponse;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        customerResponse = buildCustomerResponse(customerId, OnboardingStatus.DRAFT);
    }

    // ===================== Helper Methods =====================

    private CustomerResponse buildCustomerResponse(UUID id, OnboardingStatus status) {
        CustomerResponse r = new CustomerResponse();
        r.setCustomerId(id);
        r.setFullName("Alice Kumar");
        r.setEmail("alice@example.com");
        r.setPhone("+919876543210");
        r.setDob(LocalDate.of(1990, 5, 15));
        r.setAccountNumber("RUPX001234");
        r.setAccountType("SAVINGS");
        r.setCurrency("INR");
        r.setCountryCode("IN");
        r.setRole(UserRole.MEMBER);
        r.setStatus(status);
        r.setCreatedAt(LocalDateTime.now());
        r.setUpdatedAt(LocalDateTime.now());
        return r;
    }

    private CreateCustomerRequest buildValidCreateRequest() {
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

    // ===================== POST /customers =====================

    @Nested
    @DisplayName("POST /customers")
    class CreateCustomerEndpointTests {

        @Test
        @DisplayName("Should return 201 with created customer on valid request")
        void createCustomer_ValidRequest_Returns201() throws Exception {
            when(customerOnboardingService.createCustomer(any(CreateCustomerRequest.class)))
                    .thenReturn(customerResponse);

            mockMvc.perform(post("/customers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildValidCreateRequest())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.customerId", is(customerId.toString())))
                    .andExpect(jsonPath("$.email", is("alice@example.com")))
                    .andExpect(jsonPath("$.status", is("DRAFT")));

            verify(customerOnboardingService, times(1)).createCustomer(any(CreateCustomerRequest.class));
        }

        @Test
        @DisplayName("Should return 400 when fullName is missing")
        void createCustomer_MissingFullName_Returns400() throws Exception {
            CreateCustomerRequest req = buildValidCreateRequest();
            req.setFullName("");

            mockMvc.perform(post("/customers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode", is("VALIDATION_FAILED")));
        }

        @Test
        @DisplayName("Should return 400 when email is invalid")
        void createCustomer_InvalidEmail_Returns400() throws Exception {
            CreateCustomerRequest req = buildValidCreateRequest();
            req.setEmail("not-an-email");

            mockMvc.perform(post("/customers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode", is("VALIDATION_FAILED")));
        }

        @Test
        @DisplayName("Should return 400 when phone is blank")
        void createCustomer_BlankPhone_Returns400() throws Exception {
            CreateCustomerRequest req = buildValidCreateRequest();
            req.setPhone("");

            mockMvc.perform(post("/customers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 409 when customer already exists")
        void createCustomer_DuplicateCustomer_Returns409() throws Exception {
            when(customerOnboardingService.createCustomer(any(CreateCustomerRequest.class)))
                    .thenThrow(new CustomerAlreadyExistsException("Customer already exists for email: alice@example.com"));

            mockMvc.perform(post("/customers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildValidCreateRequest())))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errorCode", is("CUSTOMER_ALREADY_EXISTS")));
        }

        @Test
        @DisplayName("Should return 400 when accountType is blank")
        void createCustomer_BlankAccountType_Returns400() throws Exception {
            CreateCustomerRequest req = buildValidCreateRequest();
            req.setAccountType("");

            mockMvc.perform(post("/customers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when currency is blank")
        void createCustomer_BlankCurrency_Returns400() throws Exception {
            CreateCustomerRequest req = buildValidCreateRequest();
            req.setCurrency("");

            mockMvc.perform(post("/customers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ===================== GET /customers =====================

    @Nested
    @DisplayName("GET /customers")
    class ListCustomersEndpointTests {

        @Test
        @DisplayName("Should return list of customers")
        void listCustomers_ReturnsOkWithList() throws Exception {
            CustomerSummary summary = new CustomerSummary();
            summary.setCustomerId(customerId);
            summary.setFullName("Alice Kumar");
            summary.setEmail("alice@example.com");
            summary.setPhone("+919876543210");
            summary.setAccountNumber("RUPX001234");
            summary.setStatus(OnboardingStatus.DRAFT);
            summary.setRole(UserRole.MEMBER);

            when(customerOnboardingService.listCustomers()).thenReturn(List.of(summary));

            mockMvc.perform(get("/customers"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].email", is("alice@example.com")))
                    .andExpect(jsonPath("$[0].status", is("DRAFT")));
        }

        @Test
        @DisplayName("Should return empty array when no customers exist")
        void listCustomers_Empty_ReturnsEmptyArray() throws Exception {
            when(customerOnboardingService.listCustomers()).thenReturn(List.of());

            mockMvc.perform(get("/customers"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    // ===================== GET /customers/{customerId} =====================

    @Nested
    @DisplayName("GET /customers/{customerId}")
    class GetCustomerEndpointTests {

        @Test
        @DisplayName("Should return 200 with customer details for valid id")
        void getCustomer_ValidId_ReturnsOk() throws Exception {
            when(customerOnboardingService.getCustomer(customerId)).thenReturn(customerResponse);

            mockMvc.perform(get("/customers/{id}", customerId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.customerId", is(customerId.toString())))
                    .andExpect(jsonPath("$.fullName", is("Alice Kumar")))
                    .andExpect(jsonPath("$.accountNumber", is("RUPX001234")));
        }

        @Test
        @DisplayName("Should return 404 when customer not found")
        void getCustomer_UnknownId_Returns404() throws Exception {
            when(customerOnboardingService.getCustomer(customerId))
                    .thenThrow(new CustomerNotFoundException("Customer not found: " + customerId));

            mockMvc.perform(get("/customers/{id}", customerId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode", is("CUSTOMER_NOT_FOUND")));
        }
    }

    // ===================== GET /customers/{customerId}/status =====================

    @Nested
    @DisplayName("GET /customers/{customerId}/status")
    class GetStatusEndpointTests {

        @Test
        @DisplayName("Should return status with eligibility for approved customer")
        void getStatus_ApprovedCustomer_ReturnsEligible() throws Exception {
            CustomerStatusResponse statusResponse = new CustomerStatusResponse();
            statusResponse.setCustomerId(customerId);
            statusResponse.setStatus(OnboardingStatus.APPROVED);
            statusResponse.setEligibleForPayments(true);

            when(customerOnboardingService.getStatus(customerId)).thenReturn(statusResponse);

            mockMvc.perform(get("/customers/{id}/status", customerId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("APPROVED")))
                    .andExpect(jsonPath("$.eligibleForPayments", is(true)));
        }

        @Test
        @DisplayName("Should return status with ineligibility for draft customer")
        void getStatus_DraftCustomer_ReturnsIneligible() throws Exception {
            CustomerStatusResponse statusResponse = new CustomerStatusResponse();
            statusResponse.setCustomerId(customerId);
            statusResponse.setStatus(OnboardingStatus.DRAFT);
            statusResponse.setEligibleForPayments(false);

            when(customerOnboardingService.getStatus(customerId)).thenReturn(statusResponse);

            mockMvc.perform(get("/customers/{id}/status", customerId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("DRAFT")))
                    .andExpect(jsonPath("$.eligibleForPayments", is(false)));
        }

        @Test
        @DisplayName("Should return 404 when customer not found")
        void getStatus_UnknownId_Returns404() throws Exception {
            when(customerOnboardingService.getStatus(customerId))
                    .thenThrow(new CustomerNotFoundException("Customer not found: " + customerId));

            mockMvc.perform(get("/customers/{id}/status", customerId))
                    .andExpect(status().isNotFound());
        }
    }

    // ===================== POST /customers/{customerId}/consents =====================

    @Nested
    @DisplayName("POST /customers/{customerId}/consents")
    class AddConsentEndpointTests {

        @Test
        @DisplayName("Should return 204 when consent is added successfully")
        void addConsent_ValidRequest_Returns204() throws Exception {
            ConsentRequest req = new ConsentRequest();
            req.setConsentType("TERMS_AND_CONDITIONS");
            req.setVersion("1.0");
            req.setAccepted(true);

            doNothing().when(customerOnboardingService).addConsent(eq(customerId), any(ConsentRequest.class));

            mockMvc.perform(post("/customers/{id}/consents", customerId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isNoContent());

            verify(customerOnboardingService).addConsent(eq(customerId), any(ConsentRequest.class));
        }

        @Test
        @DisplayName("Should return 400 when consentType is missing")
        void addConsent_MissingConsentType_Returns400() throws Exception {
            ConsentRequest req = new ConsentRequest();
            req.setVersion("1.0");

            mockMvc.perform(post("/customers/{id}/consents", customerId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode", is("VALIDATION_FAILED")));
        }

        @Test
        @DisplayName("Should return 400 when version is missing")
        void addConsent_MissingVersion_Returns400() throws Exception {
            ConsentRequest req = new ConsentRequest();
            req.setConsentType("TERMS");

            mockMvc.perform(post("/customers/{id}/consents", customerId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode", is("VALIDATION_FAILED")));
        }

        @Test
        @DisplayName("Should return 404 when customer not found")
        void addConsent_UnknownCustomer_Returns404() throws Exception {
            ConsentRequest req = new ConsentRequest();
            req.setConsentType("TERMS");
            req.setVersion("1.0");

            doThrow(new CustomerNotFoundException("Customer not found"))
                    .when(customerOnboardingService).addConsent(eq(customerId), any(ConsentRequest.class));

            mockMvc.perform(post("/customers/{id}/consents", customerId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isNotFound());
        }
    }

    // ===================== POST /customers/{customerId}/submit =====================

    @Nested
    @DisplayName("POST /customers/{customerId}/submit")
    class SubmitForReviewEndpointTests {

        @Test
        @DisplayName("Should return 204 when submitted successfully")
        void submitForReview_ValidCustomer_Returns204() throws Exception {
            doNothing().when(customerOnboardingService).submitForReview(customerId);

            mockMvc.perform(post("/customers/{id}/submit", customerId))
                    .andExpect(status().isNoContent());

            verify(customerOnboardingService).submitForReview(customerId);
        }

        @Test
        @DisplayName("Should return 409 when status transition is invalid")
        void submitForReview_InvalidStatus_Returns409() throws Exception {
            doThrow(new InvalidStatusTransitionException("Invalid status transition from PENDING_REVIEW, required: DRAFT"))
                    .when(customerOnboardingService).submitForReview(customerId);

            mockMvc.perform(post("/customers/{id}/submit", customerId))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errorCode", is("INVALID_STATUS_TRANSITION")));
        }

        @Test
        @DisplayName("Should return 404 when customer not found")
        void submitForReview_UnknownCustomer_Returns404() throws Exception {
            doThrow(new CustomerNotFoundException("Customer not found"))
                    .when(customerOnboardingService).submitForReview(customerId);

            mockMvc.perform(post("/customers/{id}/submit", customerId))
                    .andExpect(status().isNotFound());
        }
    }

    // ===================== POST /customers/{customerId}/approve =====================

    @Nested
    @DisplayName("POST /customers/{customerId}/approve")
    class ApproveEndpointTests {

        @Test
        @DisplayName("Should return 204 when customer is approved successfully")
        void approve_ValidCustomer_Returns204() throws Exception {
            doNothing().when(customerOnboardingService).approve(customerId);

            mockMvc.perform(post("/customers/{id}/approve", customerId))
                    .andExpect(status().isNoContent());

            verify(customerOnboardingService).approve(customerId);
        }

        @Test
        @DisplayName("Should return 409 when status transition is invalid")
        void approve_InvalidStatus_Returns409() throws Exception {
            doThrow(new InvalidStatusTransitionException("Invalid status transition from DRAFT, required: PENDING_REVIEW"))
                    .when(customerOnboardingService).approve(customerId);

            mockMvc.perform(post("/customers/{id}/approve", customerId))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errorCode", is("INVALID_STATUS_TRANSITION")));
        }

        @Test
        @DisplayName("Should return 404 when customer not found")
        void approve_UnknownCustomer_Returns404() throws Exception {
            doThrow(new CustomerNotFoundException("Customer not found"))
                    .when(customerOnboardingService).approve(customerId);

            mockMvc.perform(post("/customers/{id}/approve", customerId))
                    .andExpect(status().isNotFound());
        }
    }

    // ===================== POST /customers/{customerId}/reject =====================

    @Nested
    @DisplayName("POST /customers/{customerId}/reject")
    class RejectEndpointTests {

        @Test
        @DisplayName("Should return 204 when customer is rejected successfully")
        void reject_ValidRequest_Returns204() throws Exception {
            RejectRequest req = new RejectRequest();
            req.setReason("Incomplete KYC documentation");

            doNothing().when(customerOnboardingService).reject(eq(customerId), any(RejectRequest.class));

            mockMvc.perform(post("/customers/{id}/reject", customerId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isNoContent());

            verify(customerOnboardingService).reject(eq(customerId), any(RejectRequest.class));
        }

        @Test
        @DisplayName("Should return 400 when reason is blank")
        void reject_BlankReason_Returns400() throws Exception {
            RejectRequest req = new RejectRequest();
            req.setReason("");

            mockMvc.perform(post("/customers/{id}/reject", customerId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode", is("VALIDATION_FAILED")));
        }

        @Test
        @DisplayName("Should return 409 when status transition is invalid")
        void reject_InvalidStatus_Returns409() throws Exception {
            RejectRequest req = new RejectRequest();
            req.setReason("Invalid");

            doThrow(new InvalidStatusTransitionException("Invalid transition"))
                    .when(customerOnboardingService).reject(eq(customerId), any(RejectRequest.class));

            mockMvc.perform(post("/customers/{id}/reject", customerId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Should return 404 when customer not found")
        void reject_UnknownCustomer_Returns404() throws Exception {
            RejectRequest req = new RejectRequest();
            req.setReason("No show");

            doThrow(new CustomerNotFoundException("Customer not found"))
                    .when(customerOnboardingService).reject(eq(customerId), any(RejectRequest.class));

            mockMvc.perform(post("/customers/{id}/reject", customerId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isNotFound());
        }
    }
}
