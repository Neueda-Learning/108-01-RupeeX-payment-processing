package com.rupeex.main.controller;

import com.rupeex.main.dto.CreateAccountRequest;
import com.rupeex.main.entity.Account;
import com.rupeex.main.repository.AccountsRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/accounts")
@Tag(name = "Accounts", description = "Account management endpoints")
public class AccountController {

    private final AccountsRepository accountsRepository;

    public AccountController(AccountsRepository accountsRepository) {
        this.accountsRepository = accountsRepository;
    }

    @GetMapping
    @Operation(summary = "Get all accounts", description = "Retrieve a list of all accounts in the system, sorted by account holder name")
    @ApiResponse(responseCode = "200", description = "List of accounts retrieved successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Account.class)))
    public List<Account> getAllAccounts() {
        return accountsRepository.findAllByOrderByAccountHolderAsc();
    }

    @GetMapping("/{accountNumber}")
    @Operation(summary = "Get account by number", description = "Retrieve account details using the account number")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Account found and returned successfully"),
            @ApiResponse(responseCode = "404", description = "Account not found with the provided account number")
    })
    public Account getAccount(
            @Parameter(description = "Account number", example = "ACC123456", required = true)
            @PathVariable String accountNumber) {
        return accountsRepository
                .findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Account not found: " + accountNumber));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create account", description = "Create a new account in the system")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Account created successfully"),
            @ApiResponse(responseCode = "409", description = "Account with the same account number already exists")
    })
    public Account createAccount(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Account creation request with required fields",
                    required = true)
            @RequestBody CreateAccountRequest request) {
        if (accountsRepository.findByAccountNumber(request.getAccountNumber()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Account already exists: " + request.getAccountNumber());
        }
        Account account = new Account();
        account.setAccountNumber(request.getAccountNumber());
        account.setAccountHolder(request.getAccountHolder());
        account.setAccountType(request.getAccountType() != null ? request.getAccountType() : "SAVINGS");
        account.setCurrency(request.getCurrency() != null ? request.getCurrency() : "INR");
        account.setCountryCode(request.getCountryCode());
        account.setStatus("ACTIVE");
        return accountsRepository.save(account);
    }
}
