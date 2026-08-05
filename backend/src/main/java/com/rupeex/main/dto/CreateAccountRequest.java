package com.rupeex.main.dto;

public class CreateAccountRequest {

    private String accountNumber;
    private String accountHolder;
    private String accountType;
    private String currency;
    private String countryCode;
    private String email;

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getAccountHolder() { return accountHolder; }
    public void setAccountHolder(String accountHolder) { this.accountHolder = accountHolder; }

    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}

