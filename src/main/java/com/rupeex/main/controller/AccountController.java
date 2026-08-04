package com.rupeex.main.controller;

import com.rupeex.main.dto.CreateAccountRequest;
import com.rupeex.main.entity.Account;
import com.rupeex.main.repository.AccountsRepository;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountsRepository accountsRepository;

    public AccountController(AccountsRepository accountsRepository) {
        this.accountsRepository = accountsRepository;
    }

    @GetMapping
    public List<Account> getAllAccounts() {
        return accountsRepository.findAllByOrderByAccountHolderAsc();
    }

    @GetMapping("/{accountNumber}")
    public Account getAccount(@PathVariable String accountNumber) {
        return accountsRepository
                .findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Account not found: " + accountNumber));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Account createAccount(@RequestBody CreateAccountRequest request) {
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
