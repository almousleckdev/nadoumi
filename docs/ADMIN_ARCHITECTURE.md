# Nadoumi — Admin Architecture

> Read `docs/PLATFORM_ARCHITECTURE.md` first — the approved enterprise-platform direction this doc rolls up to (2026-09-02).

Status: **BASELINE** · **EXISTING** · **PLANNED** · **OPEN**.

The admin is Nadoumi's internal operations console. **Baseline UI = `RuoYi-Vue3`**
(D1; `docs/FRONTEND_ARCHITECTURE.md` §3.1), delivered as `nadoumi-admin/`. The
restored Vue 2 `ruoyi-ui` is transitional/reference only — **no Nadoumi admin screen
is built on Vue 2**. Full staff permission detail: `docs/PERMISSION_CATALOGUE.md`.

---

## 1. What exists today (EXISTING)

### 1.0 Two frontends, one target — authoritative

Verified 2026-09-02 (see `docs/ARCHITECTURE_GAP_ANALYSIS.md` §2):

| | `nadoumi-admin` — **the Nadoumi admin** | `ruoyi-ui` — reference only, frozen |
| --- | --- | --- |
| Dev URL | **`http://localhost:8082`** | **`http://localhost:1024`** (wants `:80`) |
| Stack | Vue 3 + Vite + TS + Element Plus + Pinia | Vue 2 + vue-cli-service + Element UI (stock RuoYi) |
| Nadoumi screens | **Dashboard** (real, live applicant metrics) + **Applicants** (real list/create/archive) | none |
| RuoYi platform screens | none yet — unknown `component` → `views/placeholder.vue` | all implemented |
| Nadoumi commits | 2 | 2 tweaks on top of 696 upstream |

**Decision (recorded):** `nadoumi-admin` is the single Nadoumi admin application;
all business UI is built there. `ruoyi-ui` is reference-only and frozen — no
Nadoumi feature work, kept runnable until `nadoumi-admin` covers the RuoYi
platform screens operations need (§2), then removed in a dedicated change. It is
**not** part of the default environment "up"; start it with `cd ruoyi-ui && npm
run dev` when you need to consult a stock screen.

### 1.1 Shared backend-driven shell

Both apps are driven by the same backend contract:

- **Login** → JWT; **`/getInfo`** → user/roles/permissions;
  **`/getRouters`** → dynamic sidebar built from `sys_menu`.
- **Permission directives / guards** are UX-only; the server re-checks every call.
- **Menu/permission data** lives in `sys_menu` (`M`/`C`/`F`), assigned to roles
  via `sys_role_menu` (Flyway `V2` seed, `V6` English relabel).

`nadoumi-admin` resolves each backend route's `component` string against
`src/views/**/*.vue`; anything unmatched renders `placeholder.vue`
("This screen is not part of the Nadoumi admin yet") — a **transitional** state,
never counted as a delivered feature.

`ruoyi-ui` implements the full stock set: `System` (User, Role, Menu, Dept, Post,
Dict, Config, Notice, Log); `Monitor` (Online, Job, Druid, Server, Cache, Operlog,
Logininfor); `Tool` (Gen, Form Build, Swagger) — the reference for porting each
into `nadoumi-admin`.

Seed roles: `admin` (all), `common`, `nadoumi_super_admin`. Seed users: `almousleck`
(day-to-day super-admin, `user_type='00'`, role `nadoumi_super_admin`); `admin`
(`user_id=1`) is **disabled** (`status='1'`) as the break-glass account. **Revision 2**
removes the RuoYi "your password is still the initial password" prompt
(`sys.account.initPasswordModify → 0`; the `ruoyi-ui` nag deleted;
`SysLoginController.initPasswordIsModify` returns `false`) — forced first-login
rotation for `almousleck` is carried by password expiry (`pwd_update_date IS NULL`
+ `passwordValidateDays = 90`). Credentials are never in UI, logs, source, or
production-facing docs (`docs/SECURITY.md` §7).

