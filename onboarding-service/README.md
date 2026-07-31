# Onboarding Service

Simple customer onboarding microservice for RupeeX.

## Current scope
- Create customer
- Get customer by id
- Get customer payment-eligibility status

## Run tests
```powershell
Set-Location "C:\Users\Administrator\Desktop\RupeeX-payment-processing"
.\mvnw.cmd -f ".\onboarding-service\pom.xml" test
```

## Run locally
```powershell
Set-Location "C:\Users\Administrator\Desktop\RupeeX-payment-processing"
.\mvnw.cmd -f ".\onboarding-service\pom.xml" spring-boot:run
```

Default port: `8090`

