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
import com.rupeex.onboarding.service.CustomerOnboardingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CustomerOnboardingServiceImpl implements CustomerOnboardingService {

    private static final Logger log = LoggerFactory.getLogger(CustomerOnboardingServiceImpl.class);

    private final CustomerRepository customerRepository;
    private final ConsentRepository consentRepository;
    private final RestTemplate restTemplate;

    @Value("${payment.service.url:http://localhost:8080}")
    private String paymentServiceUrl;

    public CustomerOnboardingServiceImpl(CustomerRepository customerRepository,
                                         ConsentRepository consentRepository,
                                         RestTemplate restTemplate) {
        this.customerRepository = customerRepository;
        this.consentRepository = consentRepository;
        this.restTemplate = restTemplate;
    }

    @Override
    @Transactional
    public CustomerResponse createCustomer(CreateCustomerRequest request) {
        if (customerRepository.existsByEmail(request.getEmail())) {
            throw new CustomerAlreadyExistsException("Customer already exists for email: " + request.getEmail());
        }
        if (customerRepository.existsByPhone(request.getPhone())) {
            throw new CustomerAlreadyExistsException("Customer already exists for phone: " + request.getPhone());
        }

        Customer customer = new Customer();
        customer.setFullName(request.getFullName());
        customer.setEmail(request.getEmail());
        customer.setPhone(request.getPhone());
        customer.setDob(request.getDob());
        customer.setExternalRef(request.getExternalRef());
        customer.setAccountNumber(request.getAccountNumber());
        customer.setAccountType(request.getAccountType());
        customer.setCurrency(request.getCurrency());
        customer.setCountryCode(request.getCountryCode());
        customer.setRole(request.getRole() != null ? request.getRole() : UserRole.MEMBER);
        customer.setStatus(OnboardingStatus.DRAFT);

        Customer saved = customerRepository.save(customer);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getCustomer(UUID customerId) {
        Customer customer = getCustomerEntity(customerId);
        return toResponse(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerStatusResponse getStatus(UUID customerId) {
        Customer customer = getCustomerEntity(customerId);

        CustomerStatusResponse status = new CustomerStatusResponse();
        status.setCustomerId(customer.getId());
        status.setStatus(customer.getStatus());
        status.setEligibleForPayments(customer.getStatus() == OnboardingStatus.APPROVED);
        return status;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerSummary> listCustomers() {
        return customerRepository.findAll().stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void addConsent(UUID customerId, ConsentRequest request) {
        Customer customer = getCustomerEntity(customerId);

        Consent consent = new Consent();
        consent.setCustomer(customer);
        consent.setConsentType(request.getConsentType());
        consent.setConsentVersion(request.getVersion());
        consent.setAccepted(request.isAccepted());

        consentRepository.save(consent);
    }

    @Override
    @Transactional
    public void submitForReview(UUID customerId) {
        Customer customer = getCustomerEntity(customerId);
        requireStatus(customer, OnboardingStatus.DRAFT);

        boolean hasAcceptedConsent = consentRepository.existsByCustomer_IdAndAcceptedTrue(customerId);
        if (!hasAcceptedConsent) {
            throw new InvalidStatusTransitionException("At least one accepted consent is required before submit");
        }

        customer.setStatus(OnboardingStatus.PENDING_REVIEW);
        customerRepository.save(customer);
    }

    @Override
    @Transactional
    public void approve(UUID customerId) {
        Customer customer = getCustomerEntity(customerId);
        requireStatus(customer, OnboardingStatus.PENDING_REVIEW);
        customer.setStatus(OnboardingStatus.APPROVED);
        customerRepository.save(customer);

        try {
            createAccountInPaymentService(customer);
            log.info("Account created in payment service for customer: {}, account: {}", customerId, customer.getAccountNumber());
        } catch (Exception e) {
            log.error("Failed to create account in payment service for customer: {}. Error: {}", customerId, e.getMessage());
        }
    }

    private void createAccountInPaymentService(Customer customer) {
        String url = paymentServiceUrl + "/accounts";
        Map<String, Object> payload = new HashMap<>();
        payload.put("accountNumber", customer.getAccountNumber());
        payload.put("accountHolder", customer.getFullName());
        payload.put("accountType", customer.getAccountType() != null ? customer.getAccountType() : "SAVINGS");
        payload.put("currency", customer.getCurrency() != null ? customer.getCurrency() : "INR");
        payload.put("countryCode", customer.getCountryCode());
        payload.put("email", customer.getEmail());
        restTemplate.postForEntity(url, payload, Map.class);
    }

    @Override
    @Transactional
    public void reject(UUID customerId, RejectRequest request) {
        Customer customer = getCustomerEntity(customerId);
        requireStatus(customer, OnboardingStatus.PENDING_REVIEW);
        customer.setStatus(OnboardingStatus.REJECTED);
        customerRepository.save(customer);
    }

    private Customer getCustomerEntity(UUID customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + customerId));
    }

    private void requireStatus(Customer customer, OnboardingStatus requiredStatus) {
        if (customer.getStatus() != requiredStatus) {
            throw new InvalidStatusTransitionException(
                    "Invalid status transition from " + customer.getStatus() + ", required: " + requiredStatus
            );
        }
    }

    private CustomerResponse toResponse(Customer customer) {
        CustomerResponse response = new CustomerResponse();
        response.setCustomerId(customer.getId());
        response.setFullName(customer.getFullName());
        response.setEmail(customer.getEmail());
        response.setPhone(customer.getPhone());
        response.setDob(customer.getDob());
        response.setExternalRef(customer.getExternalRef());
        response.setStatus(customer.getStatus());
        response.setAccountNumber(customer.getAccountNumber());
        response.setAccountType(customer.getAccountType());
        response.setCurrency(customer.getCurrency());
        response.setCountryCode(customer.getCountryCode());
        response.setRole(customer.getRole());
        response.setCreatedAt(customer.getCreatedAt());
        response.setUpdatedAt(customer.getUpdatedAt());
        return response;
    }

    private CustomerSummary toSummary(Customer customer) {
        CustomerSummary summary = new CustomerSummary();
        summary.setCustomerId(customer.getId());
        summary.setFullName(customer.getFullName());
        summary.setEmail(customer.getEmail());
        summary.setPhone(customer.getPhone());
        summary.setAccountNumber(customer.getAccountNumber());
        summary.setStatus(customer.getStatus());
        summary.setRole(customer.getRole());
        return summary;
    }
}
