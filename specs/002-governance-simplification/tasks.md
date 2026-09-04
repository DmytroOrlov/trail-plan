---
description: Task list for "Governance Simplification — Back to Stock Spec Kit"
---

# Tasks: Governance Simplification — Back to Stock Spec Kit

**Input**: Design documents from `specs/002-governance-simplification/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: This is a governance/process-only change. FR-026 forbids a new test
harness, so there are NO unit/contract/integration test tasks. Verification is
mechanical (term search, `specify workflow` state, path-scoped `git diff`) and is
expressed as verification tasks drawn from quickstart.md V-1…V-8 (the V-9
`/speckit.converge` check is contributor-run post-implementation acceptance, not a
task executed by `/speckit.implement`).

**Organization**: Tasks are grouped by user story to enable independent
implementation and testing. The constitution rewrite (the shared governance
owner read by both US1 and US2, single file → one atomic MAJOR amendment per
research.md D2) lives in the Foundational phase; each story phase then delivers
and independently verifies its own increment.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files/actions, no incomplete dependency)
- **[Story]**: US1 (stock workflow), US2 (update-rule findability), US3 (history preservation)
- Exact file paths included in each task

## Path Conventions

Repository-root governance paths (`/Users/do/git/trail-plan`): `.specify/memory/`,
`.specify/workflows/`, `.agents/skills/`, `AGENTS.md`, `docs/`,
`specs/002-governance-simplification/`. No `src/`/`tests/` — no application code
changes.

<!--
  NOTE: US1 (machinery removal + router + pointer) is the MVP. US2 and US3 are
  verification-only because the durable governance content they depend on is
  authored once, atomically, in the Foundational constitution rewrite.
-->

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Record the non-change baseline and confirm stock tooling is present
before any governance file is touched (quickstart Prerequisites).

- [X] T001 Record the pre-change baseline in the repo shell: capture `BASE=$(git rev-parse HEAD)` and confirm `git status --porcelain` shows only `specs/002-governance-simplification/` plan-phase artifacts (quickstart Prerequisites)
- [X] T002 Record the default regression-suite SELF-TESTS baseline: run `./trail-plan.scala` per `docs/DEVELOPMENT.md` full-log discipline against Valhalla (`http://localhost:8002`) and save the `SELF-TESTS: <n> passed, <m> failed` line for the V-6 comparison in T025 (if Valhalla is unavailable, record the documented pre-change run output per quickstart V-6 fallback)
- [X] T003 [P] Verify stock prerequisites: `specify --version` reports `specify 1.0.3`; `rg` and `git` on `PATH`; the eight stock commands named by the completion sequence (`speckit.specify`, `speckit.clarify`, `speckit.plan`, `speckit.checklist`, `speckit.tasks`, `speckit.analyze`, `speckit.implement`, `speckit.converge`) are present in `.opencode/commands/` (GOV-X-21)

**Checkpoint**: Baseline (BASE commit + SELF-TESTS result) recorded; stock command set confirmed.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: The single atomic MAJOR rewrite of `.specify/memory/constitution.md`
from 1.1.0 → 2.0.0. This file is the shared governance owner that BOTH US1 (stock
completion path) and US2 (same-change update + honesty rules) read, so it is
authored here before story phases. All tasks touch the SAME file → strictly
sequential, no [P].

**⚠️ CRITICAL**: No user story can be independently tested until the 2.0.0 text is
coherent and names no deleted machinery.

