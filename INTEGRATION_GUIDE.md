# Downstream Service Integration Guide

This guide describes how downstream services (Wallet, KYC, Voucher, etc.) can integrate with the **Centralized Auth Service** to verify user identity and extract claims.

## 1. Authentication Flow Overview

Downstream services are **stateless**. They do not maintain their own user databases. Instead, they trust the JWT tokens issued by the Auth Service.

1.  Client requests the Auth Service for a token.
2.  Auth Service signs the JWT with its **Private Key**.
3.  Client sends the JWT in the `Authorization: Bearer <TOKEN>` header to your service.
4.  Your service uses the Auth Service's **Public Key** to verify the signature.

## 2. Shared JWT Identity Model

Each JWT contains the following claims (payload):

| Claim Name | Type | Description |
| :--- | :--- | :--- |
| `sub` | `String` | The user's email address. |
| `userId` | `UUID` | The globally unique identifier for the user. |
| `tenantId` | `UUID` | The identifier for the tenant/organization the user belongs to. |
| `status` | `String` | Current **User** status (`ACTIVE`, `SUSPENDED`, etc.). |
| `tenantStatus` | `String` | Current **Tenant** status (`ACTIVE`, `SUSPENDED`, etc.). |
| `roles` | `List<String>` | List of assigned roles (e.g., `["USER"]`, `["ADMIN"]`, `["TENANT_ADMIN"]`, `["SUPER_ADMIN"]`). |
| `iat` | `Long` | Issued At timestamp. |
| `exp` | `Long` | Expiration timestamp (typically 15 minutes). |

> [!IMPORTANT]
> Always check **both** `status` and `tenantStatus`. If either is not `ACTIVE`, the request should be rejected.

## 3. How to Get the Public Key (JWKS)

Our service uses **RS256** (RSA Signature with SHA-256). You do **not** hardcode the public key. Instead, you fetch it from our **JWKS (JSON Web Key Set)** endpoint.

*   **Endpoint URL**: `http://<AUTH_SERVICE_IP>:8080/.well-known/jwks.json`
*   **Method**: `GET`
*   **Format**: Standard RFC 7517 compliant JSON.

### Example Response:
```json
{
  "keys": [
    {
      "kty": "RSA",
      "use": "sig",
      "alg": "RS256",
      "kid": "auth-key-2026-04-03",
      "n": "...", 
      "e": "AQAB"
    }
  ]
}
```

## 4. Integration Steps (Spring Boot Example)

If you are using Spring Boot, you can simply use the `spring-boot-starter-oauth2-resource-server`.

1. **Update `application.yml`**:
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          jwk-set-uri: http://<AUTH_SERVICE_IP>:8080/.well-known/jwks.json
```

2. **Verify Claims in Controller**:
```java
@GetMapping("/my-data")
public ResponseEntity<?> getMyData(@AuthenticationPrincipal Jwt jwt) {
    String userId = jwt.getClaimAsString("userId");
    String tenantStatus = jwt.getClaimAsString("tenantStatus");
    
    if (!"ACTIVE".equals(tenantStatus)) {
        return ResponseEntity.status(403).body("Tenant account is suspended.");
    }
    
    return ResponseEntity.ok("Your user ID is: " + userId);
}
```

## 5. Security Checklist
- [ ] **Verify `exp`**: Ensure the token is not expired.
- [ ] **Verify `kid`**: Match the `kid` in the JWT header with the `kid` from the JWKS endpoint.
- [ ] **Verify `iss` (if set)**: Ensure the issuer matches the Auth Service URL.
- [ ] **Check Status**: Enforce the `tenantStatus` check on every sensitive request.
