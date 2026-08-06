package com.rupeex.onboarding.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GlobalExceptionHandler Tests")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("Should return 404 with CUSTOMER_NOT_FOUND for CustomerNotFoundException")
    void handleNotFound_Returns404() {
        CustomerNotFoundException ex = new CustomerNotFoundException("Customer not found: 123");
        ResponseEntity<Map<String, Object>> response = handler.handleNotFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("errorCode", "CUSTOMER_NOT_FOUND");
        assertThat(response.getBody()).containsEntry("message", "Customer not found: 123");
        assertThat(response.getBody()).containsKey("timestamp");
    }

    @Test
    @DisplayName("Should return 409 with CUSTOMER_ALREADY_EXISTS for CustomerAlreadyExistsException")
    void handleDuplicate_Returns409() {
        CustomerAlreadyExistsException ex = new CustomerAlreadyExistsException("Customer already exists for email: x@x.com");
        ResponseEntity<Map<String, Object>> response = handler.handleDuplicate(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).containsEntry("errorCode", "CUSTOMER_ALREADY_EXISTS");
        assertThat(response.getBody()).containsEntry("message", "Customer already exists for email: x@x.com");
    }

    @Test
    @DisplayName("Should return 409 with INVALID_STATUS_TRANSITION for InvalidStatusTransitionException")
    void handleStatusTransition_Returns409() {
        InvalidStatusTransitionException ex = new InvalidStatusTransitionException("Invalid transition from DRAFT");
        ResponseEntity<Map<String, Object>> response = handler.handleStatusTransition(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).containsEntry("errorCode", "INVALID_STATUS_TRANSITION");
        assertThat(response.getBody()).containsEntry("message", "Invalid transition from DRAFT");
    }

    @Test
    @DisplayName("Should return 400 with VALIDATION_FAILED for MethodArgumentNotValidException")
    void handleValidation_Returns400() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("req", "email", "Email must be valid");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<Map<String, Object>> response = handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("errorCode", "VALIDATION_FAILED");
        assertThat(response.getBody().get("message").toString()).contains("email");
    }

    @Test
    @DisplayName("Should return first field error message for multiple validation errors")
    void handleValidation_MultipleErrors_ReturnsFirstOne() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError firstError = new FieldError("req", "fullName", "Full name is required");
        FieldError secondError = new FieldError("req", "phone", "Phone is required");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(firstError, secondError));

        ResponseEntity<Map<String, Object>> response = handler.handleValidation(ex);

        assertThat(response.getBody().get("message").toString()).contains("fullName");
    }

    @Test
    @DisplayName("Should return 400 with fallback message when field errors list is empty")
    void handleValidation_EmptyFieldErrors_ReturnsFallback() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of());

        ResponseEntity<Map<String, Object>> response = handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("message").toString()).contains("Validation failed");
    }

    @Test
    @DisplayName("Should return 500 with INTERNAL_ERROR for unhandled exceptions")
    void handleGeneral_Returns500() {
        Exception ex = new RuntimeException("Something went wrong");
        ResponseEntity<Map<String, Object>> response = handler.handleGeneral(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("errorCode", "INTERNAL_ERROR");
        assertThat(response.getBody()).containsEntry("message", "Something went wrong");
    }

    @Test
    @DisplayName("Should include timestamp in all error responses")
    void errorResponse_AlwaysContainsTimestamp() {
        CustomerNotFoundException ex = new CustomerNotFoundException("not found");
        ResponseEntity<Map<String, Object>> response = handler.handleNotFound(ex);
        assertThat(response.getBody()).containsKey("timestamp");
        assertThat(response.getBody().get("timestamp")).isNotNull();
    }
}