The full internal business platform (dashboard tiles + module list matching the
product brief — Applications / Pending Review / New Applicants / Accepted / Rejected
/ Revenue / Expenses / Net Earnings / Outstanding Payments / Employees / Recent
Applications / Recent Activity / Operational Tasks / Alerts; modules for Applicants,
Applications, Universities, Programs, Scholarships, Documents, Employees, Roles &
Permissions, Marketing/CMS, Partnerships, Finance, Payments, Invoices, Expenses,
Revenue/Earnings, Payroll, Notifications, Communication, Reports & Analytics, System
Settings) is specified in **§2 and §6–7 as PLANNED architecture** and is **not built
in the nadoumi-web build**.

## 2. Target admin scope for Nadoumi

### 2.0 Module status (keep current)

| Module | Status | Notes |
| --- | --- | --- |
| Dashboard | **IMPLEMENTED** | Real applicant KPIs + honest coming-soon (§6.1). |
| Applicants — list | **IMPLEMENTED** | Search / status / nationality filter, pagination, archive. `GET /api/staff/applicants`. |
| Applicants — detail | **IMPLEMENTED** | Overview + Education + Test scores + Contacts + Access tabs. |
| Applicant — profile edit | **IMPLEMENTED** | Drawer form → `PUT /api/staff/applicants/{id}` (`nad:applicant:edit`). PII fields hidden/locked without `nad:applicant:pii:view`. |
| Applicant — education / test scores / contacts CRUD | **IMPLEMENTED** | Add / edit / remove via drawer forms; staff `POST` / `PUT` / `DELETE` on each sub-resource (`nad:applicant:edit`). |
| Applicant — access / delegation | **IMPLEMENTED** (read) | Access tab lists grants from `GET /api/staff/applicants/{id}/access` (`nad:applicant:access:view`). Grant / revoke / transfer endpoints exist (`nad:applicant:access:manage`) — **CURRENT** to surface in the UI. Grantee name (vs `User #id`) needs a `sys_user` join — follow-up. |
| Universities — list + detail + CRUD | **IMPLEMENTED** | `nadoumi-university` module, `nad_university` + full profile depth (V10) + `nad_university_ranking` / `nad_university_highlight`, `/api/staff/universities` (+ public `/api/public/universities`), `nad:university:list/view/create/edit/remove`. Unique `(name, country)`. Sectioned drawer (Identity / Profile / About / Highlights & advantages / Rankings / Publication); detail page renders every section + a `publish_status` badge. Gallery deferred to the Document slice (Step 7). |
| Programs / Scholarships | **CURRENT** — Phase B | No backend yet. Programs hang off `nad_university`. |
| Applications (+ timeline / tasks / documents / decisions) | **PLANNED** — Phase C | No backend. |
| Partnerships / Employees / Roles & Permissions | **PLANNED** — Phase D | |
| Finance / Payments / Invoices / Revenue / Expenses / Payroll | **PLANNED** — Phase E | |
| Marketing-CMS / Communication / Notifications / Reports / System-Audit | **PLANNED** — Phase F | |

The rest of this section is the **full planned scope** — planned architecture,
delivered incrementally on `nadoumi-admin/`.

New top-level menu group **"Nadoumi"** (`sys_menu` `M`) with `C`/`F` children per
domain. Each module maps to a bounded context (§7) and a permission prefix:

