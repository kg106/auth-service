# Centralized Stateless JWT Architecture: Technical Overview

This document provides a clear explanation of our move toward a **Centralized, Stateless Identity Architecture**. This design decouples identity management from business logic, ensuring scalability and security across all microservices (Wallet, KYC, Vouchers, etc.).

---

## 1. Core Architectural Concept

### The Problem: Tightly Coupled Identity
Currently, every microservice (Wallet, KYC) maintains its own users table. If a user is banned, every service must be notified. If a user changes their email, every database must be updated. This is fragile and hard to sync.

### The Solution: Stateless Identity
We move to a **"One Source, Many Consumers"** model:

*   **Auth Service (The Control Plane):** The **ONLY** service that owns the `users` and `organizations` tables. It handles registration, login, and profile updates.
*   **Downstream Services (The Data Plane):** Services like `Wallet` or `KYC` delete their users tables. They identify users purely via a verified JWT (JSON Web Token).
*   **Trust via Math, Not Queries:** Downstream services don't ask the Auth Service "Is this token valid?" for every request. Instead, they use a **Public Key (JWKS)** to mathematically verify the token's signature locally.

---

## 2. Auth Service Specifications

To support this migration, the Central Auth Service must be the definitive source for the following entities.

### A. Organization Entity (Centralized)

| Field | Type | Description |
| :--- | :--- | :--- |
| `id` | UUID / String | The global `tenantId`. This is the FK used by all services. |
| `name` | String | Legal name of the organization. |
| `org_code` | String | Human-readable unique mnemonic (e.g., "AZILEN-IND"). |
| `status` | Enum | `ACTIVE`, `SUSPENDED`, `INACTIVE`. |

### B. User Entity (Centralized)

| Field | Type | Description |
| :--- | :--- | :--- |
| `id` | UUID / String | The global `userId`. This is the FK used by all services. |
| `email` | String | The **Anchor Field**. Used to map old data to the new system. |
| `password_hash` | String | Encrypted password (stored **ONLY** here). |
| `tenant_id` | UUID | Foreign Key to the Organization. |
| `roles` | `List<String>` | e.g., `["ROLE_USER", "ROLE_ADMIN"]`. |
| `status` | Enum | `ACTIVE`, `BANNED`, `PENDING_VERIFICATION`. |

---

## 3. The Identity Carrier: The JWT

Every request to the ecosystem must carry a JWT. The Auth Service injects the following **Custom Claims** into the payload:

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

### Why this works:
*   **Decoupling:** `Wallet` service only looks at the `userId` claim. It doesn't care about the user's password or city.
*   **Security:** If a user is **BANNED**, the `status` claim changes. The API Gateway or the service's Security Filter rejects the token immediately without a DB lookup.

---

## 4. Migration Strategy: "The Bridge Pattern"

How do we link an old Wallet (with `user_id = 45`) to a new Auth user (with `userId = "550e8400..."`)?

1.  **Selection:** We use `email` as the common denominator across all services.
2.  **Mapping Table:** We create a temporary mapping: `Old_Service_ID (45) + Email (abc@x.com) -> New_Auth_UUID (550e8400...)`
3.  **Backfill:**
    *   Add a temporary `user_uuid` column to the Wallet table.
    *   Update the `user_uuid` by joining the Wallet table to the Mapping Table.
    *   Once verified, drop the old Long `user_id` and rename `user_uuid` to `user_id`.

---

## 5. Summary for Colleagues

*   **Auth Service** is the **brain** (Storage).
*   **API Gateway** is the **gatekeeper** (Validation & Status Enforcement).
*   **Wallet/KYC** are the **workers** (Stateless logic using only the string `userId` from the token).

No more database sync issues. Identity is verified mathematically.
