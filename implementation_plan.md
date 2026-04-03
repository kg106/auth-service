# Auth Service Bug Fixes & Enhancements

Fix Swagger UI access, 401 on register/login from external machines, add the missing `ROLE_TENANT_ADMIN` role, update the role mapping SQL, and include `tenantStatus` in the JWT.

## Proposed Changes

---

### 1. Security Config (Fix Swagger + External IP 401)

#### [MODIFY] [SecurityConfig.java](file:///home/tithaal.sheth/Documents/Centralized%20auth%20service%20/auth-service/src/main/java/com/example/auth_service/config/SecurityConfig.java)

**Root cause of 401 from external IP**: Spring Security auto-configures HTTP Basic authentication. When a request hits from another machine, Spring sends a `WWW-Authenticate: Basic` challenge (401) before the permit rules can fire — because `httpBasic()` is never explicitly disabled.

**Root cause of Swagger 401**: The `springdoc` Swagger UI also needs `/swagger-ui/index.html` and `/v3/api-docs/swagger-config` whitelisted, plus `formLogin` and `httpBasic` must be disabled.

Changes:
- Explicitly disable `httpBasic` and `formLogin`
- Add missing Swagger paths: `/swagger-ui/index.html`, `/v3/api-docs/swagger-config`
- Add CORS permissive config so cross-machine requests aren't blocked

---

### 2. Role Table: Add ROLE_TENANT_ADMIN

**Role mapping across the 4 services:**

| Auth Role | KYC | OrderMgmt | Wallet | Voucher |
|---|---|---|---|---|
| `ROLE_USER` (id=1) | [User](file:///home/tithaal.sheth/Documents/Centralized%20auth%20service%20/auth-service/src/main/java/com/example/auth_service/entity/User.java#10-60) | `CUSTOMER` | `ROLE_USER` | `USER` |
| `ROLE_ORG_ADMIN` (id=2) | `Default_Admin` | `ADMIN` | `ROLE_ORG_ADMIN` | *(NA)* |
| `ROLE_TENANT_ADMIN` (id=4) | `Tenant_Admin` | `ORG_ADMIN` | *(NA)* | `TENANT_ADMIN` |
| `ROLE_SUPER_ADMIN` (id=3) | `Super_Admin` | `SUPER_ADMIN` | `ROLE_SUPER_ADMIN` | `PLATFORM_ADMIN` |

#### [MODIFY] [auth_schema.sql](file:///home/tithaal.sheth/Documents/Centralized%20auth%20service%20/auth-service/src/main/resources/auth_schema.sql)
- Add [(4, 'ROLE_TENANT_ADMIN')](file:///home/tithaal.sheth/Documents/Centralized%20auth%20service%20/auth-service/src/main/java/com/example/auth_service/entity/User.java#10-60) to the seed INSERT

#### [MODIFY] [05_seed_roles.sql](file:///home/tithaal.sheth/Documents/Centralized%20auth%20service%20/auth-service/src/main/resources/migration/05_seed_roles.sql)
- Add [(4, 'ROLE_TENANT_ADMIN')](file:///home/tithaal.sheth/Documents/Centralized%20auth%20service%20/auth-service/src/main/java/com/example/auth_service/entity/User.java#10-60) to the INSERT
- Update the CASE statements for all 4 services to correctly map `Tenant_Admin`, `ORG_ADMIN`, `TENANT_ADMIN` → role id 4

---

### 3. JWT Token: Add Tenant Status

#### [MODIFY] [JwtTokenProvider.java](file:///home/tithaal.sheth/Documents/Centralized%20auth%20service%20/auth-service/src/main/java/com/example/auth_service/security/JwtTokenProvider.java)
- Add `claim("tenantStatus", user.getTenant().getStatus())` so downstream services can enforce tenant suspension/inactivity at the JWT level without a DB roundtrip.
- Rename existing `"status"` claim to `"userStatus"` for clarity (or keep it alongside).

> [!IMPORTANT]
> Renaming `"status"` → `"userStatus"` in the JWT payload is a **breaking change** for any downstream services already reading `"status"`. Confirm if you want to rename it or just add `"tenantStatus"` as a new claim alongside the existing `"status"`.

---

## Verification Plan

### Manual Verification (no existing tests in the project)

1. **Restart the server** after changes and confirm it starts clean.

2. **Swagger UI from external machine:**
   - From another machine on the network: `curl -v http://<YOUR_IP>:8080/swagger-ui/index.html`
   - Expected: `200 OK` with HTML content (previously returned `401`)

3. **Register from external machine:**
   - `curl -v -X POST http://<YOUR_IP>:8080/api/v1/auth/register -H "Content-Type: application/json" -d '{"email":"test@x.com",...}'`
   - Expected: `200 OK` (previously returned `401`)

4. **Verify ROLE_TENANT_ADMIN in DB:**
   - Run in psql: `SELECT * FROM auth.roles ORDER BY id;`
   - Expected: 4 rows with ids 1, 2, 3, 4

5. **JWT claims after login:**
   - Login and decode the JWT at [jwt.io](https://jwt.io)
   - Expected payload to contain both `"status"` (user status) and `"tenantStatus"` (tenant status)
