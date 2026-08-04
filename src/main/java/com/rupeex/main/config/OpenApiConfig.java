package com.rupeex.main.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

/**
 * OpenAPI / Swagger Configuration for RupeeX Payment Platform.
 * 
 * Provides API documentation metadata, server configuration, and grouping of endpoints.
 * Access Swagger UI at: http://localhost:8080/api/swagger-ui.html
 * Access OpenAPI spec at: http://localhost:8080/api/v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI rupeexOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("RupeeX Payment Platform API")
                        .version("1.0.0")
                        .description("Comprehensive REST API for the RupeeX payment processing platform, " +
                                "supporting payments, account management, fraud detection, audit logging, " +
                                "and platform metrics.")
                        .contact(new Contact()
                                .name("RupeeX Development Team")
                                .email("support@rupeex.com")
                                .url("https://rupeex.com"))
                        .license(new License()
                                .name("Apache License 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(Arrays.asList(
                        new Server()
                                .url("http://localhost:8080/api")
                                .description("Local Development Server"),
                        new Server()
                                .url("http://localhost:8000/api")
                                .description("Docker Compose Development Server"),
                        new Server()
                                .url("https://api.rupeex.com")
                                .description("Production Server")
                ))
                .tags(Arrays.asList(
                        new Tag()
                                .name("Accounts")
                                .description("Account management endpoints"),
                        new Tag()
                                .name("Payments")
                                .description("Payment creation, retrieval, and status management"),
                        new Tag()
                                .name("Payment Audit")
                                .description("Payment history and audit logging"),
                        new Tag()
                                .name("Fraud Detection")
                                .description("Fraud rules and detection management"),
                        new Tag()
                                .name("Fraud Prevention")
                                .description("Fraud prevention and transaction verification"),
                        new Tag()
                                .name("Platform Metrics")
                                .description("System metrics and performance monitoring"),
                        new Tag()
                                .name("Payment Platform")
                                .description("Advanced payment platform operations"),
                        new Tag()
                                .name("Dead Letter Queue")
                                .description("Failed message handling and recovery"),
                        new Tag()
                                .name("Notifications")
                                .description("Notification management and delivery")
                ));
    }
}

