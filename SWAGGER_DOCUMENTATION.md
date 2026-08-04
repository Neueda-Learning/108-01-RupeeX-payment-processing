# Swagger API Documentation Integration

## Overview

This document provides information about the Swagger/OpenAPI integration for the RupeeX Payment Processing Platform API. Swagger API documentation has been fully integrated, providing interactive API exploration and testing capabilities.

## Accessing Swagger UI

### Local Development
- **Swagger UI**: http://localhost:8080/api/swagger-ui.html
- **OpenAPI JSON Spec**: http://localhost:8080/api/v3/api-docs
- **OpenAPI YAML Spec**: http://localhost:8080/api/v3/api-docs.yaml

### Docker Compose
- **Swagger UI**: http://localhost:8000/api/swagger-ui.html
- **OpenAPI JSON Spec**: http://localhost:8000/api/v3/api-docs

### Production
- **Swagger UI**: https://api.rupeex.com/swagger-ui.html
- **OpenAPI JSON Spec**: https://api.rupeex.com/v3/api-docs

## Features

### 1. **Comprehensive API Documentation**
   - All REST endpoints are fully documented with descriptions
   - Request and response schemas are clearly defined
   - Parameter examples are provided

### 2. **Interactive Testing**
   - "Try it out" feature allows testing API endpoints directly from the UI
   - Real-time request/response display
   - cURL command generation

### 3. **Organized Endpoints**
   All endpoints are grouped into logical categories (Tags):

   - **Accounts**: Account management operations
   - **Payments**: Payment creation, retrieval, and status management
   - **Payment Audit**: Payment history and audit logging
   - **Fraud Detection**: Fraud rules and detection management
   - **Fraud Prevention**: Fraud prevention and transaction verification
   - **Platform Metrics**: System metrics and performance monitoring
   - **Payment Platform**: Advanced payment platform operations
   - **Dead Letter Queue**: Failed message handling and recovery
   - **Notifications**: Notification management and delivery

## API Endpoints Summary

### Accounts Endpoints
```
GET    /api/accounts                           - Get all accounts
GET    /api/accounts/{accountNumber}           - Get account by number
POST   /api/accounts                           - Create account
```

### Payment Endpoints
```
POST   /api/legacy/payments                    - Create payment
GET    /api/legacy/payments/{paymentId}        - Get payment by ID
PATCH  /api/legacy/payments/{paymentId}/status - Update payment status
POST   /api/legacy/payments/{paymentId}/verification-decision - Process verification decision
```

### Payment Platform Endpoints
```
POST   /api/payments                           - Create payment (platform)
GET    /api/payments                           - Get paginated payments
GET    /api/payments/{id}                      - Get payment by ID
POST   /api/payments/{id}/retry                - Retry failed payment
POST   /api/payments/{id}/cancel               - Cancel payment
GET    /api/payments/{id}/history              - Get payment history
```

### Payment Audit Endpoints
```
GET    /api/audit/payments/{paymentId}/logs    - Get payment audit logs
```

### Fraud Detection Endpoints
```
GET    /api/fraud/rules                        - Get all fraud rules
POST   /api/fraud/rules                        - Create fraud rule
PUT    /api/fraud/rules/{id}                   - Update fraud rule
DELETE /api/fraud/rules/{id}                   - Delete fraud rule
```

### Platform Metrics Endpoints
```
GET    /api/metrics                            - Get metrics snapshot
GET    /api/dashboard                          - Get dashboard summary
GET    /api/events                             - Get recent system events
GET    /api/health                             - Get service health status
```

### Dead Letter Queue Endpoints
```
GET    /api/dlq                                - Get all DLQ entries
```

### Notification Endpoints
```
POST   /api/notifications/test                 - Send test notification
```

## Configuration

The Swagger configuration is managed through:

1. **OpenAPI Configuration Bean** (`src/main/java/com/rupeex/main/config/OpenApiConfig.java`)
   - Defines API metadata (title, version, description)
   - Configures server endpoints
   - Organizes endpoints into tags

2. **Application Properties** (`src/main/resources/application.properties`)
   ```properties
   springdoc.api-docs.path=/v3/api-docs
   springdoc.swagger-ui.path=/swagger-ui.html
   springdoc.swagger-ui.enabled=true
   springdoc.swagger-ui.tagsSorter=alpha
   springdoc.swagger-ui.operationsSorter=method
   springdoc.swagger-ui.try-it-out-enabled=true
   ```