- [X] T004 Replace the leading `Sync Impact Report` HTML comment in `.specify/memory/constitution.md` with the MAJOR amendment record: version change `1.1.0 -> 2.0.0`, amendment rationale (explicit user-requested withdrawal of first-change process machinery), changed principles, and follow-up status (research.md D2, GOV-V-01)
- [X] T005 In `.specify/memory/constitution.md`, author **Core durable rules**: canonical ownership and source precedence incl. "one normative fact, one textual owner" and the non-authority note that historical Scala snapshots / archived change artifacts / prior chat context are not current truth (GOV-R-01); the no-silent-weakening rule covering product, safety, exactness, representation, and fail-closed behavior with explicit-active-change-plus-approval (GOV-R-02); and the change-vs-current-truth separation for active `specs/<change>/` vs canonical docs vs completed records (GOV-R-03). Remove the old Principle I–III product-semantics restatements.
- [X] T006 In `.specify/memory/constitution.md`, author **Working rules**: same-change update of the affected canonical owner when durable product/architecture truth changes (GOV-R-04); same-change update of `docs/TEST_MATRIX.md` when normative clauses or their regression coverage change (GOV-R-05); deterministic executable regression protection for newly established normative invariants when practically testable, with honest in-change gap reporting replacing the old separate `BLOCKED` verification stage (GOV-R-06); and the `DIRECT` honesty rule deferring coverage-class definitions to `docs/TEST_MATRIX.md` rather than restating them (GOV-R-07). Do NOT name `invariant-promotion` or `test-traceability-sync`.
- [X] T007 In `.specify/memory/constitution.md`, author **Completion**: contributor's direct use of the stock `/speckit.*` sequence (`/speckit.specify` → `/speckit.clarify` when needed → `/speckit.plan` → `/speckit.checklist` when useful → `/speckit.tasks` → `/speckit.analyze` → `/speckit.implement` → `/speckit.converge`) plus ordinary review/commit/PR; implementation, tests, canonical owners, and traceability mutually consistent; convergence reporting no remaining gaps; bundled/base Spec Kit material untouched (GOV-R-08). Retain the shorter-stock-path allowance for small mechanical/doc changes with the same guard that it must not alter product semantics, architecture, safety, executable traceability, or established/rejected decision state. Word independent review as permitted case-by-case, never mandatory (GOV-N-02).
- [X] T008 In `.specify/memory/constitution.md`, author the short **Amendment/governance** section (explicit rationale, human approval, version record, SemVer classes for governance) (GOV-R-09) and set the footer to `Version: 2.0.0 | Ratified: 2026-09-02 | Last Amended: <this change's completion date>`.
- [X] T009 Self-check the rewritten `.specify/memory/constitution.md` against the validation rules: no clause names the deleted skills, `.agents/skills/**`, `trail-plan-speckit-completion`, a repository overlay, a mandatory independent-review stage, separate verification stages, a required `promotion/` package, or a mandatory release gate; no product/architecture semantics or coverage-class definitions restated; every MUST satisfiable by stock commands, retained files, or contributor judgment (GOV-N-01…GOV-N-06, GOV-R-10)

**Checkpoint**: Constitution 2.0.0 is a coherent durable-rules-only document. US1/US2
verification can now proceed.

---

## Phase 3: User Story 1 — Contributor starts a change with stock Spec Kit (Priority: P1) 🎯 MVP

**Goal**: Delete the repository-specific process machinery (two skills, the
workflow overlay, mandatory lifecycle stages, gates, evidence packages), reduce
the router to navigation-only, and mechanically re-point the one stale
current-doc citation — so the required workflow is direct stock `/speckit.*` use.

**Independent Test**: Read only `AGENTS.md` and the constitution; the enumerated
completion obligations contain no project-specific lifecycle stage, skill-load
step, evidence-package step, or custom gate beyond the stock sequence and the
same-change update rules (spec US1 Independent Test; V-1/V-2/V-3).

### Implementation for User Story 1

