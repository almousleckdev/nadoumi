# P4 — Admin Application Workbench — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: `superpowers:subagent-driven-development` or `superpowers:executing-plans`. Steps `- [ ]`.
> **Depends on P2** (staff application API merged). Independent of P3. **No migrations.**
> Task granularity = task + interfaces + test list; executor writes the RED/GREEN/commit micro-steps following P1's worked pattern and the existing `nadoumi-admin` conventions.

**Goal:** The `nadoumi-admin` `/applications` workbench — filterable list, detail with stage timeline, tasks panel, decisions panel, guarded transition actions (with blocked-reason feedback), assign/claim, and a read-only workflow-definition viewer.

**Architecture:** Pure `nadoumi-admin` (Vue 3 + Element Plus) against the P2 staff API (`/api/staff/applications`, `/api/staff/workflow-definitions`). Built from the shared `ui/` primitives (`DataTable` with `storage-key`, `Drawer`, `FormSection`, `DescriptionList`, `StatusBadge`, `FilterBar`, `SearchInput`, `useConfirm`). No new backend.

**Tech Stack:** Vue 3 `<script setup lang="ts">`, Element Plus, Pinia, `vue-i18n` (`src/lang/en.ts` default + derived `zh.ts`), Vitest + `@vue/test-utils`.

**Spec:** `docs/superpowers/specs/2026-09-03-media-storage-and-application-engine-design.md` §II.9.

## Global Constraints

- Follow the conventions already in `nadoumi-admin/src/` — `src/api/*.ts` typed clients over the `request` axios instance; `src/views/<area>/{index,detail,*Drawer}.vue`; static `src/config/nav.ts` with `status: 'implemented' | 'planned'`; `src/lang/en.ts` is the source of truth, `zh.ts` is `JSON.parse(JSON.stringify(en))` + overrides (so adding to `en.ts` auto-covers `zh`).
- `pnpm exec eslint --fix` before every commit (`vue/max-attributes-per-line` is enforced). `pnpm run lint && pnpm run build && pnpm run test` is the gate.
- Perms drive visibility: buttons/tabs gated on `userStore.hasPerm('nad:application:*')` — but this is **display only**; the server is authoritative.
- Transition buttons must show **why** a transition is disabled — the P2 `GET /{id}` response carries `transitionOptions[]` (`{ code, enabled, blockedReason }`); render `blockedReason` in a tooltip.
- Standard commit trailers; `git reset -q .claude .gitignore ry.sh` before committing.

## File Structure

**New — `nadoumi-admin/src`**
- `api/application.ts` — typed client + types.
- `views/applications/index.vue` — list.
- `views/applications/detail.vue` — thin orchestrator with tabs.
- `views/applications/components/StageTimeline.vue`, `TasksPanel.vue`, `DecisionsPanel.vue`, `TransitionBar.vue`, `AssignControl.vue`.
- `views/applications/WorkflowDefinitionDialog.vue` — read-only definition viewer.
- `views/applications/RecordDecisionDrawer.vue`, `SkipTaskDrawer.vue`.
- `tests/unit/pages/Applications.test.ts`, `tests/unit/pages/ApplicationDetail.test.ts`, `tests/unit/components/TransitionBar.test.ts`.

**Modified — `nadoumi-admin/src`**
- `config/nav.ts` — add `{ path: '/applications', label: 'nav.applications', status: 'implemented', perm: 'nad:application:list' }`.
- `router` (wherever routes are registered) — `/applications` + `/applications/:id`.
- `lang/en.ts` — `nav.applications`, `application.*` namespace.
- `tests/unit/nav.test.ts` + `tests/unit/Sidebar.test.ts` — add `/applications` to the implemented-paths list.

**Modified — docs**
- `docs/ADMIN_ARCHITECTURE.md` (§2.0 module status + §2.3 if new primitives), `docs/FRONTEND_ARCHITECTURE.md` (n/a — admin only, but note in ADMIN), `docs/PLATFORM_ARCHITECTURE.md §8` (Step 6 → workbench ✅).

---

## Task 1: `api/application.ts` — typed client

