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
4. Downstream services never perform authentication; they simply accept the signed token as absolute proof of identity.

### Authorization ("What can you do?")
Authorization is handled at two levels: **The Edge (API Gateway)** and **The Node (Downstream Service)**.
1. **Coarse-Grained Authorization (Gateway):** The API Gateway inspects the token's `status` claim. If the status is `BANNED` or `SUSPENDED`, the Gateway blocks the request at the edge, protecting the internal network from malicious traffic.
2. **Fine-Grained Authorization (Downstream):** The downstream service (e.g., Wallet) reads the `roles` and `userId` claims. It uses the `userId` to scope data (e.g., "fetch wallet where user_id = X") and checks the `roles` to ensure the user has permission to perform specific actions (e.g., "only ADMIN can refund a transaction").

---

## 2. Trust via Cryptography (Sign & Verify)

We do not use a database to validate tokens. Instead, we use **Asymmetric Cryptography (RSA)**.

### Who Signs the Key? (The Private Key)
The **Auth Service** is the only entity that holds the **Private Key**. 
When a user logs in successfully, the Auth Service constructs the JWT payload and signs it using this Private Key (using the `RS256` algorithm). The signature guarantees that the token was minted by the Auth Service and hasn't been tampered with.

### Who Verifies the Key? (The Public Key)
The **API Gateway** (and optionally, downstream services) hold the corresponding **Public Key**. 
When a request arrives, the Gateway uses the Public Key to mathematically verify the signature on the JWT. 
*   **Decentralized Verification:** The Gateway does not need to ping the Auth Service to ask "Is this token valid?". It knows purely through math.
*   **Key Distribution:** The Auth Service exposes its public key via a `.well-known/jwks.json` endpoint so the Gateway can automatically download and cache it.

---

## 3. The JWT Structure

A JSON Web Token consists of three parts: `Header.Payload.Signature`.

### 1. Header
Specifies the algorithm and the Key ID (used for key rotation).
```json
{
  "alg": "RS256",
  "typ": "JWT",
  "kid": "auth-key-2024-v1"
}
```

### 2. Payload (Custom Claims)
Contains the user's identity and organizational context.
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

### 3. Signature
A cryptographic hash of the Header and Payload, signed with the Auth Service's Private Key.

---

## 4. Dependencies, Tools, and Architecture

To build this architecture, we will utilize the following stack (assuming a Spring Boot ecosystem):

### A. Auth Service (The Issuer)
*   **Spring Security:** Core security framework framework for managing login paths and password hashing (BCrypt).
*   **nimbus-jose-jwt / jjwt:** Java libraries for generating, signing, and managing JWTs using RSA algorithms.
*   **Vault / AWS Secrets Manager / Spring Cloud Config:** To securely store the RSA Private Key. Do not hardcode this in properties files.
*   **Database:** PostgreSQL/MySQL to store `users` and `organizations`.

### B. API Gateway (The Edge Enforcer)
*   **Spring Cloud Gateway:** To route requests to respective microservices.
*   **Spring Security WebFlux (OAuth2 Resource Server):** Configured to act as a Resource Server. It will be configured with a `jwk-set-uri` (pointing to the Auth Service or an API gateway local cache) to automatically resolve the public key and validate signatures.
*   **Custom Global Filter:** To extract the JWT claims (like `userId`, `tenantId`) and inject them as trusted HTTP Headers (e.g., `X-User-Id`) for the downstream services.

### C. Downstream Services (The Workers - Wallet, KYC)
*   **Spring Web:** They accept incoming requests from the Gateway.
*   **Filter / Interceptor:** Even though the Gateway validated the JWT, downstream services can use a simple interceptor to read the `X-User-Id` header to build their local `SecurityContext` or directly use it in their controllers.

---

## 5. Implementation Strategy & Plan

### Phase 1: Cryptographic Foundation
1.  Generate an RSA Key Pair (Private & Public).
2.  Store the Private Key securely in the Auth Service.
3.  Create a `/keys` or `/.well-known/jwks.json` endpoint on the Auth Service to expose the Public Key.

### Phase 2: Auth Service Development
1.  Implement the central `users` and `organizations` database schemas.
2.  Create the `/login` endpoint.
3.  Implement token generation: Assemble the payload with custom claims (`userId`, `status`, `tenantId`) and sign it with the Private Key.

### Phase 3: API Gateway Configuration
1.  Configure the Gateway as an OAuth2 Resource Server.
2.  Point the `jwk-set-uri` to the Auth Service's public key endpoint.
3.  Implement a security filter to reject tokens where `status != ACTIVE`.
4.  Implement a global routing filter to mutate incoming requests, extracting the JWT claims and appending them as `X-User-Id` and `X-Tenant-Id` headers.

### Phase 4: Downstream Service Refactoring
1.  Remove existing database-driven authentication logic from Wallet/KYC/Vouchers.
2.  Drop local `users` tables (following the Data Migration Plan).
3.  Update controllers/services to read identity entirely from the `X-User-Id` header provided by the Gateway.
4.  Update business logic to rely on the global UUID instead of local auto-incrementing IDs.

### Phase 5: Testing & Deployment
1.  Test token issuance and expiration.
2.  Test Gateway rejection of tampered tokens, expired tokens, or `BANNED` status claims.
3.  Test cross-service flows (Auth -> Gateway -> Wallet) to ensure the `uuid` propagates correctly.