- [X] T010 [P] [US1] Delete `.agents/skills/invariant-promotion/SKILL.md` (and its directory) — durable residue already folded into the constitution (FR-011, GOV-X-01/X-04)
- [X] T011 [P] [US1] Delete `.agents/skills/test-traceability-sync/SKILL.md` (and its directory) — replaced by the same-change rule GOV-R-05 (FR-012, GOV-X-02/X-04)
- [X] T012 [P] [US1] Unregister the project overlay through the supported CLI: `specify workflow overlay remove speckit trail-plan-speckit-completion` (FR-013, GOV-X-05); the CLI may modify the tracked `.specify/workflows/workflow-registry.json` if the registry state requires it (counted in V-8/T026 only if such a diff is produced); in this implementation it did not need to — the pre-change registry already represented bundled-only state
- [X] T013 [US1] Delete `.specify/workflows/overlays/speckit.yml` (repository-owned overlay source) after T012 (FR-013, GOV-X-08; D3 order: CLI unregistration before file deletion)
- [X] T014 [US1] Delete `.specify/workflows/overlays/speckit/trail-plan-speckit-completion.yml` (CLI-normalized registered copy) after T012 (FR-013, GOV-X-08)
- [X] T015 [P] [US1] Simplify `AGENTS.md`: remove the `## Skill routing` section (lines 28–33); keep a thin navigation-only router to the constitution, the active `specs/<change>/` artifacts, the canonical owners, and `docs/DEVELOPMENT.md`; retain the history/chat/archived non-authority note as navigation; add no MUST/SHALL governance rule of its own (FR-021, GOV-X-17/X-18)
- [X] T016 [P] [US1] Correct the stale pointer in `docs/TEST_MATRIX.md` §Coverage gaps (line 113): replace the ``../.agents/skills/invariant-promotion/SKILL.md`` citation with ``../.specify/memory/constitution.md`` (the relative-path convention already used elsewhere in that file), preserving the sentence's guidance (decide permanence first; prefer extending an existing contract test). Change NO coverage classification, `PC-*` mapping, architecture mapping, or test name (FR-022, GOV-X-19/X-20)
- [X] T017 [US1] Verify V-1 (stock path only): run `rg -n 'speckit\.' AGENTS.md .specify/memory/constitution.md` and `ls .opencode/commands`; every named command exists; no step says "load a skill", "run the project workflow", "produce `promotion/`", or "clear the release gate" (SC-001, GOV-R-08, GOV-X-21)
- [X] T018 [US1] Verify V-2 (zero live TEXTUAL references — the textual half of SC-003): run the quickstart V-2 deleted-machinery term search over `AGENTS.md .specify/memory/constitution.md docs/PRODUCT_CONTRACT.md docs/ARCHITECTURE.md docs/CURRENT_STATE.md docs/TEST_MATRIX.md docs/DEVELOPMENT.md docs/adr` expecting no matches; then confirm the historical carve-out still matches (`rg -l` over `specs/001-truth-review-fixes docs/migrations`). This task deliberately does not search `.specify/workflows/**`: active workflow configuration is verified by T019 (V-3), and T018 + T019 together establish SC-003 (SC-003, GOV-X-15)
- [X] T019 [US1] Verify V-3 (skills + overlay gone via supported mechanism — the active-workflow-configuration half of SC-003, jointly with T018): `ls .agents/skills`, `ls .specify/workflows/overlays`, `specify workflow overlay list speckit` (no overlays), `specify workflow resolve speckit` (single `[base]` layer, `converge` attributed to no layer), `specify workflow list` (bundled `Full SDD Cycle (speckit) v1.0.0`), and empty path-scoped diff of `.specify/templates .specify/scripts .opencode/commands .specify/workflows/speckit` (SC-003 with T018, SC-004, GOV-X-01…X-10)
- [X] T020 [US1] Verify V-5 (docs semantics preserved): path-scoped `git diff "$BASE" --` shows empty for `docs/PRODUCT_CONTRACT.md docs/ARCHITECTURE.md docs/CURRENT_STATE.md docs/adr docs/DEVELOPMENT.md`, and a pointer-only hunk for `docs/TEST_MATRIX.md`; any `DIRECT`/`PARTIAL`/`MISSING`, `PC-*`, or test-name change is a scope defect to revert (FR-022/FR-024, GOV-X-19/X-20, GOV-P-09/P-10)

**Checkpoint (MVP)**: US1 independently verifiable — stock-only path, zero live
deleted-machinery references, skills/overlay removed through the supported CLI,
router and pointer corrected, docs semantics preserved.

---

## Phase 4: User Story 2 — Maintainer finds the update rule where it is needed (Priority: P2)

**Goal**: Confirm the same-change owner/traceability update rule, the
regression-when-practically-testable rule, and the `DIRECT` honesty rule are
answerable from the constitution alone — no procedural document to discover or
load. (These rules are authored in T006; this phase verifies and reviews them.)

**Independent Test**: From the constitution alone, without opening any skill file,
answer (a) which document owns a kind of truth, (b) what must be updated when a
normative clause changes, (c) when a new invariant needs executable protection,
(d) when a `DIRECT` coverage claim is dishonest (spec US2 Independent Test; V-4).

### Implementation for User Story 2

- [X] T021 [US2] Verify V-4 (update rule locatable): confirm all four answers (a–d) resolve within `.specify/memory/constitution.md` via `git diff "$BASE" -- .specify/memory/constitution.md` (qualitative read) with no skill file consulted (SC-002, GOV-R-04…R-07, GOV-X-17/X-18)
- [X] T022 [US2] Review the constitution GOV-R-07 honesty clause: it states the `DIRECT` prohibition and defers coverage-class definitions to `docs/TEST_MATRIX.md` without duplicating class text (Principle I discipline)
- [X] T023 [US2] Review the constitution for GOV-N-01/GOV-N-02/GOV-N-03: no retained MUST is satisfiable only by the deleted skills/overlay/gates/`promotion/`; independent review and Spec Kit checklists are worded permissively (case-by-case / optional), not as mandatory stages

