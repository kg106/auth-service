# Detailed Microservice Authentication Flow & Implementation Guide

This document provides a comprehensive deep-dive into our **Centralized Stateless Authentication Architecture**, detailing the mechanics of authentication, authorization, cryptographic trust, and the implementation strategy.

---

## 1. Authentication vs. Authorization Mechanics

In a distributed microservice environment, it is critical to separate the concepts of "Who are you?" (Authentication) and "What can you do?" (Authorization).

### Authentication ("Who are you?")
Authentication is handled **exclusively** by the Auth Service.
1. The user submits their credentials (e.g., email and password) to the Auth Service.
2. The Auth Service queries its database to verify the credentials.
3. Upon success, it generates a JWT containing the user's identity and state.
4. Downstream services never perform authentication; they simply accept the signed token as proof of identity.

### Authorization ("What can you do?")
Authorization is handled at multiple levels to ensure **Defense-in-Depth**.
1. **Coarse-Grained Authorization (Gateway):** The API Gateway inspects the token's `status` claim. If the status is `BANNED` or `SUSPENDED`, the Gateway blocks the request at the edge.
2. **Fine-Grained Authorization (Downstream):** The downstream service (e.g., Wallet) reads the `roles` and `userId` claims. It uses the `userId` to scope data and checks `roles` for specific action permissions.

---

## 2. Trust via Cryptography (Defense-in-Depth)

> [!WARNING]
> **Never rely solely on HTTP Headers (e.g., X-User-Id) forwarded by the Gateway.** 
> If a service is accidentally exposed or the internal network is compromised, headers can be faked. We implement **Defense-in-Depth** by validating the JWT at both the Edge and the Service level.

### Who Signs the Key? (The Private Key)
The **Auth Service** holds the **Private Key** and signs the JWT using the `RS256` algorithm.

### Who Verifies the Key? (The Public Key)
Both the **Gateway** and **Downstream Services** use the **Public Key** to verify the signature.

#### Recommended Security Models:
*   **Option A (Balanced):** Gateway performs full validation (signature + expiration + status). Downstream services also validate the signature to ensure the token was issued by our Auth Service, preventing header injection attacks.
*   **Option B (High Security):** Every microservice acts as a full **OAuth2 Resource Server**, validating the JWT independently. This is the preferred model for enterprise-grade security.

---

## 3. The JWT Structure

A JSON Web Token consists of three parts: `Header.Payload.Signature`.

### 1. Header
```json
{
  "alg": "RS256",
  "typ": "JWT",
  "kid": "auth-key-2024-v1"
}
```

### 2. Payload (Custom Claims)
```json
{
  "sub": "user@example.com",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "tenantId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "status": "ACTIVE",
  "roles": ["ROLE_USER"],
  "iat": 1711883177,
  "exp": 1711884077
}
```

---

## 4. Dependencies, Tools, and Architecture

### A. Auth Service (The Issuer)
*   **Spring Security:** Core security framework.
*   **nimbus-jose-jwt / jjwt:** Libraries for JWT generation and RS256 signing.
*   **Vault / AWS Secrets Manager:** Secure storage for the RSA Private Key.

### B. API Gateway (Edge Protection)
*   **Spring Cloud Gateway:** Routing and edge filtering.
*   **Spring Security WebFlux (OAuth2 Resource Server):** Validates the JWT signature, expiration, and status claims.
*   **JWKS Endpoint:** Fetches public keys from the Auth Service.

### C. Downstream Services (The Data Plane)
*   **Spring Security (OAuth2 Resource Server):** Even behind the gateway, services maintain their own security configuration to validate JWTs locally.
*   **Security Context:** Claims (like `userId`) are extracted from the validated JWT and populated into the `SecurityContextHolder`, ensuring the application logic only ever works with verified identities.

---

## 5. Implementation Strategy & Plan

### Phase 1: Cryptographic Foundation
1.  Generate an RSA Key Pair.
2.  Store Private Key in Auth Service (Securely).
3.  Expose Public Key via `/.well-known/jwks.json` on the Auth Service.

### Phase 2: Auth Service Development
1.  Implement `/login` and token generation with custom claims.
2.  Sign tokens with the RS256 algorithm.

### Phase 3: API Gateway Configuration
1.  Configure as a Resource Server using the Auth Service's JWKS endpoint.
2.  Implement filters for status-based rejection (`BANNED`, etc.).

### Phase 4: Downstream Service Hardening
1.  Configure each microservice (Wallet, KYC, etc.) as an **OAuth2 Resource Server**.
2.  Implement local JWT signature validation using the same JWKS endpoint.
3.  **Discard trust in plain HTTP headers**; always derive `userId` from the validated JWT claims.

### Phase 5: Verification
1.  **Direct Access Test:** Attempt to call a service directly with a spoofed header; it must fail due to a missing/invalid JWT.
2.  **Signature Test:** Attempt to use a token signed with a different key; it must fail at both the Gateway and the Service level.