3. **Controller Annotations**
   - `@Tag`: Groups endpoints by resource/feature
   - `@Operation`: Describes what each endpoint does
   - `@ApiResponse`: Documents HTTP response codes
   - `@Parameter`: Describes path, query, and request parameters
   - `@RequestBody`: Describes request body schema

## Example Usage

### Using Swagger UI to Test an API

1. Navigate to http://localhost:8080/api/swagger-ui.html
2. Locate the desired endpoint (e.g., GET /payments)
3. Click on the endpoint to expand it
4. Click "Try it out" button
5. Enter any required parameters
6. Click "Execute" button
7. View the response under "Response"

### Getting OpenAPI Specification

The OpenAPI specification can be retrieved in multiple formats:

**JSON Format:**
```bash
curl http://localhost:8080/api/v3/api-docs
```

**YAML Format:**
```bash
curl http://localhost:8080/api/v3/api-docs.yaml
```

## Integration Details

### Dependencies
- **springdoc-openapi-starter-webmvc-ui** (v2.6.0)
  - Provides Swagger UI and OpenAPI support for Spring Boot 3.x
  - No separate Springdoc UI build required
  - Auto-configured by Spring Boot

### Annotation Usage

All controllers now include:

1. **Class-level annotations**:
   ```java
   @Tag(name = "Payments", description = "Payment operations")
   public class PaymentController { }
   ```

2. **Method-level annotations**:
   ```java
   @Operation(summary = "Create payment", description = "Create a new payment")
   @ApiResponses(value = {
       @ApiResponse(responseCode = "201", description = "Payment created"),
       @ApiResponse(responseCode = "400", description = "Invalid request")
   })
   public PaymentResponse createPayment(@RequestBody PaymentRequest request) { }
   ```

3. **Parameter annotations**:
   ```java
   @Parameter(description = "Payment ID", example = "12345", required = true)
   @PathVariable Long paymentId
   ```

## Best Practices

1. **Keep Descriptions Clear**: Write concise, meaningful descriptions for all endpoints
2. **Document Response Codes**: Always include success and error response codes
3. **Provide Examples**: Use the `example` attribute for parameters
4. **Document DTOs**: Add Swagger annotations to request/response DTOs for better schema documentation
5. **Regular Updates**: Keep Swagger documentation in sync with API changes

## Troubleshooting

### Swagger UI Not Showing
- Ensure `springdoc.swagger-ui.enabled=true` in application.properties
- Check that the context path is correct (default: `/api`)
- Verify the application is running on the correct port

### Endpoints Not Appearing
- Ensure controllers have `@RestController` and `@RequestMapping` annotations
- Check that endpoints have proper HTTP method annotations (`@GetMapping`, `@PostMapping`, etc.)
- Verify endpoints are public and not marked as private

### Schema Issues
- Add `@Schema` annotations to DTO classes for better documentation
- Ensure DTOs have proper getter methods (required for schema inspection)
- Use `@NotNull`, `@Valid` annotations from `jakarta.validation`

## Integration with CI/CD

The Swagger/OpenAPI documentation is automatically generated during the build process. The OpenAPI spec can be:
- Validated in CI pipelines
- Used to generate SDKs automatically
- Compared against previous versions for backward compatibility

## Frontend Integration

The OpenAPI specification can be used to:
- Generate TypeScript/JavaScript clients automatically using tools like OpenAPI Generator
- Keep frontend and backend APIs in sync
- Auto-document frontend API integration

Example command to generate a TypeScript client:
```bash
npx @openapitools/openapi-generator-cli generate \
  -i http://localhost:8080/api/v3/api-docs \
  -g typescript-fetch \
  -o ./generated-client
```

## References

- [SpringDoc OpenAPI Documentation](https://springdoc.org/)
- [OpenAPI Specification](https://spec.openapis.org/oas/v3.0.3)
- [Swagger UI Documentation](https://swagger.io/tools/swagger-ui/)

## Support

For issues with Swagger integration, check:
1. Spring Boot and Springdoc versions compatibility
2. Annotation usage on controllers and methods
3. DTO class structure and annotations
4. Application properties configuration