**Checkpoint**: US2 independently verifiable — a maintainer answers all update-rule
questions from the constitution alone.

---

## Phase 5: User Story 3 — History reader sees the first change intact (Priority: P3)

**Goal**: Confirm the withdrawn machinery's evidence (the completed first-change
record) and the planner's behavior are untouched — history is not sanitized and
the product is a non-change.

**Independent Test**: Compare historical artifacts and the default suite before/after;
the earlier artifacts are unchanged and the planner suite still passes (spec US3
Independent Test; V-6/V-7).

### Implementation for User Story 3

- [X] T024 [US3] Verify V-7 (history intact): `git diff --name-status "$BASE" -- specs/001-truth-review-fixes docs/migrations` is empty, and `specs/001-truth-review-fixes/promotion/connector-admissibility.md` plus `specs/001-truth-review-fixes/checklists/release-gate.md` still exist; runnable independently of the US1 write phase because it only asserts unchanged paths (SC-006, GOV-P-01…P-05)
- [X] T025 [US3] Verify V-6 (planner unchanged): `git diff --name-only "$BASE" -- trail-plan.scala` is empty, and the post-change `SELF-TESTS: <n> passed, 0 failed` line matches the T002 baseline using the quickstart V-6 full-log discipline (do not substitute a filtered log or rerun to recover output) (SC-005, GOV-P-06/P-07/P-08). **Ordering**: this is the *post-change* control run — start it only after the implementation write operations that can touch the work tree have settled, at minimum T010–T016 (US1 deletions, overlay removal, router edit, and the TEST_MATRIX pointer fix); running it concurrently with those writes is invalid because the suite outcome would not describe the finished tree.

**Checkpoint**: US3 independently verifiable — history byte-identical, planner
behavior unchanged.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Whole-change scope and simplicity checks, plus the implementation-time
verification report for V-1…V-8. `/speckit.analyze` (invoked by the contributor
before `/speckit.implement`) and `/speckit.converge` (invoked by the contributor
after `/speckit.implement`) are lifecycle steps around implementation, not tasks
executed during it, so they are intentionally absent here.

- [X] T026 [P] Verify V-8 (no replacement machinery): `git diff --name-status "$BASE" | grep -v 'specs/002-governance-simplification/'` shows modifications ONLY within the allowlist `{.specify/memory/constitution.md, AGENTS.md, docs/TEST_MATRIX.md}` plus, conditionally, `.specify/workflows/workflow-registry.json` if the CLI actually produced a registry edit (a CLI-produced registry diff is permitted, not required; absence of such a diff is valid and is what happened here, since the pre-change registry already represented bundled-only state; no other `.specify/workflows/**` path may be modified) and deletions ONLY `{.agents/skills/invariant-promotion/SKILL.md, .agents/skills/test-traceability-sync/SKILL.md, .specify/workflows/overlays/speckit.yml, .specify/workflows/overlays/speckit/trail-plan-speckit-completion.yml}`; confirm retained structure exists (docs set, thin AGENTS.md, constitution) and no new governance artifact/skill/gate/workflow abstraction was added (SC-007, GOV-P-11/P-12, GOV-N-06)
- [X] T027 [P] SC-002 qualitative simplicity review of `.specify/memory/constitution.md`: confirm it is materially simpler than 1.1.0, carries only the FR-002…FR-010 durable rules, and leaves no withdrawn machinery as a live obligation (no numeric line-count budget)
- [X] T028 Confirm the implementation-time verification results for V-1…V-8 — re-run the mechanical checks behind T017 (V-1), T018 (V-2), T019 (V-3), T021 (V-4), T020 (V-5), T025 (V-6), T024 (V-7), and T026 (V-8) against the finished work tree and report the pass/fail outcome of each in the implementation completion report. Do NOT edit `specs/002-governance-simplification/quickstart.md` merely to persist runtime results; the V-9 convergence check is contributor-run post-implementation acceptance, not an implementation task (SC-001…SC-007)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately. T001 (BASE) feeds every
  path-scoped diff; T002 (SELF-TESTS baseline) feeds T025.
- **Foundational (Phase 2)**: Depends on Setup. BLOCKS all story verification.
  T004→T005→T006→T007→T008→T009 are sequential (single file).
- **US1 (Phase 3)**: Depends on Foundational (constitution must already drop the
  skill references that T018/V-2 checks). This is the MVP.
- **US2 (Phase 4)**: Depends on Foundational (T006 authored the rules verified here).
  Independent of US1's deletions.
