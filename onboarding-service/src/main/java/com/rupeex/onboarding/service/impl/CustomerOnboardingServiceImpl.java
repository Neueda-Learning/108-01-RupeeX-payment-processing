package com.rupeex.onboarding.service.impl;

import com.rupeex.onboarding.dto.ConsentRequest;
import com.rupeex.onboarding.dto.CreateCustomerRequest;
import com.rupeex.onboarding.dto.CustomerResponse;
import com.rupeex.onboarding.dto.CustomerStatusResponse;
import com.rupeex.onboarding.dto.RejectRequest;
import com.rupeex.onboarding.entity.Consent;
import com.rupeex.onboarding.entity.Customer;
import com.rupeex.onboarding.enums.OnboardingStatus;
import com.rupeex.onboarding.exception.CustomerAlreadyExistsException;
import com.rupeex.onboarding.exception.CustomerNotFoundException;
import com.rupeex.onboarding.exception.InvalidStatusTransitionException;
import com.rupeex.onboarding.repository.ConsentRepository;
import com.rupeex.onboarding.repository.CustomerRepository;
import com.rupeex.onboarding.service.CustomerOnboardingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CustomerOnboardingServiceImpl implements CustomerOnboardingService {

    private final CustomerRepository customerRepository;
    private final ConsentRepository consentRepository;

    public CustomerOnboardingServiceImpl(CustomerRepository customerRepository, ConsentRepository consentRepository) {
        this.customerRepository = customerRepository;
        this.consentRepository = consentRepository;
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
        response.setCreatedAt(customer.getCreatedAt());
        response.setUpdatedAt(customer.getUpdatedAt());
        return response;
    }
}
