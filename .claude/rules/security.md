# Security rules

Operationalizes `CLAUDE.md` §8, §13, §16. Enforced server-side, always.

## Non-negotiables

- Authorization is decided on the **server**. Never rely on frontend route
  guards, hidden fields, disabled buttons, or client-side role checks.
- Every authorization decision considers: user identity, role, organization,
  applicant access grant, application ownership, staff responsibility, and
  resource confidentiality.
- `User != Applicant`. A user may be authorized for zero, one, or many
  applicants. Check the access grant on every applicant-scoped request.

## Scholarship confidentiality (has automated-test requirement)

- A scholarship may internally belong to a partner university. Student-facing
  responses expose only: `title, country, degree, field, benefits, eligibility,
  requirements, deadline`.
- `university`, `partnership`, and any internal/operational/confidential fields
  are **staff-only**, returned through a separate DTO and a separate,
  authorization-checked endpoint.
- This must be enforced in the service/DTO layer, not by omitting fields in Vue.
- Required tests: an unauthorized student request for a scholarship cannot
  retrieve the university association or any confidential field. See
  `testing.md`.

## Application data

- The application lifecycle is auditable. Never overwrite significant historical
  state (stage, status, decision, assignment) without preserving prior state and
  an event record.

## Standard checks (before every commit)

- [ ] No hardcoded secrets — use env vars / secret manager; fail fast at startup
      if a required secret is missing.
- [ ] All external input validated (bean validation on DTOs + service guards).
- [ ] Parameterized MyBatis SQL only; no string-concatenated queries.
- [ ] XSS-safe output on both frontends; sanitize any stored HTML.
- [ ] CSRF protection intact for session-authenticated routes.
- [ ] Error responses do not leak stack traces, SQL, or internal identifiers.
- [ ] New/changed endpoint has an explicit authorization annotation or check.
- [ ] File uploads: type/size checks, stored outside MySQL, not web-served raw.

## If a security issue is found

Stop. Run the `nadoumi-security-reviewer` agent (or ECC `security-reviewer`).
Fix CRITICAL findings before continuing. Rotate exposed secrets. Sweep for the
same pattern elsewhere. Note it in `docs/SECURITY.md` if it changes a rule.

## Reference

`.claude/rules/ecc/common/security.md`, `.claude/rules/ecc/java/security.md`,
`.claude/rules/ecc/typescript/security.md`, `.claude/rules/ecc/vue/security.md`.
ECC skills: `springboot-security`, `security-review`, `security-scan`.
