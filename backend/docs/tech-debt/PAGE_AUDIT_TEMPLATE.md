# Page Audit Template

Use this template to document one page/route at a time.

---

## Page / Route
Write the route and page name.

Example:
`/login — Login page`

---

## Purpose
What this page does from the product point of view.

Questions:
- Why does this page exist?
- What user task does it solve?
- Is it public or authenticated?

---

## Main flow
Describe the full flow from UI to backend and back.

Recommended format:
1. User action
2. Frontend component/page
3. Context/service/API call
4. Backend controller/service
5. Response
6. What is rendered or updated in UI

---

## Files involved
List the main files that implement this page.

Recommended sections:
- frontend page/component files
- frontend services/context/state files
- backend controller/service files
- config/infra files if relevant

---

## Business entities involved
What domain objects are used on this page?

Examples:
- user
- profile
- application
- offer
- document
- contract

---

## What is already good
List what is already implemented well.

Examples:
- clear flow
- good separation of concerns
- backend is source of truth
- validation is aligned
- no hidden localStorage dependency

---

## Problems / Tech debt
List issues found on this page.

For each item, briefly describe:
- what is wrong
- where it is
- why it matters

Recommended style:
- one issue per bullet or subheading

---

## Risk level
Classify the page and its changes.

Example:
- Low risk
- Medium risk
- High risk

Optional:
Explain which parts are most dangerous to change.

---

## Hidden dependencies
List anything indirect that this page depends on.

Examples:
- auth context
- current user profile
- localStorage draft
- Keycloak
- route guards
- backend validation
- imported config
- Docker/bootstrap setup

---

## Refactor strategy
Describe the safest order of refactoring.

Recommended structure:
1. Safe cleanup first
2. Medium-risk extraction/refactor
3. High-risk or architectural changes later

---

## Immediate fixes
List small changes that can be made now with low risk.

Examples:
- remove duplicate redirect
- extract helper
- improve parsing
- strengthen typing
- remove dead code

---

## Deferred improvements
List changes that are useful, but not necessary right now.

Examples:
- split large service
- redesign auth storage
- add integration tests
- move to stronger validation rules

---

## Acceptance criteria
What must still work after refactoring this page?

Examples:
- page renders correctly
- route behavior unchanged
- backend flow still works
- profile fields still load
- form submit still succeeds
- no new hidden dependencies introduced

---

## Manual test checklist
Write quick test steps for this page.

Example:
1. Open page
2. Fill form
3. Submit
4. Reload
5. Check resulting state
6. Check related page still works

---

## Related backlog items
Link to technical debt items or GitHub issues related to this page.

Example:
- Auth and profile backlog — P1.2
- Issue #17
- Issue #21