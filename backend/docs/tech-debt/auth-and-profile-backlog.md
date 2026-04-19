# Auth and Profile Tech Debt Backlog

This file tracks known technical debt around:
- authentication
- registration
- login
- current profile loading
- Keycloak integration
- profile-prefill into application creation

The goal is to keep these items visible and understandable, even if they are not fixed immediately.

---

## P1 — Important

### 1. Replace localStorage token storage
**Area:** frontend auth  
**Priority:** P1

**Problem**  
Access and refresh tokens are stored in `localStorage`, which is a security compromise and more vulnerable to XSS-related token leakage.

**Why it matters**  
This is acceptable for a learning/dev-oriented project, but not ideal for a production-grade auth flow.

**Done criteria**
- token storage approach is reviewed and replaced with a safer strategy
- auth flow still works for login / register / refresh / logout

---

### 2. Optimize `/api/auth/me`
**Area:** backend auth/profile  
**Priority:** P1

**Problem**  
`/api/auth/me` currently depends on Keycloak Admin API calls and admin token resolution, which is expensive for a current-user profile endpoint.

**Why it matters**  
This is workable now, but it is heavier than desirable and couples the app strongly to Keycloak admin operations.

**Done criteria**
- `/api/auth/me` is cheaper and simpler
- repeated admin-token/admin-user lookup chain is reduced or redesigned

---

### 3. Split `KeycloakAuthService`
**Area:** backend auth  
**Priority:** P1

**Problem**  
`KeycloakAuthService` currently handles too many responsibilities:
- login
- register
- refresh
- logout
- admin token access
- client secret resolution
- current profile loading
- role assignment
- profile mapping

**Why it matters**  
This makes the service harder to read, test, and safely modify.

**Done criteria**
- responsibilities are split into smaller services/components
- auth/token logic is separated from user admin/profile logic

---

### 4. Add product-level age validation
**Area:** backend + frontend validation  
**Priority:** P1

**Problem**  
Birth date validation currently prevents future dates, but does not enforce product/business rules such as minimum applicant age.

**Why it matters**  
A credit product usually requires real age validation, not only technical date validation.

**Done criteria**
- business age rule is defined
- backend validates it
- frontend reflects the same rule clearly

---

### 5. Add integration tests for auth/profile flow
**Area:** backend/frontend integration  
**Priority:** P1

**Problem**  
The auth/profile flow is already non-trivial, but does not appear to have dedicated integration coverage.

**Why it matters**  
Without tests, the following can break silently:
- register -> `/api/auth/me`
- login -> `/api/auth/me`
- refresh -> `/api/auth/me`
- profile field mapping
- current user prefill behavior

**Done criteria**
- integration tests cover register/login/refresh/current profile flow
- profile fields like `middleName` and `birthDate` are validated by tests

---

## P2 — Useful Improvements

### 6. Remove mutable module-level `currentUser`
**Area:** frontend auth  
**Priority:** P2

**Problem**  
`currentUser` is stored as mutable module-level state in `auth.service.ts`.

**Why it matters**  
This is easy to work with, but is not the cleanest source of truth and can become fragile over time.

**Done criteria**
- current user state is owned more explicitly by provider/store logic
- hidden module-level mutable state is reduced or removed

---

### 7. Implement or remove `minValidity` in refresh flow
**Area:** frontend auth  
**Priority:** P2

**Problem**  
`refreshToken(minValidity)` accepts a parameter that is currently unused.

**Why it matters**  
This makes the API misleading and suggests behavior that does not exist.

**Done criteria**
- either `minValidity` is implemented
- or the unused parameter is removed

---

### 8. Unify login/register page patterns
**Area:** frontend auth pages  
**Priority:** P2

**Problem**  
Login and register pages are now better than before, but could still be made more consistent in structure and helper usage.

**Why it matters**  
Shared auth patterns reduce drift and make future changes safer.

**Done criteria**
- login/register use consistent state/submit/error handling patterns
- shared small helpers are introduced only where useful

---

### 9. Remove legacy attribute fallback keys
**Area:** backend Keycloak profile mapping  
**Priority:** P2

**Problem**  
Some backward-compatible fallback keys are still used while reading profile attributes.

**Why it matters**  
Now that canonical keys are in place, old compatibility code can eventually become noise.

**Done criteria**
- canonical profile attribute keys are the only active path
- legacy fallback keys are removed after confirming migration stability

---

### 10. Further split `CreateApplication` page
**Area:** frontend application creation  
**Priority:** P2

**Problem**  
`CreateApplication.tsx` is cleaner now, but still contains a lot of logic:
- draft handling
- prefill logic
- parsing
- submit orchestration
- scoring form state

**Why it matters**  
Large page components become harder to maintain and review safely.

**Done criteria**
- page logic is split into smaller helpers/components/models where it actually helps

---

## P3 — Acceptable for Now

### 11. Harden Keycloak realm bootstrap/reset workflow
**Area:** infra / local development  
**Priority:** P3

**Problem**  
Realm import depends on container startup behavior and clean state expectations.

**Why it matters**  
This is acceptable for local/dev, but can confuse contributors when old volumes or old realms remain.

**Done criteria**
- local reset/recreate workflow is documented clearly
- Keycloak bootstrap behavior is easy to reproduce

---

### 12. Tighten unmanaged attribute policy
**Area:** Keycloak realm config  
**Priority:** P3

**Problem**  
`unmanagedAttributePolicy` may currently be broader than necessary.

**Why it matters**  
This is acceptable during development, but stricter managed-profile-only behavior may be preferable later.

**Done criteria**
- only intentionally supported profile attributes remain enabled
- unnecessary openness is reduced

---

### 13. Revisit long-term ownership of user profile data
**Area:** architecture  
**Priority:** P3

**Problem**  
User profile data is currently tightly coupled to Keycloak as both auth provider and profile storage source.

**Why it matters**  
This is fine for now, but long-term architecture may want a clearer boundary between auth identity and application-owned profile data.

**Done criteria**
- architecture decision is documented:
    - keep Keycloak as profile source
    - or move profile ownership elsewhere intentionally

---

## Suggested execution order

1. Add integration tests for auth/profile flow
2. Split `KeycloakAuthService`
3. Optimize `/api/auth/me`
4. Add real product-level age validation
5. Improve token storage strategy
6. Clean up frontend auth state ownership
7. Unify login/register page structure
8. Split `CreateApplication` further if needed
9. Remove legacy compatibility code after stabilization

---

## Notes

This backlog is intentionally focused on auth/profile/application-prefill related debt.
It is not a full-project backlog.