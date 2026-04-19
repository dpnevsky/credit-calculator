# Tech Debt Template

Use this template to document technical debt in a clear, reusable format.

---

## Title
Short and specific name of the technical debt item.

Example:
`Replace localStorage token storage`

---

## Area
Which part of the system this belongs to.

Examples:
- frontend auth
- backend auth
- profile flow
- application creation
- Keycloak integration
- infra
- testing

---

## Priority
Choose one:
- P1 — important
- P2 — useful improvement
- P3 — acceptable for now

---

## Problem
Describe what is wrong now.

Questions to answer:
- What is implemented poorly?
- What is fragile?
- What is inconsistent?
- What is too expensive / too coupled / too hard to maintain?

---

## Why it matters
Explain why this is technical debt and why it is worth tracking.

Examples:
- security risk
- performance issue
- maintainability problem
- bad developer experience
- inconsistent behavior
- hard to test
- too much coupling

---

## Current behavior
Describe how it works today.

Optional, but useful when the system already works and the debt is about quality, not correctness.

---

## Desired behavior
Describe what “better” looks like.

Do not overdesign.
Keep it practical and concrete.

---

## Done criteria
List the conditions under which this debt can be considered resolved.

Examples:
- auth tokens are no longer stored in localStorage
- `/api/auth/me` no longer depends on Keycloak Admin API for every request
- integration tests cover register/login/refresh/profile flow

---

## Risks / Notes
Anything important to remember:
- migration concerns
- backward compatibility
- dev/prod differences
- external system dependencies
- things intentionally postponed

---

## Related files
Optional list of the main files/modules affected.

Example:
- `frontend/src/services/auth.service.ts`
- `backend/apps/api-gateway/.../KeycloakAuthService.java`

---

## Related issues / PRs
Optional links or references.

Example:
- GitHub Issue #12
- PR #34