| Admin module | Bounded context | Permission prefix | Backs onto (planned tables) |
| --- | --- | --- | --- |
| Dashboard | Reporting | `nad:dashboard:view` (+ per-widget scopes, §6) | reporting read models only |
| Applicants | Applicant | `nad:applicant:*` | `nad_applicant`, `nad_user_applicant_access`, education / test-score |
| Applications (workbench) | Application | `nad:application:*` | `nad_application` + stage history / events / tasks / notes / decisions |
| Workflow admin | Workflow | `nad:workflow:*` | `nad_wf_definition/stage/transition/*` |
| Documents | Document | `nad:document:*` | `nad_document`, `nad_document_version` |
| Universities | University | `nad:university:*` | `nad_university` |
| Programs | Program | `nad:program:*` | `nad_program`, `nad_program_intake` |
| Scholarships | Scholarship | `nad:scholarship:*`, `nad:scholarship:internal:view` | `nad_scholarship` + `nad_scholarship_internal` |
| Partnerships | Partnership | `nad:partnership:*`, `nad:partnership:terms:view` | `nad_partnership` (confidential) |
| Marketing / CMS | Marketing / CMS | `nad:cms:*` | `nad_cms_page`, `nad_cms_article`, `nad_cms_faq`, `nad_cms_media`, `nad_cms_campaign` |
| Communication | Communication | `nad:conversation:*` | `nad_conversation`, `nad_message` |
| Notifications | Notification | `nad:notification:*` | templates, delivery log, preferences |
| Employees | Employee | `nad:employee:*`, `nad:employee:sensitive:view` | `nad_employee`, `nad_employee_assignment`, `nad_employee_document` (links to `sys_user`) |
| Roles & Permissions | Identity & Access | reuse `system:role:*` / `system:menu:*` | `sys_role`, `sys_menu`, `sys_role_menu` |
| Finance | Finance | `nad:finance:*` | `nad_fin_account`, `nad_fin_ledger_entry`, `nad_expense`, `nad_revenue`, `nad_commission_ledger` |
| Payments | Payments | `nad:payment:*` | `nad_payment`, `nad_payment_txn`, `nad_refund` |
| Invoices | Finance | `nad:invoice:*` | `nad_invoice`, `nad_invoice_line` |
| Expenses | Finance | `nad:expense:*` | `nad_expense`, `nad_expense_category` |
| Revenue / Earnings | Finance | `nad:revenue:*` | `nad_revenue`, `nad_commission_ledger` (read models) |
| Payroll | Payroll | `nad:payroll:*` (highly restricted) | `nad_payroll_run`, `nad_payslip`, `nad_salary` |
| Reports & Analytics | Reporting | `nad:report:*` | reporting read models / aggregates |
| System Settings | Identity & Access / platform | `system:config:*`, `nad:settings:*` | `sys_config`, `sys_dict_*`, Nadoumi settings tables |
| Staff assignment (cross-cutting) | Application | `nad:assignment:*` | `nad_application.assignee_user_id` |

### 2.1 Module → bounded-context rule

`Employee`, `Finance`, `Payroll`, `Payments`, `Marketing / CMS`, and `Reporting` are
**separate bounded contexts** (§7). In particular **Finance, Payroll, and Payments
stay separate domains** — Payments is transaction capture at the application boundary,
Finance is the ledger / invoicing / expense / revenue view, Payroll is employee
compensation. They integrate through explicit read models and domain events, not by
sharing tables.

### 2.2 Sidebar navigation (EXISTING)

The sidebar is a **static manifest in the frontend**, `nadoumi-admin/src/config/nav.ts`
— the single source of truth for both the sidebar and the router (no
`/getRouters`, no dynamic menu, no `placeholder.vue`). `/getInfo` is still called,
only for the user's roles + permission tokens.

Each manifest item declares `{ key, path, icon, perm?, status }`. The manifest
carries the **full module map as the roadmap**, but the sidebar and router render
**only `status: 'implemented'` items** the user is permitted to reach. Planned
modules are **excluded from active navigation entirely** — no route, no page, no
"planned" row. Building a module is one `status` flip plus the screen + route +
tests in the same change.

Approved group order (`nav.groups`):

| Group | Items (→ status) |
| --- | --- |
| _(top)_ | Dashboard → **implemented** |
| Operations | Applicants → **implemented** · Applications · Documents |
| Education | Universities · Programs · Scholarships |
| _(top)_ | Partnerships |
| People | Employees · Roles & Permissions |
| Finance | Payments · Invoices · Revenue · Expenses · Payroll |
| Growth | Marketing / CMS |
| Communication | Conversations · Notifications |
| _(top)_ | Reports & Analytics |
| System | Configuration · Audit Logs |

Everything not marked **implemented** is **PLANNED** and absent from the running
sidebar. A status report lists a Nadoumi screen as delivered only when it has a
real view, a route, a `nad:*` permission, server-side authorization, and a test.

`sys_menu` seeding (`V2`) stays only for the RuoYi console screens `ruoyi-ui`
still serves; the Nadoumi admin does not read it.

### 2.3 Shared component library (EXISTING — `nadoumi-admin/src/components/ui/`)

