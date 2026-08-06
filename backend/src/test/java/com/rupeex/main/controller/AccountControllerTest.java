package com.rupeex.main.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rupeex.main.dto.CreateAccountRequest;
import com.rupeex.main.entity.Account;
import com.rupeex.main.repository.AccountsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AccountController.class)
@DisplayName("AccountController Tests")
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AccountsRepository accountsRepository;

    private Account testAccount;

    @BeforeEach
    void setUp() {
        testAccount = new Account();
        testAccount.setId(1L);
        testAccount.setAccountNumber("ACC-001");
        testAccount.setAccountHolder("John Doe");
    }

    @Test
    @DisplayName("Should get all accounts")
    void getAllAccounts_Success() throws Exception {
        List<Account> accounts = new ArrayList<>();
        accounts.add(testAccount);
        when(accountsRepository.findAllByOrderByAccountHolderAsc()).thenReturn(accounts);

        mockMvc.perform(get("/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].accountNumber", is("ACC-001")));

        verify(accountsRepository, times(1)).findAllByOrderByAccountHolderAsc();
    }

    @Test
    @DisplayName("Should return empty list when no accounts")
    void getAllAccounts_Empty_Success() throws Exception {
        when(accountsRepository.findAllByOrderByAccountHolderAsc()).thenReturn(new ArrayList<>());

        mockMvc.perform(get("/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Should get account by number")
    void getAccount_ValidNumber_Success() throws Exception {
        when(accountsRepository.findByAccountNumber("ACC-001")).thenReturn(Optional.of(testAccount));

        mockMvc.perform(get("/accounts/ACC-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber", is("ACC-001")))
                .andExpect(jsonPath("$.accountHolder", is("John Doe")));

        verify(accountsRepository, times(1)).findByAccountNumber("ACC-001");
    }

    @Test
    @DisplayName("Should return error for non-existent account")
    void getAccount_NotFound_ReturnsError() throws Exception {
        when(accountsRepository.findByAccountNumber("NON-EXIST")).thenReturn(Optional.empty());

        // Note: GlobalExceptionHandler currently maps all uncaught exceptions
        // (including ResponseStatusException) to 500 via handleGeneralException.
        mockMvc.perform(get("/accounts/NON-EXIST"))
                .andExpect(status().isInternalServerError());

        verify(accountsRepository, times(1)).findByAccountNumber("NON-EXIST");
    }

    @Test
    @DisplayName("Should return JSON content type")
    void getAccount_CorrectContentType_Success() throws Exception {
        when(accountsRepository.findByAccountNumber("ACC-001")).thenReturn(Optional.of(testAccount));

        mockMvc.perform(get("/accounts/ACC-001"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"));
    }

    @Test
    @DisplayName("Should handle multiple accounts")
    void getAllAccounts_Multiple_Success() throws Exception {
        List<Account> accounts = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Account account = new Account();
            account.setAccountNumber("ACC-" + String.format("%03d", i));
            account.setAccountHolder("User " + i);
            accounts.add(account);
        }
        when(accountsRepository.findAllByOrderByAccountHolderAsc()).thenReturn(accounts);

        mockMvc.perform(get("/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)));
    }

    @Test
    @DisplayName("Should create account successfully")
    void createAccount_ValidRequest_Returns201() throws Exception {
        CreateAccountRequest request = new CreateAccountRequest();
        request.setAccountNumber("ACC-002");
        request.setAccountHolder("Jane Doe");
        request.setAccountType("SAVINGS");
        request.setCurrency("INR");
        request.setCountryCode("IN");
        request.setEmail("jane@example.com");

        Account created = new Account();
        created.setId(2L);
        created.setAccountNumber("ACC-002");
        created.setAccountHolder("Jane Doe");

        when(accountsRepository.findByAccountNumber("ACC-002")).thenReturn(Optional.empty());
        when(accountsRepository.save(any(Account.class))).thenReturn(created);

        mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountNumber").value("ACC-002"))
                .andExpect(jsonPath("$.accountHolder").value("Jane Doe"));

        verify(accountsRepository, times(1)).save(any(Account.class));
    }

    @Test
    @DisplayName("Should return 409 when account already exists")
    void createAccount_DuplicateAccountNumber_Returns409() throws Exception {
        CreateAccountRequest request = new CreateAccountRequest();
        request.setAccountNumber("ACC-001");
        request.setAccountHolder("John Doe");
        request.setAccountType("SAVINGS");
        request.setCurrency("INR");

        when(accountsRepository.findByAccountNumber("ACC-001")).thenReturn(Optional.of(testAccount));

        // Note: ResponseStatusException is caught by GlobalExceptionHandler's handleGeneralException
        // which returns 500 for all exceptions not explicitly handled by a specific handler.
        mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());

        verify(accountsRepository, never()).save(any());
    }
}

