# Bank Logging Spring Boot Starter

Spring Boot Starter pour logging centralisé avec conformité RGPD/PCI-DSS.

## Fonctionnalités

- 🔒 **Masquage RGPD** - PAN, IBAN, emails, téléphones
- 🔗 **Correlation ID** - Traçabilité inter-services
- 📝 **AOP Logging** - Logging automatique avec `@PaymentLog`
- 📊 **JSON Output** - Format ELK/Splunk ready

## Installation

```xml
<dependency>
    <groupId>com.bank</groupId>
    <artifactId>bank-logging-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Utilisation

```java
@Service
public class PaymentService {

    @PaymentLog(operation = "SEPA_TRANSFER", auditEnabled = true)
    public TransferResult executeTransfer(TransferRequest request) {
        // Logging automatique
        return result;
    }
}
```

## Configuration

```yaml
bank:
  logging:
    enabled: true
    masking:
      enabled: true
    aspect:
      enabled: true
      performance-threshold-ms: 1000
    correlation:
      enabled: true
      header-name: X-Correlation-ID
```

## Masquage

| Type | Input | Output |
|------|-------|--------|
| PAN | 4532015112830366 | 453201******0366 |
| IBAN | FR7630006000011234567890189 | FR76************0189 |
| Email | jean@email.com | j***@email.com |

## Build

```bash
mvn clean install
```