**Files:** `nadoumi-admin/src/api/application.ts`.
**Interfaces:**
```ts
export type ApplicationType = 'PROGRAM_WITH_SCHOLARSHIP' | 'PROGRAM_ONLY'
export interface ApplicationListItem {
  id: number; applicationType: ApplicationType; applicantName: string
  programName: string; universityName: string; scholarshipTitle: string | null
  currentStatus: string; stageCode: string; assigneeName: string | null
  submittedAt: string | null; updatedAt: string
}
export interface TransitionOption { code: string; enabled: boolean; blockedReason: string | null }
export interface ApplicationTask {
  id: number; title: string; roleRequired: string | null; mandatory: boolean
  blocksExit: boolean; status: 'OPEN' | 'DONE' | 'SKIPPED' | 'CANCELLED'
  assigneeName: string | null; dueAt: string | null; skipReason: string | null
}
export interface ApplicationDecision {
  id: number; decisionType: string; outcome: string; rationale: string
  decidedByName: string; decidedAt: string
}
export interface TimelineEntry {
  kind: 'STAGE' | 'EVENT'; label: string; detail: string | null; at: string; actorName: string | null
}
export interface ApplicationDetail extends ApplicationListItem {
  version: number; referenceCode: string | null
  tasks: ApplicationTask[]; decisions: ApplicationDecision[]; timeline: TimelineEntry[]
  transitionOptions: TransitionOption[]
  snapshots: { profile: boolean; requirements: boolean }
}
export interface WorkflowDefinitionView {
  code: string; name: string; version: number
  stages: { code: string; name: string; stageType: string; statusLabel: string; slaHours: number | null }[]
  transitions: { code: string; fromStageCode: string | null; toStageCode: string; guard: string | null; auto: boolean }[]
  taskTemplates: { stageCode: string; title: string; roleRequired: string | null; mandatory: boolean; blocksExit: boolean }[]
}
```
Functions: `listApplications(params)`, `getApplication(id)`, `transitionApplication(id, code, { reason?, version })`, `recordDecision(id, { decisionType, outcome, rationale })`, `completeTask(id, taskId)`, `skipTask(id, taskId, reason)`, `assignApplication(id, assigneeUserId)`, `claimApplication(id)`, `listWorkflowDefinitions()`, `getWorkflowDefinition(code)`.
**Test:** covered via the page tests (mock `request`).
**Commit:** `feat(admin): application API client + types`.

---

## Task 2: `/applications` list

**Files:** `views/applications/index.vue`, `config/nav.ts`, router, `lang/en.ts`, `tests/unit/pages/Applications.test.ts`, `tests/unit/{nav,Sidebar}.test.ts`.
**Interfaces / behaviour:**
- `DataTable` `storage-key="applications"`; columns: Applicant · Opportunity (`programName` + `universityName`, `scholarshipTitle` as a tag) · Type · Status (`StatusBadge`) · Stage · Assignee · Submitted · Actions (Open).
- `FilterBar` + `SearchInput`: `q`, `type` select, `status` select (distinct `currentStatus` values), `stageCode` select, `assignee` select (staff users — reuse an existing user-lookup if present, else free text), unassigned toggle.
- Row click / "Open" → `/applications/:id`.
- Gated on `nad:application:list`.
**Tests:** renders rows from a mocked `listApplications`; filter change refetches; `/applications` present in `nav.test.ts` implemented paths.
**Commit:** `feat(admin): applications workbench list`.

---

## Task 3: `detail.vue` orchestrator + `StageTimeline`

**Files:** `views/applications/detail.vue`, `views/applications/components/StageTimeline.vue`, `lang/en.ts`, `tests/unit/pages/ApplicationDetail.test.ts`.
**Interfaces / behaviour:**
- `detail.vue` — loads `getApplication(id)`; header = `DescriptionList` (applicant, opportunity, type, reference, assignee, status badge, submitted, snapshot presence). `AppTabs`: **Overview** (timeline), **Tasks**, **Decisions**, **Definition**. Holds `detail` + a `reload()` passed to child panels; children emit `changed` → `reload()`.
- `StageTimeline` — vertical timeline from `detail.timeline` (`kind` STAGE vs EVENT styling); `actorName` + `at` per entry; `detail` shown when present.
**Tests:** tab switching; timeline renders entries in order; header shows reference + snapshot flags.
**Commit:** `feat(admin): application detail shell + stage timeline`.

---

## Task 4: `TransitionBar` (guarded actions)

**Files:** `views/applications/components/TransitionBar.vue`, `tests/unit/components/TransitionBar.test.ts`.
**Interfaces / behaviour:**
- Props: `options: TransitionOption[]`, `version: number`, `applicationId: number`.
- Renders one button per option; `:disabled="!o.enabled"`; disabled buttons wrapped in an `el-tooltip` showing `o.blockedReason`.
- Click → for `withdraw` (and any transition whose code implies a reason) open a small `el-dialog` with a required reason textarea; otherwise `useConfirm`. Then `transitionApplication(id, code, { reason, version })`; on `409` show "changed elsewhere, reloading" and emit `changed`; on success emit `changed`.
- Gated on `nad:application:transition`.
**Tests:** disabled button shows `blockedReason` tooltip; `withdraw` requires a reason before the call; a `409` response surfaces the reload message.
**Commit:** `feat(admin): guarded transition bar with blocked-reason feedback`.

---

## Task 5: `TasksPanel` + `SkipTaskDrawer`