Every module composes from these; no module hand-rolls its own table, filter,
empty/error/loading surface or confirm dialog.

| Component | Responsibility |
| --- | --- |
| `PageHeader` (`components/`) | Title + subtitle + `#actions` slot. Every screen. |
| `ui/DataTable` | `el-table` + pagination + built-in error / skeleton / empty states; `columns` prop, per-column `#cell-<prop>` slots, `row-click`. |
| `ui/FilterBar` | Filter row layout + "Clear filters" affordance (`dirty` prop). |
| `ui/SearchInput` | Debounced search field (`v-model` + `@search`). |
| `ui/StatusBadge` | One status → tone vocabulary for the whole admin (`DEFAULT_MAP`). |
| `ui/Avatar` | Initials / image circle. |
| `ui/EmptyState` / `ui/ErrorState` / `ui/LoadingState` | The three non-content states, used directly and inside `DataTable`. |
| `ui/AppTabs` | Underline tabs for detail pages (`{ key, label, count? }`). |
| `ui/DescriptionList` | Key/value grid for detail overviews. |
| `ui/StatePanel` | loading / error / empty gate wrapping any content slot (used by every detail tab). |
| `ui/Drawer` | Side sheet with a consistent header + sticky cancel/save footer (`saving` disables both). Add/edit forms live here. |
| `ui/FormSection` | Titled group inside a form. |
| `composables/useConfirm` | Typed wrapper over `ElMessageBox` — the single confirm surface. |
| `composables/useResourceList<T>` | fetch + loading/error/loaded state for a list resource; `load()` once, `reload()` forces. |

**Form fields:** inside a validated form, use Element Plus `<el-form>` +
`<el-form-item>` directly — that *is* the shared field component (a dependency,
not duplication); no bespoke `FormField`. `FormSection` handles grouping.

Shared prop types live in `ui/types.ts`. Dashboard widgets (`StatCard`,
`DonutStat`, `ComingSoonCard`, `DashboardGroup`, `RecentApplicants`) build on the
same primitives.

### Roles (BASELINE — `sys_role` + `sys_role_menu`; data scope via `sys_role.data_scope`)

Ten roles, defined with their exact token sets and data scope in
**`docs/PERMISSION_CATALOGUE.md` §3**:
`nadoumi_super_admin`, `ops_manager`, `workflow_admin`, `case_officer`,
`document_reviewer`, `partnerships_manager`, `content_editor`, `finance`,
`support_agent`, `read_only_analyst`.

Confidentiality among staff (least privilege):
- `nad:scholarship:internal:view` (operational linkage: which university/partnership a
  scholarship maps to) — `ops_manager`, `case_officer`, `partnerships_manager`,
  `read_only_analyst`.
- `nad:partnership:view` — `ops_manager`, `partnerships_manager`,
  `read_only_analyst` (no terms).
- `nad:partnership:terms:view` (commission / confidential terms) —
  `partnerships_manager`, `finance` only.
- `nad:applicant:pii:view` — `ops_manager`, `case_officer`, `document_reviewer`.
UI hiding is cosmetic; the backend enforces every token regardless (`SECURITY.md` §2,
`PERMISSION_CATALOGUE.md` §5). **No staff role ever leaks a confidential field onto a
`/api/public|student` response** — that boundary is structural.

## 3. Build strategy for admin screens (BASELINE)

1. **Phase 3.5:** stand up `nadoumi-admin` (RuoYi-Vue3), re-verify the RuoYi API
   contract, re-point `ruoyi-generator` templates to Vue 3.
2. Scaffold straightforward catalog CRUD (Universities, Programmes, Content,
   Scholarships) from `nad_*` tables with the generator → **review & refine** the
   generated Java + Vue; never ship generated code unreviewed.
3. Hand-build the non-CRUD surfaces: the **Application workbench** (one view: timeline
   merging `nad_message` + `nad_application_event`, stage transitions, task checklist,
   document checklist, notes, decisions), workflow definition editor, document review
   queue, reporting dashboards.
4. Seed menus/permissions (`sys_menu`, `sys_role`, `sys_role_menu`) via **Flyway
   migrations** (`V2` + a role-seed migration), never console edits.
