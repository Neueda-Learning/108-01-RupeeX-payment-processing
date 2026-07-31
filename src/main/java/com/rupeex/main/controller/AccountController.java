package com.rupeex.main.controller;


import com.rupeex.main.model.Accounts;
import com.rupeex.main.repository.AccountsRepository;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;



@RestController
@RequestMapping("/accounts")
public class AccountController {


    private final AccountsRepository accountsRepository;


    public AccountController(
            AccountsRepository accountsRepository){

        this.accountsRepository = accountsRepository;
    }



    @GetMapping("/{accountNumber}")
    public Accounts getAccount(
            @PathVariable String accountNumber){

        return accountsRepository
                .findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Account not found: " + accountNumber
                ));
    }

}