- **US3 (Phase 5)**: T024 depends on Setup only (needs BASE) and stays
  independently runnable. T025 depends on Setup (BASE + T002 baseline) **and** on
  completion of the implementation write phase that can change the work tree — at
  minimum T010–T016 (US1 changes) — because its `SELF-TESTS` run is the post-change
  control on the settled tree. Neither task depends on US1/US2 governance content.
- **Polish (Phase 6)**: Depends on US1, US2, US3 completion.

### Within User Story 1

- T012 (CLI unregistration) MUST precede T013/T014 (overlay file deletions) — D3:
  "file deletion alone is not sufficient"; registry and file state must agree.
- T010, T011, T012, T015, T016 touch different files/actions → parallelizable.
- T017–T020 (verification) run after the T010–T016 changes they check.

### Parallel Opportunities

- Setup: T003 [P] alongside T001/T002.
- US1 changes: T010, T011, T012, T015, T016 can run in parallel (distinct files/CLI);
  T013/T014 parallel with each other after T012.
- US1 verifications T017–T020 can run in parallel once changes land.
- US2 (T021–T023) and T024 [US3] are independent of each other and of US1's
  deletions → can proceed in parallel after Foundational. T025 [US3] is NOT
  parallel-safe against the US1 write phase: its post-change `SELF-TESTS` control
  run starts only after T010–T016 have settled the work tree.
- Polish: T026 and T027 [P].

---

## Parallel Example: User Story 1

```bash
# After Foundational (constitution drops skill/overlay references):
Task: "Delete .agents/skills/invariant-promotion/SKILL.md"                 # T010
Task: "Delete .agents/skills/test-traceability-sync/SKILL.md"              # T011
Task: "specify workflow overlay remove speckit trail-plan-speckit-completion"  # T012
Task: "Simplify AGENTS.md: drop ## Skill routing"                          # T015
Task: "Re-point docs/TEST_MATRIX.md §Coverage gaps (line 113)"            # T016
# THEN (after T012):
Task: "Delete .specify/workflows/overlays/speckit.yml"                     # T013
Task: "Delete .specify/workflows/overlays/speckit/trail-plan-speckit-completion.yml"  # T014
# THEN verify:
Task: "V-1 stock path only"   # T017
Task: "V-2 zero live refs"    # T018
Task: "V-3 skills+overlay gone"  # T019
Task: "V-5 docs semantics intact"  # T020
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (record BASE + SELF-TESTS baseline, confirm stock commands).
2. Complete Phase 2: Foundational — rewrite the constitution to 2.0.0 (blocks all).
3. Complete Phase 3: US1 — delete skills, withdraw overlay via CLI+files, reduce
   router, re-point TEST_MATRIX, run V-1/V-2/V-3/V-5.
4. **STOP and VALIDATE**: US1 independently testable — stock-only workflow, zero
   live deleted-machinery references.

### Incremental Delivery

1. Setup + Foundational → durable governance owner (2.0.0) in place.
2. Add US1 → stock workflow path verified → MVP demonstrable.
3. Add US2 → update-rule findability from constitution alone verified.
4. Add US3 → history + planner non-change verified.
5. Polish → scope/V-8 checks, SC-002 simplicity review, and the V-1…V-8
   implementation-completion report. (Contributor then runs `/speckit.converge` as
   post-implementation acceptance, per quickstart V-9 / SC-008 — not an
   implementation task.)

### Parallel Team Strategy

1. One owner: Setup + Foundational constitution rewrite (single file).
2. Then split: Developer A runs US1 (deletions + router + pointer + V-1/2/3/5);
   Developer B runs US2 (V-4 + honesty/GOV-N reviews);
   Developer C runs US3 (T024's V-7 preservation diff any time after Foundational;
   T025's V-6 planner control run — diff plus `SELF-TESTS` — only after Developer
   A's T010–T016 write operations have settled the work tree).

---

## Notes

- [P] tasks = different files/actions, no incomplete dependency.
- Constitution tasks (T004–T009) share one file → never parallel, strictly ordered.
- This change adds NO test harness (FR-026); all verification is mechanical.
- Never edit bundled/base material (`.specify/templates/**`, `.specify/scripts/**`,
  `.opencode/commands/**`, `.specify/workflows/speckit/workflow.yml`) (FR-014).
- Never rewrite `specs/001-truth-review-fixes/**` or `docs/migrations/**` (FR-023).
- Expected `trail-plan.scala` diff: 0 lines (FR-025).
- Stop at any checkpoint to validate the story independently before proceeding.