5. Admin user-management screens filter `sys_user.user_type='00'` by default; a
   separate "External users" view (`/api/staff/external-users/**`) lists
   students/agents/guardians.

## 4. Admin ↔ backend contract (BASELINE)

- **Auth/session:** `POST /login` → JWT (cookie `nadoumi-admin-token`);
  `GET /getInfo` → user + roles + permission tokens. `/getRouters` is **not used**
  — navigation is the static `src/config/nav.ts` manifest (§2.2).
- Existing RuoYi endpoints (`/system/**`, `/monitor/**`, `/tool/**`) stay unchanged;
  `ruoyi-ui` depends on them until those screens are ported.
- New Nadoumi admin calls go to **`/api/staff/**`** (Phase 1: `/api/staff/applicants`)
  — typed `record` DTOs, real HTTP status codes, `problem+json` errors
  (`docs/API_DESIGN.md` §5). `src/utils/request.ts` handles both shapes: `code`-in-body
  for `/login`·`/getInfo`, real 4xx/5xx for `/api/**`.
- **Dashboard data (Phase 1):** the Applicants widgets read `/api/staff/applicants`
  directly (interim source, §6.1). No `/api/staff/dashboard` endpoint yet; that
  arrives with the Reporting read models.

## 5. Decision status

| Item | Status |
| --- | --- |
| **D1** admin UI baseline | **APPROVED** — `RuoYi-Vue3` as `nadoumi-admin/`, integrate Phase 3.5. |
| Nadoumi admin API conventions | **APPROVED** — `/api/v1/staff/**` (new conventions), not `AjaxResult`; adapt the interceptor. |
| Menu/permission seeding | **APPROVED** — Flyway migrations. |
| Delivery | **APPROVED** — served by the reverse proxy; not bundled into the jar (`DEPLOYMENT.md` §3). |
| Full business-platform module scope (§2, §6, §7) | **APPROVED as planned architecture** — sequencing per phase; not implemented in the current `nadoumi-web` build. |

---

## 6. Admin Dashboard (PLANNED)

Route `/dashboard`, permission `nad:dashboard:view`. Built **only** from Reporting
read models (§7), never by querying operational tables directly — so a dashboard
widget can never be the path that leaks a confidential field. Each widget is
independently permission-gated; a role sees the union of the widgets its tokens
allow.

| Widget | Source read model | Extra gate |
| --- | --- | --- |
| Total / new / pending applications | `rm_application_counts` | `nad:report:applications:view` |
| Recent applications | `rm_application_recent` | `nad:application:list` (+ data scope / assignment) |
| Accepted / rejected applications | `rm_application_outcomes` | `nad:report:applications:view` |
| New applicants | `rm_applicant_counts` | `nad:report:applicants:view` |
| Application status distribution | `rm_application_status_dist` | `nad:report:applications:view` |
| Revenue | `rm_finance_revenue` | `nad:report:finance:view` |
| Expenses | `rm_finance_expenses` | `nad:report:finance:view` |
| Net earnings | `rm_finance_net` | `nad:report:finance:view` |
| Outstanding payments | `rm_payments_outstanding` | `nad:report:finance:view` |
| Employee count | `rm_employee_headcount` | `nad:report:hr:view` |
| Recent activity | `rm_activity_feed` (from `nad_application_event` + audit) | per-row visibility filter |
| Operational alerts / tasks | `rm_ops_alerts` (overdue tasks, failed deliveries, stuck stages, invite SLA breaches) | `nad:application:list` / `nad:notification:view` |

Financial and HR widgets (`revenue`, `expenses`, `net earnings`, `outstanding
payments`, `employee count`) are hidden entirely — not zeroed — for roles without
the finance / HR report tokens.

### 6.1 Current implementation status (EXISTING — Admin Phase 1)

Route `/dashboard`, visible to any signed-in staff user (no `nad:dashboard:view`
token is seeded yet); each widget gates on its own data permission. Composed from
reusable primitives — `PageHeader`, `DashboardGroup`, `StatCard`, `DonutStat`
(hand-rolled SVG, **no chart library**), `ComingSoonCard`, `RecentApplicants` —
with real loading (skeleton), error (retry banner) and empty states.