**Files:** `views/applications/components/TasksPanel.vue`, `views/applications/SkipTaskDrawer.vue`, `lang/en.ts`.
**Interfaces / behaviour:**
- Table of `detail.tasks`: title, mandatory/blocks-exit tags, status badge, assignee, due date. Row actions: **Complete** (`useConfirm` → `completeTask`), **Skip** (opens `SkipTaskDrawer` with a required reason → `skipTask`). Actions hidden for non-`OPEN` tasks and without `nad:application:transition`.
- Emits `changed` after either action.
**Tests:** complete calls `completeTask` + emits `changed`; skip requires a reason.
**Commit:** `feat(admin): application tasks panel (complete / skip)`.

---

## Task 6: `DecisionsPanel` + `RecordDecisionDrawer`

**Files:** `views/applications/components/DecisionsPanel.vue`, `views/applications/RecordDecisionDrawer.vue`, `lang/en.ts`.
**Interfaces / behaviour:**
- List of `detail.decisions` (type, outcome, rationale, who/when).
- "Record decision" (gated `nad:application:decide`) → `RecordDecisionDrawer`: `decisionType` select (`NADOUMI_INTERNAL`, `UNIVERSITY_OFFER`, `SCHOLARSHIP_AWARD`, `APPLICANT_RESPONSE`, `DOCUMENTS_COMPLETE`, `FEE_SETTLED`), `outcome` select whose options depend on the chosen type (e.g. `UNIVERSITY_OFFER` → `OFFER`/`CONDITIONAL_OFFER`/`WAITLIST`/`REJECT`; `DOCUMENTS_COMPLETE`/`FEE_SETTLED` → `CONFIRMED`), required `rationale`. → `recordDecision(...)` → emit `changed` (a recorded decision often unblocks a transition — the parent `reload()` refreshes `transitionOptions`).
**Tests:** outcome options track the selected type; rationale required; submit calls `recordDecision`.
**Commit:** `feat(admin): application decisions panel + record-decision drawer`.

---

## Task 7: `AssignControl` + `WorkflowDefinitionDialog`

**Files:** `views/applications/components/AssignControl.vue`, `views/applications/WorkflowDefinitionDialog.vue`, `lang/en.ts`.
**Interfaces / behaviour:**
- `AssignControl` — current assignee + "Assign" (staff-user select → `assignApplication`, gated `nad:application:assign`) + "Claim" (→ `claimApplication`, gated `nad:application:claim`, shown only when unassigned). Emits `changed`.
- `WorkflowDefinitionDialog` — opened from the Definition tab; `getWorkflowDefinition(detail's definition code)`; renders the stage list (order, type, status label, SLA), the transition list (from→to, guard string, auto), and task templates grouped by stage. Read-only.
**Tests:** assign calls the API + emits `changed`; claim hidden when already assigned; dialog renders stages + transitions from a mocked definition.
**Commit:** `feat(admin): assign/claim control + read-only workflow definition viewer`.

---

## Task 8: Docs + gate

**Files:** `docs/ADMIN_ARCHITECTURE.md`, `docs/PLATFORM_ARCHITECTURE.md §8`, `phase-status.md` memory.
**Steps:**
- [ ] `cd nadoumi-admin && pnpm exec eslint --fix "src/**/*.{vue,ts}" >/dev/null 2>&1; pnpm run lint && pnpm run build && pnpm run test` — all green, new page/component tests + `nav`/`Sidebar` updated.
- [ ] `ADMIN_ARCHITECTURE.md` §2.0 — Applications workbench = IMPLEMENTED; §2.3 — any new shared primitive (none expected; all reuse).
- [ ] `PLATFORM_ARCHITECTURE.md §8` — Step 6 fully ✅ (engine P2 + public/portal P3 + workbench P4).
- [ ] Update `phase-status.md` memory — Step 6 complete.
- [ ] Commit `docs: admin application workbench` and `chore(admin): P4 gate green`.

---

## Self-review (authoring time)

- **Spec coverage (§II.9):** list w/ filters (T2); detail + stage timeline (T3); tasks panel complete/skip (T5); decisions panel + record (T6); guarded transition buttons with blocked-reason (T4); assign/claim (T7); read-only definition viewer (T7); built from shared primitives (all tasks). Complete.
- **Placeholders:** none — `api/application.ts` gives the exact type surface every component consumes; each component task names its props, actions, gates, and test cases.
- **Type consistency:** `ApplicationDetail` (T1) is the single shape `detail.vue` (T3) passes to `TransitionBar` (`transitionOptions`, `version`), `TasksPanel` (`tasks`), `DecisionsPanel` (`decisions`), `AssignControl`. `changed` event convention uniform across T4–T7 → `detail.vue.reload()`. `TransitionOption.blockedReason` (T1) rendered in T4. Decision type/outcome vocabulary in T6 matches the P2 seed guards (`DOCUMENTS_COMPLETE:CONFIRMED`, `FEE_SETTLED:CONFIRMED`, `UNIVERSITY_OFFER:OFFER|...`).
- **Cross-plan note:** depends only on P2's `GET /api/staff/applications/{id}` returning `transitionOptions[]` and `version` — confirm P2 Task 15 delivers exactly that field set before starting P4.
