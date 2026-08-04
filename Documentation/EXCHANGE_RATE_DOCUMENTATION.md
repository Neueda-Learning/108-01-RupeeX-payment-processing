# Exchange Rate Module — Documentation

## Overview

The Exchange Rate module provides real-time currency conversion functionality for the RupeeX payment processing platform. It integrates with the [ExchangeRate-API](https://www.exchangerate-api.com/) (v6) to fetch live conversion rates and return the converted amount for any given currency pair.

---

## Architecture

```
HTTP Request
     │
     ▼
ExchangeRateController          ← REST layer  (controller/)
     │
     ▼
ExchangeRateService (interface) ← Contract    (service/)
     │
     ▼
ExchangeRateServiceImpl         ← Business logic (service/impl/)
     │
     ▼
ExchangeApiClient               ← External HTTP client (client/)
     │
     ▼
ExchangeRate-API (v6)           ← Third-party API
```

---

## File Reference

| Layer | File | Package |
|---|---|---|
| Controller | `ExchangeRateController.java` | `com.rupeex.main.controller` |
| Service Interface | `ExchangeRateService.java` | `com.rupeex.main.service` |
| Service Implementation | `ExchangeRateServiceImpl.java` | `com.rupeex.main.service.impl` |
| HTTP Client | `ExchangeApiClient.java` | `com.rupeex.main.client` |
| Request DTO | `ExchangeRequest.java` | `com.rupeex.main.dto` |
| Response DTO | `ExchangeResponse.java` | `com.rupeex.main.dto` |

---

## API Endpoint

### Convert Currency

**`POST /api/exchange/convert`**

Accepts a currency conversion request and returns the converted amount along with the applied exchange rate.

#### Request Body

```json
{
  "amount": 1000.00,
  "fromCurrency": "INR",
  "toCurrency": "USD"
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `amount` | `BigDecimal` | ✅ | The amount to convert |
| `fromCurrency` | `String` | ✅ | ISO 4217 source currency code (e.g., `INR`, `USD`, `EUR`) |
| `toCurrency` | `String` | ✅ | ISO 4217 target currency code |

#### Response Body (`200 OK`)

```json
{
  "originalAmount": 1000.00,
  "fromCurrency": "INR",
  "toCurrency": "USD",
  "exchangeRate": 0.012,
  "convertedAmount": 12.00
}
```

| Field | Type | Description |
|---|---|---|
| `originalAmount` | `BigDecimal` | The original amount passed in the request |
| `fromCurrency` | `String` | Source currency code |
| `toCurrency` | `String` | Target currency code |
| `exchangeRate` | `BigDecimal` | The live rate fetched from ExchangeRate-API |
| `convertedAmount` | `BigDecimal` | Final converted amount, rounded to 2 decimal places (HALF_UP) |

#### Error Response

If the external API fails, the currency is unsupported, or the API key is invalid, a `RuntimeException` is thrown with message `"Currency conversion failed"`.

```json
{
  "timestamp": "...",
  "status": 500,
  "error": "Internal Server Error",
  "message": "Currency conversion failed"
}
```

---

## DTOs

### `ExchangeRequest`

Plain Java class (no Lombok) with standard getters/setters.

```java
public class ExchangeRequest {
    private BigDecimal amount;
    private String fromCurrency;
    private String toCurrency;
}
```

### `ExchangeResponse`

Plain Java class implementing a manual **Builder pattern**.

```java
public class ExchangeResponse {
    private BigDecimal originalAmount;
    private String fromCurrency;
    private String toCurrency;
    private BigDecimal exchangeRate;
    private BigDecimal convertedAmount;

    // Builder: ExchangeResponse.builder()
    //             .originalAmount(...)
    //             .fromCurrency(...)
    //             .toCurrency(...)
    //             .exchangeRate(...)
    //             .convertedAmount(...)
    //             .build();
}
```

---

## Service Layer

### `ExchangeRateService` (Interface)

```java
public interface ExchangeRateService {
    ExchangeResponse convert(ExchangeRequest request);
}
```

### `ExchangeRateServiceImpl` (Implementation)

**Dependencies injected via constructor:**
- `ExchangeApiClient` — calls the external API
- `ObjectMapper` — parses the JSON response

**Conversion logic:**

1. Calls `ExchangeApiClient.getRates(fromCurrency)` to fetch all rates for the source currency.
2. Parses the JSON response and extracts the `conversion_rates` node.
3. Looks up the target currency rate from the `conversion_rates` map.
4. Multiplies `amount × rate`, rounds to **2 decimal places** using `RoundingMode.HALF_UP`.
5. Returns a populated `ExchangeResponse` via the builder.

**Error handling:**

| Condition | Exception | Message |
|---|---|---|
| `conversion_rates` node is missing | `IllegalStateException` | `"Exchange API response does not contain conversion_rates"` |
| Target currency not found in rates | `IllegalArgumentException` | `"Unsupported target currency: <code>"` |
| Any other failure (network, JSON parse, etc.) | `RuntimeException` | `"Currency conversion failed"` |

---

## HTTP Client

### `ExchangeApiClient`

Spring `@Component` using **Spring's `RestClient`** (introduced in Spring Boot 3.2).

**Constructor parameters (injected via `@Value`):**

| Property | Config Key | Default |
|---|---|---|
| Base URL | `exchange.api.url` | `https://v6.exchangerate-api.com/v6` |
| API Key | `exchange.api.key` | `demo-key` |

**Method:**

```java
public String getRates(String currency) ;
```

Calls:
```
GET https://v6.exchangerate-api.com/v6/{apiKey}/latest/{currency}
Accept: application/json
```

Returns the raw JSON response as a `String` for the service to parse.

---

## Configuration

Add the following keys to `src/main/resources/application.properties`:

```properties
# ExchangeRate-API Integration
exchange.api.url=https://v6.exchangerate-api.com/v6
exchange.api.key=YOUR_API_KEY
```

> ⚠️ **Never commit a real API key to the repository.** Use environment variables or a secret manager in production. Set `exchange.api.key` via an environment variable or Docker/Jenkins secret.

For local development, you can override via environment variable:
```powershell
$env:EXCHANGE_API_KEY = "your_real_key"
```

Or in `docker-compose.yml`:
```yaml
environment:
  - EXCHANGE_API_KEY=${EXCHANGE_API_KEY}
```

And bind it in `application.properties`:
```properties
exchange.api.key=${EXCHANGE_API_KEY:demo-key}
```

---

## Supported Currencies

All currencies supported by [ExchangeRate-API v6](https://www.exchangerate-api.com/docs/supported-currencies) are supported. Common examples:

| Code | Currency |
|---|---|
| `INR` | Indian Rupee |
| `USD` | US Dollar |
| `EUR` | Euro |
| `GBP` | British Pound |
| `AED` | UAE Dirham |
| `SGD` | Singapore Dollar |
| `JPY` | Japanese Yen |

---

## Example Usage (cURL)

```bash
curl -X POST http://localhost:8080/api/exchange/convert \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 5000,
    "fromCurrency": "INR",
    "toCurrency": "USD"
  }'
```

**Example Response:**
```json
{
  "originalAmount": 5000,
  "fromCurrency": "INR",
  "toCurrency": "USD",
  "exchangeRate": 0.012,
  "convertedAmount": 60.00
}
```

---

## Testing

Currently, no dedicated unit tests exist for the exchange rate module. The following tests are recommended:

### Unit Tests — `ExchangeRateServiceImplTest`

| Test Case | Description |
|---|---|
| `convert_validRequest_returnsConvertedAmount` | Happy path — valid currency pair and amount |
| `convert_unsupportedTargetCurrency_throwsIllegalArgumentException` | Target currency not in rates map |
| `convert_missingConversionRates_throwsIllegalStateException` | API response missing `conversion_rates` key |
| `convert_apiClientThrows_throwsRuntimeException` | External API call fails (network error) |
| `convert_amountRoundedToTwoDecimalPlaces` | Verify `HALF_UP` rounding is applied |

### Integration Tests — `ExchangeRateControllerTest`

| Test Case | Description |
|---|---|
| `POST /api/exchange/convert` with valid body returns `200 OK` | Full controller + service mock |
| `POST /api/exchange/convert` with bad currency returns `500` | Error propagation |

### How to run tests

```powershell
# From project root
.\mvnw.cmd test
```

---

## Payment Service Integration

The `Payment` entity (`com.rupeex.main.entity.Payment`) contains an `exchangeRate` field (`BigDecimal`) which stores the rate that was applied at the time of a cross-currency payment.

The exchange rate module can be used by the payment processing pipeline to:
1. Fetch the live rate before processing.
2. Store the applied rate on the `Payment` record for audit/reconciliation.

---

## Sequence Diagram

```
Client          ExchangeRateController   ExchangeRateServiceImpl   ExchangeApiClient   ExchangeRate-API
  │                      │                        │                       │                    │
  │  POST /convert       │                        │                       │                    │
  │─────────────────────►│                        │                       │                    │
  │                      │  convert(request)       │                       │                    │
  │                      │───────────────────────►│                       │                    │
  │                      │                        │  getRates(fromCurrency)│                    │
  │                      │                        │───────────────────────►                    │
  │                      │                        │                       │  GET /latest/{cur} │
  │                      │                        │                       │───────────────────►│
  │                      │                        │                       │   JSON response    │
  │                      │                        │                       │◄───────────────────│
  │                      │                        │◄───────────────────────                    │
  │                      │                        │  parse + calculate     │                    │
  │                      │◄───────────────────────│                       │                    │
  │  ExchangeResponse    │                        │                       │                    │
  │◄─────────────────────│                        │                       │                    │
```

---

## Related Files

- `src/main/resources/application.properties` — API URL and key configuration
- `src/main/java/com/rupeex/main/entity/Payment.java` — contains `exchangeRate` field
- `Documentation/EXCHANGE_RATE_DOCUMENTATION.md` — this file

---

*Last updated: 2026-08-04*