**Real (from `GET /api/staff/applicants`, interim source until the Reporting
slice — the widget contract + gates in the §6 table stay the target):**

- Stat cards: Total applicants · New (last 30 d, via the `createdAfter` filter) ·
  Incomplete profiles · Active applicants.
- `DonutStat`: profile completion — complete vs incomplete over a bounded
  200-row sample, labelled "of the latest N"; not a precise aggregate.
- `RecentApplicants`: the eight most recent, with `createdAt` (added to
  `ApplicantResponse` for this).

**Not yet available (honest `ComingSoonCard`, em-dash value, never a number):**
Applications · Revenue · Expenses · Net earnings · Payments · Employees ·
Payroll · Notifications — each tagged with the bounded context (§7) that will
supply it. Recent activity and Pending tasks are empty-state panels pending the
Reporting event store / Workflow module.

When the Reporting read models land, the real widgets switch to `rm_*` sources
and each coming-soon card is replaced context-by-context; the layout does not
change.

---

## 7. Business-platform bounded contexts & sensitive-data boundaries (PLANNED)

Planned architecture. These are logical modules in the modular monolith
(`nadoumi-modules/*`), **not** microservices, and **not** built in the current
`nadoumi-web` build. High-level entities are indicative; realized DDL comes with each
context's own phase.

### 7.1 Employee (context: `nadoumi-modules/employee`)

Distinct from `sys_user` (a login identity) and from RuoYi `Roles & Permissions`.

- **Entities:** `nad_employee` (1:0..1 `sys_user`; job title, department `sys_dept`,
  employment type, start/end date, manager), `nad_employee_assignment` (role on the
  application workbench, capacity), `nad_employee_document` (contract, ID — governed
  like applicant documents), `nad_employee_emergency_contact`.
- **Sensitive:** national ID / tax ID, bank details, home address, emergency
  contacts, salary band, performance notes, termination reason.
- **Authorization boundary:** `nad:employee:view` sees roster + role + department
  only. `nad:employee:sensitive:view` (HR + the employee themselves) is required for
  ID, bank, address, emergency contact. Salary lives in **Payroll**, not here.
  Managers see their **direct reports'** non-sensitive record via data scope; no
  cross-department browsing without `nad:employee:view:all`.

### 7.2 Finance (context: `nadoumi-modules/finance`)

The ledger / invoicing / expense / revenue view. Separate from Payments and Payroll.

- **Entities:** `nad_fin_account` (chart of accounts), `nad_fin_ledger_entry`
  (double-entry, append-only), `nad_invoice` + `nad_invoice_line`, `nad_expense` +
  `nad_expense_category`, `nad_revenue`, `nad_commission_ledger` (commission earned
  per enrolment — forward-referenced from `DOMAIN_MODEL.md` G-P3).
- **Sensitive:** all of it. Partnership commission rates and per-partner revenue tie
  into Partnership confidential terms (`nad:partnership:terms:view`).
- **Authorization boundary:** `nad:finance:view` for read, `nad:finance:manage` for
  posting entries / issuing invoices / approving expenses. Commission and per-partner
  revenue rows additionally require `nad:partnership:terms:view`. **No finance field
  is ever exposed on `/api/public/**` or `/api/student/**`** (structural, like
  scholarship confidentiality). Ledger entries are append-only; corrections are
  reversing entries.

### 7.3 Payroll (context: `nadoumi-modules/payroll`)

Employee compensation. The most restricted context.

- **Entities:** `nad_salary` (current + history, effective-dated), `nad_payroll_run`
  (period, status), `nad_payslip` (per employee per run; gross, deductions, net,
  tax), `nad_payroll_adjustment`.
- **Sensitive:** every field. An individual may see **only their own** payslips.
- **Authorization boundary:** `nad:payroll:view` and `nad:payroll:run` are held by a
  **very small** role set (`finance` + a dedicated `payroll_admin`), never by
  `ops_manager` / `case_officer` / analysts. Self-service payslip view is a separate
  `nad:payroll:self:view` scoped to `employee_id = current`. `nadoumi_super_admin`
  does **not** implicitly get payroll data — it is an explicit grant, audited. Every
  read of another person's payslip writes an audit row.

