package com.rupeex.main.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CreateAccountRequest {

    @JsonProperty("accountNumber")
    private String accountNumber;
    
    @JsonProperty("accountHolder")
    private String accountHolder;
    
    @JsonProperty("accountType")
    private String accountType;
    
    @JsonProperty("currency")
    private String currency;
    
    @JsonProperty("countryCode")
    private String countryCode;
    
    @JsonProperty("email")
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

