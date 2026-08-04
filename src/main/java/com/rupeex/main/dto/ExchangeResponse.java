package com.rupeex.main.dto;


import java.math.BigDecimal;


public class ExchangeResponse {


    private BigDecimal originalAmount;


    private String fromCurrency;


    private String toCurrency;


    private BigDecimal exchangeRate;


    private BigDecimal convertedAmount;

    public BigDecimal getOriginalAmount() {
        return originalAmount;
    }

    public void setOriginalAmount(BigDecimal originalAmount) {
        this.originalAmount = originalAmount;
    }

    public String getFromCurrency() {
        return fromCurrency;
    }

    public void setFromCurrency(String fromCurrency) {
        this.fromCurrency = fromCurrency;
    }

    public String getToCurrency() {
        return toCurrency;
    }

    public void setToCurrency(String toCurrency) {
        this.toCurrency = toCurrency;
    }

    public BigDecimal getExchangeRate() {
        return exchangeRate;
    }

    public void setExchangeRate(BigDecimal exchangeRate) {
        this.exchangeRate = exchangeRate;
    }

    public BigDecimal getConvertedAmount() {
        return convertedAmount;
    }

    public void setConvertedAmount(BigDecimal convertedAmount) {
        this.convertedAmount = convertedAmount;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final ExchangeResponse response = new ExchangeResponse();

        public Builder originalAmount(BigDecimal originalAmount) {
            response.setOriginalAmount(originalAmount);
            return this;
        }

        public Builder fromCurrency(String fromCurrency) {
            response.setFromCurrency(fromCurrency);
            return this;
        }

        public Builder toCurrency(String toCurrency) {
            response.setToCurrency(toCurrency);
            return this;
        }

        public Builder exchangeRate(BigDecimal exchangeRate) {
            response.setExchangeRate(exchangeRate);
            return this;
        }

        public Builder convertedAmount(BigDecimal convertedAmount) {
            response.setConvertedAmount(convertedAmount);
            return this;
        }

        public ExchangeResponse build() {
            return response;
        }
    }

}