### 7.4 Payments (context: `nadoumi-modules/payment`)

Transaction capture at the application boundary (application fees, deposits).
Separate from Finance and Payroll.

- **Entities:** `nad_payment` (application_id, kind, amount, currency, status),
  `nad_payment_txn` (provider, provider_ref, raw callback — PCI-sensitive fields
  tokenized / never stored raw), `nad_refund`.
- **Sensitive:** provider references, payer contact, card/last-4 (tokenized).
  Students see **their own** payment status + receipts via `/api/student/**` (amount,
  status, date, receipt) — never provider internals or reconciliation data.
- **Authorization boundary:** `nad:payment:view` / `nad:payment:manage` for staff;
  reconciliation and provider-raw data require `nad:payment:reconcile`. Payments
  **emits domain events** (`PaymentSettled`, `RefundIssued`) that Finance consumes to
  post ledger entries — it does not write to `nad_fin_*` itself.

### 7.5 Marketing / CMS (context: `nadoumi-modules/cms`)

Public content behind `nadoumi-web` marketing pages, plus outbound campaigns.

- **Entities:** `nad_cms_page`, `nad_cms_article`, `nad_cms_faq`,
  `nad_cms_destination_guide`, `nad_cms_media`, `nad_cms_campaign`,
  `nad_cms_lead` (contact-form / newsletter capture).
- **Sensitive:** `nad_cms_lead` holds personal contact data (PII handling per
  `SECURITY.md` §6); campaign recipient lists.
- **Authorization boundary:** `nad:cms:view` / `nad:cms:edit` / `nad:cms:publish`
  (draft vs published separated; publish is a distinct token). Leads are visible to
  `nad:cms:leads:view` + `support_agent`. Published CMS content is the **only** thing
  in this context served anonymously (through `/api/public/**`); drafts, leads and
  campaigns never are.

### 7.6 Reporting (context: `nadoumi-modules/reporting`)

Read models / aggregates for §6 and exports. **Read-only, derived.**

- **Entities:** `rm_*` projections (materialized or query views) rebuilt from domain
  events / operational tables by a scheduled job; `nad_report_definition`,
  `nad_report_export` (audit of who exported what).
- **Authorization boundary:** each `rm_*` model carries the permission of its source
  domain (finance read models → `nad:report:finance:view`, HR → `nad:report:hr:view`,
  etc.). A reporting query **cannot** widen access — if the caller can't see the
  underlying domain, the read model returns nothing. Exports are audited and
  rate-limited. Confidential scholarship→university / partnership linkage is **not**
  present in any student-reachable read model.

### 7.7 Sensitive-data summary

| Data class | Context | Gating token(s) | Never on `/api/public|student` |
| --- | --- | --- | --- |
| Employee ID / bank / address / emergency contact | Employee | `nad:employee:sensitive:view` | ✅ never |
| Salary, payslips, payroll runs | Payroll | `nad:payroll:view` / `nad:payroll:self:view` | ✅ never |
| Ledger, invoices, expenses, revenue, commission | Finance | `nad:finance:view` (+ `nad:partnership:terms:view` for commission) | ✅ never |
| Payment provider internals / reconciliation | Payments | `nad:payment:reconcile` | ✅ never (student sees own receipt only) |
| Partnership existence / terms / contacts | Partnership | `nad:partnership:view` / `:terms:view` | ✅ never (`DOMAIN_MODEL.md` §6) |
| Scholarship → university / partnership linkage | Scholarship (internal) | `nad:scholarship:internal:view` | ✅ never (`DOMAIN_MODEL.md` §6) |
| CMS leads / campaign recipients | Marketing / CMS | `nad:cms:leads:view` | ✅ never (published content only) |

Enforcement is the same defense-in-depth as `SECURITY.md` §2 / `DOMAIN_MODEL.md` §6:
`@PreAuthorize` on the controller, an explicit service re-check, a scoped mapper
parameter, and — for anything with a public/student surface — a response-body
denylist net. No role combination, including `nadoumi_super_admin`, causes a
confidential financial / payroll / employee / partnership field to appear on an
anonymous or student response.
