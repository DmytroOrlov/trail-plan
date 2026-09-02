---

description: "Tasks for 001-truth-review-fixes"

---

# Tasks: Current Truth Review Fixes

**Input**: Design documents from `specs/001-truth-review-fixes/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/selector-materiality.md, contracts/trace-attributes-fail-closed.md, quickstart.md

**Tests**: REQUIRED by this change — the canonical tests are the default-running in-file `ts.test(...)` suite in `trail-plan.scala`; spec FR-002/FR-003/FR-004/FR-005/FR-006 mandate deterministic regressions, and Constitution Principle V requires regression protection for every newly established invariant.

**Organization**: Tasks are grouped by user story (derived from spec Acceptance Scenarios / FR groupings, since spec.md organizes requirements rather than narrative stories). Production and tests share the single file `trail-plan.scala` (Constitution Principle II); tasks touching the same file are NOT marked [P].

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Different files, no incomplete dependencies
- **[Story]**: US1 selector materiality, US2 trace fail-closed, US3 physics/audit strengthening, US4 connector boundary, US5 workflow overlay

## Path Conventions

Single-file production project: `trail-plan.scala` (production + canonical suite), `docs/` (canonical owners), `.specify/` (governance/workflow layer).

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Canonical-owner reading and baseline evidence required by the constitution before any substantive work.

- [x] T001 Read `.specify/memory/constitution.md`, canonical owners `docs/PRODUCT_CONTRACT.md`, `docs/ARCHITECTURE.md`, `docs/CURRENT_STATE.md`, `docs/TEST_MATRIX.md`, `docs/adr/0003..0005`, `trail-plan.scala`, and load `.agents/skills/invariant-promotion/SKILL.md` and `.agents/skills/test-traceability-sync/SKILL.md`
- [x] T002 Verify findings #1–#6 against current code and tooling paths (`fetchTraceAttributes` route-shape substitution at trail-plan.scala:1595, selector counterexample via `selectLocalMarginalDrop` at trail-plan.scala:2365, `connectorDominates`/`pruneConnectors` at trail-plan.scala:1967/2014, `specify workflow resolve speckit` showing `[base]`-only stopping at `implement`) and record the pre-change 22/22 default-suite baseline

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: The pre-fix red regression that anchors the core product change; Constitution Principle V requires the failing test before production change.

**⚠️ CRITICAL**: US1 production work must not begin before this red regression exists.

- [x] T003 Add the deterministic `PC-SELECT-01` counterexample regression "local selector preserves an established elbow under a near-zero comfort tail" in `trail-plan.scala` (suite, base frontier `(10,100),(20,80),(30,60),(100,59)` + extension `(1000,58.999999)`; fixture labels `elbow`=(30,60), `tail1`=(100,59)) and prove it fails pre-fix (extended frontier selects `tail1`); record pre-fix selection evidence per research.md (FR-002; trail-plan.scala:3637)

**Checkpoint**: Foundation ready — pre-fix red test in place; user stories can proceed.

---

## Phase 3: User Story 1 - Selector materiality resolution (Priority: P1) 🎯 MVP

**Goal**: Post-search product selection applies the approved 1.0-second `candHard` materiality resolution between exact frontier formation and local marginal-drop selection, resolving the reproduced counterexample for both base and extended frontiers without quantizing search.

**Independent Test**: quickstart.md AS2 — counterexample test passes for base AND extended frontiers (both select `elbow`); boundary test shows exactly-1.0 s gain retained, sub-1.0 s coalesced; "exact no-horizon rider search + local marginal-drop selector" and "rider product selector preserves guardrails while improving candHard" still pass (search unchanged per `PC-SEARCH-02`).

### Implementation for User Story 1

- [X] T004 [US1] Implement the 1.0 s `candHard` materiality resolution step per contracts/selector-materiality.md (retain first point; later point retained only if `candHard` gain ≥ 1.0 s vs last retained; sub-1.0 s coalesces into the lower-transfer plateau representative; never mutate/quantize `candHard`) in `trail-plan.scala` alongside `selectLocalMarginalDrop` (trail-plan.scala:2365)
- [X] T005 [US1] Wire the materiality resolution into both post-search call sites in `trail-plan.scala`: rider-DP `productFront` → `selectLocalMarginalDrop` (trail-plan.scala:2500) and `chooseFinal` `productFront` → `selectLocalMarginalDrop` (trail-plan.scala:2533), preserving existing head-selection for degenerate (<2 meaningful points) frontiers and leaving `terminalIsEligibleUpgrade`, dominance, safety, and all pre-selection state untouched (FR-003)
- [X] T006 [US1] Add the deterministic materiality boundary regression in the default suite of `trail-plan.scala` using exactly representable `Double` fixtures from data-model.md: retained case `…,(30,60),(100,59.0)` (60.0−59.0==1.0 bit-exact → new meaningful point) and coalesced case `…,(30,60),(100,59.000001)` (gain < 1.0 s → plateau onto (30,60) chain), and confirm T003 now passes for base and extended frontiers (FR-003, AS2)

**Checkpoint**: US1 functional independently — selector counterexample and boundary pinned; `PC-SELECT-01` canonical text already present in `docs/PRODUCT_CONTRACT.md` — `PC-SELECT-01`.

---

## Phase 4: User Story 2 - Trace attributes fail-closed (Priority: P1)

**Goal**: A `/trace_attributes` 2xx response without its own valid usable shape fails closed; route geometry never substitutes for trace geometry (conforms implementation to `PC-SAFE-01`; canonical text unchanged, FR-001).

**Independent Test**: quickstart.md AS1 — regression "trace attributes fail closed on missing or invalid returned shape" passes; rejection messages name the trace response defect, never route-shape reuse.

### Implementation for User Story 2

- [X] T007 [US2] Add the fail-closed parser regression in the default suite of `trail-plan.scala` covering missing, malformed, non-finite, out-of-range (lat ∉ [-90,90] or lon ∉ [-180,180]), and degenerate (<2 decoded polyline6 points) returned shapes, each asserting `Left` rejection with no inheritance of the request `/route` shape (contracts/trace-attributes-fail-closed.md, data-model.md validation rules, AS1) — write first, ensure it fails
- [X] T008 [US2] Minimally replace the `case _ => Right(shape)` route-shape substitution in `fetchTraceAttributes` (trail-plan.scala:1618-1620) with fail-closed validation of the response's own `shape` (present, string, polyline6 decode ≥ 2 finite in-range points; `Left` naming the trace-response defect), keeping existing edges/non-2xx handling unchanged (FR-001)

**Checkpoint**: US1 and US2 both independently functional.

---

## Phase 5: User Story 3 - Strengthened physics & audit regressions (Priority: P2)

**Goal**: Existing regressions become behaviorally load-bearing: ~30 m chunking and short-tail merging fail if materially changed, and the real `audit` path (not the recomputation helper) rejects inconsistent stored rider metrics.

**Independent Test**: quickstart.md AS3 + AS4 — transfer-physics test fails on material chunk-boundary or tail-merge changes; audit-path regression yields `rider metrics recomputation mismatch`.

### Implementation for User Story 3

- [X] T009 [US3] Strengthen "transfer physics uses 30m grade chunks and downhill coasting" in the default suite of `trail-plan.scala` (trail-plan.scala:3325) using the concrete deterministic fixtures pinned in data-model.md §Transfer-physics fixtures (30 m window necessity + short-tail merge pair with asserted `duration`/`candHard`/`spike` observables), per research.md finding that the current test proves only smoothing/coasting (FR-004, AS3)
- [X] T010 [US3] Add the real-audit rejection regression in the default suite of `trail-plan.scala`: construct the inconsistent stored `RiderTerminal` fixture from data-model.md, drive it through `audit` (trail-plan.scala:2609) — not the recomputation helper used by the existing test at trail-plan.scala:3537 — and assert `rider metrics recomputation mismatch` from the `auditSameRider` check (trail-plan.scala:2670) (FR-005, AS4)

**Checkpoint**: Regression strength raised without production behavior change (both tasks are test-only; no [P] — same file).

---

## Phase 6: User Story 4 - Connector dominance admissibility boundary (Priority: P2)

**Goal**: The durable pre-search connector-dominance admissibility boundary is evidenced by a continuation-retention regression (T011); proposed canonical wording is prepared change-scoped only (T012), and the canonical landing in `docs/ARCHITECTURE.md` occurs through the invariant-promotion step (T015, FR-008 ordering) — documentation/promotion only, no planner behavior change (FR-009).

**Independent Test**: quickstart.md AS5 — continuation regression passes with `pruneConnectors` active; the continuation-distinct alternative appears in the `pruneConnectors` output handed to search.

### Implementation for User Story 4

- [X] T011 [US4] Add the connector-continuation regression in the default suite of `trail-plan.scala`, constructing a fully explicit deterministic `Connector` pair (A, B) with every field relevant to `connectorDominates` fixed by the test (bit-exact `ascentM` gate, transfer via `rider.duration`, `roadStressSeconds`, `effectiveWall`, `t120`/`t140`/`t160`, `candHard`, `spike`, and the `streak120`/`streak140` components; trail-plan.scala:1967-1977), plus distinct ids/profiles so the pair survives bit-exact semantic dedupe inside `pruneConnectors`. The regression must establish the chain: connector A is locally no-worse in the relevant simple local costs **+** a continuation-sensitive interchangeability resource differs (including non-bit-exact ascent where applicable) **→** `connectorDominates(A, B)` is false **→** B is present in the `pruneConnectors` output (trail-plan.scala:2014). Membership in that `pruneConnectors` output is the required observable, because it is the pre-search connector graph handed to downstream exact eligibility (no separate downstream probe). The exact synthetic `Connector` fixture values used by the regression MUST be recorded as change evidence before T012 is finalized; those fixture values are regression evidence only — they are not new product semantics and not canonical architecture truth (evidencing the admissibility boundary under which continuation-sensitive alternatives cannot be collapsed; data-model.md connector continuation fixture; FR-006, AS5; regression is the promotion evidence for T015 — no invented or changed connector-dominance production behavior, FR-009)
- [X] T012 [US4] Finalize the change-scoped promotion evidence package for the durable admissibility boundary at `specs/001-truth-review-fixes/promotion/connector-admissibility.md` (proposed canonical wording for the Connector graph section of `docs/ARCHITECTURE.md`), following this explicit lifecycle: (1) before T011 passes, the file is only a preliminary/template draft; (2) mere existence of the file does NOT complete T012; (3) T011 supplies the deterministic regression evidence; (4) after T011 passes, T012 reconciles and finalizes the proposed wording against the actual T011 evidence, including recording the exact synthetic `Connector` fixture values as change evidence; (5) only after that reconciliation may T012 be marked complete; (6) the file remains change-scoped and non-canonical — this task MUST NOT edit `docs/ARCHITECTURE.md` or establish canonical truth directly; (7) T015 via `.agents/skills/invariant-promotion/SKILL.md` is the only step that may perform the canonical landing in `docs/ARCHITECTURE.md`; the semantic classification belongs to T015 (different files, so it may proceed in parallel with `trail-plan.scala` tasks; FR-006/FR-008 ordering)

**Checkpoint**: Boundary regression pinned (T011) and proposed wording prepared (T012); canonical `docs/ARCHITECTURE.md` landing deferred to the T015 invariant-promotion step.

---

## Phase 7: User Story 5 - Workflow completion overlay (Priority: P3)

**Goal**: The repository-owned integration point extends the reachable `speckit` "Full SDD Cycle" beyond `implement` through the constitution's later lifecycle stages; bundled material stays untouched (2026-09-03 clarification, research.md overlay decision).

**Independent Test**: quickstart.md AS6 — `specify workflow resolve speckit` shows project-overlay layer attribution and a resolved step graph extending past `implement` to converge, independent review, and executable-regression/traceability verification; `specify workflow info speckit` reflects the extended lifecycle; `specify workflow overlay list` shows the overlay registered.

### Implementation for User Story 5

- [X] T013 [P] [US5] Author the project-local overlay YAML in `.specify/workflows/overlays/speckit.yml` extending the `speckit` step graph through converge → independent review → executable-regression/traceability verification per the constitution's Spec Kit Change Lifecycle (do NOT edit bundled `.specify/workflows/speckit/workflow.yml`)
- [X] T014 [US5] Register via `specify workflow overlay add .specify/workflows/overlays/speckit.yml` and verify `specify workflow overlay list`, `specify workflow info speckit`, and `specify workflow resolve speckit` attribute the extended steps to the project overlay, not `[base]` (FR-007, AS6)

**Checkpoint**: Lifecycle completeness is now expressed in a repository-owned definition.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Constitution-mandated promotion/synchronization order and final verification.

- [X] T015 Apply `.agents/skills/invariant-promotion/SKILL.md` as the semantic classification and canonical-landing step for all newly established durable knowledge from this change, covering two architecture concerns plus the remaining classifications:
  - **(A) Selector pipeline placement (architecture synchronization of the already-approved `PC-SELECT-01` product change — NOT a re-promotion of the product rule):** land the durable Post-search selection boundary in the Post-search selection section of `docs/ARCHITECTURE.md` as: exact eligible Pareto frontier → 1.0 s `candHard` materiality resolution → local marginal-drop selector. Record stage placement only; do not duplicate the full `PC-SELECT-01` algorithmic wording. The `PC-SELECT-01` product text itself is already canonical in `docs/PRODUCT_CONTRACT.md` — here it is only reconciled/verified against the suite (selector materiality resolution invariance).
  - **(B) Connector-dominance admissibility boundary:** classify the T011 evidence and land the canonical wording from the T012 draft at `specs/001-truth-review-fixes/promotion/connector-admissibility.md` into the Connector graph section of `docs/ARCHITECTURE.md`.
  - **Also route:** PC-SAFE-01 trace fail-closed conformance (canonical contract text unchanged per FR-001).
  MUST run before traceability sync (FR-008, Constitution III/IV)
- [X] T016 Apply `.agents/skills/test-traceability-sync/SKILL.md` to synchronize `docs/TEST_MATRIX.md` with `docs/PRODUCT_CONTRACT.md` and `docs/ARCHITECTURE.md` for every new/strengthened/renamed regression from T003, T006, T007/T008, T009, T010, T011 (FR-008, Constitution V)
- [X] T017 Remove any temporary pre-fix diagnostics (e.g., pre-fix selection dumps used for T003 evidence) from `trail-plan.scala` after adjudication (Constitution IV)
- [X] T018 Run the full default suite `./trail-plan.scala --input trails --output /tmp/trail-plan-tasks --valhalla-url http://localhost:8002` and confirm all pre-existing plus new regressions pass; the run then stopping at the Valhalla `/status` boundary is expected (FR-009, AS7, quickstart.md)
- [X] T019 [conditional] With a reachable Valhalla, run the canonical production comparison and confirm the five `PC-OUT-01` outputs as baseline evidence only (not a completion gate, per plan.md step 8). Skip criterion: if no Valhalla service is reachable (production stops at the `/status` boundary as in T018), record T019 as `SKIPPED (Valhalla unreachable)`; the skip does not block completion — AS7/`FR-009` remains the only suite gate
- [X] T020 Scope audit: confirm `chooseAssignment`, unrelated planner semantics, historical rationale, migration history, and unrelated matrix gaps are untouched except behavior-preserving mechanical adjustments forced by direct compile/test dependency (FR-009)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: complete (T001–T002 done)
- **Foundational (Phase 2)**: complete (T003 red regression in place) — was blocking US1
- **User Stories (Phases 3–7)**: all unblocked by Phase 2 completion
  - US1 depends on T003's red test (already exists)
  - US2, US3, US4, US5 have no cross-story dependencies
  - US1 → T004 → T005 → T006 (same file, sequential)
  - US2 → T007 (test-first, must fail) → T008
- **Polish (Phase 8)**: T015 requires all story phases that create durable knowledge (US1, US2, US4, including the T012 proposed wording); T016 strictly after T015 (FR-008 order); T018 after all desired stories

### User Story Dependencies

- **US1 (P1)**: after Foundational — independent of US2–US5
- **US2 (P1)**: independent — touch-points are `fetchTraceAttributes` only
- **US3 (P2)**: independent — test-only strengthening of existing named tests
- **US4 (P2)**: T011 before T012 (T011 supplies the deterministic regression evidence; the mere existence of the T012 draft file does not complete T012 — it is complete only after reconciling/finalizing the wording against actual T011 evidence; canonical landing is T015 only); T012 parallel to all `trail-plan.scala` tasks (different files)
- **US5 (P3)**: fully independent (`.specify/` layer only)

### Within Each User Story

- Tests written and failing before the production change (T007→T008; T003→T004/T005)
- Frontier→selection wiring order: materiality-resolution function (T004) before call-site wiring (T005) before boundary pinning (T006)
- Proposed wording (T012) after its regression evidence (T011); canonical `docs/ARCHITECTURE.md` landing via T015 after the invariant-promotion classification

### Parallel Opportunities

- After Phase 2: US1, US2, US3, US4, US5 can proceed in parallel across stories (by different agents/members; within `trail-plan.scala` tasks serialize)
- T012 (after T011 passes; change-scoped draft, no canonical-owner edits) ∥ any `trail-plan.scala` task
- T013 [P] (overlay YAML) ∥ any code/docs task
- T015 → T016 are strictly sequential; T017 can run parallel with T016 (different concerns, but same file as nothing else open at that point)

---

## Parallel Example: post-Foundational

```bash
# Different files / independent stories can run concurrently:
Task: "T012 Draft connector admissibility wording (after T011; change-scoped)"
Task: "T013 Author overlay YAML in .specify/workflows/overlays/speckit.yml"
# trail-plan.scala work stays serialized within/across US1-US4:
Task: "T004 materiality resolution in trail-plan.scala"   # then T005, T006
Task: "T007 trace fail-closed regression in trail-plan.scala"  # then T008
```

---

## Implementation Strategy

### MVP First (User Story 1 only)

1. Phases 1–2 already complete.
2. Complete Phase 3 (T004→T006): selector materiality implemented and pinned.
3. STOP and VALIDATE: AS2 via quickstart targeted check; the counterexample goes green.
4. MVP delivered: the core accepted product fix.

### Incremental Delivery

1. US1 (MVP) → validate AS2
2. + US2 → validate AS1 (safety fail-closed)
3. + US3 → validate AS3/AS4 (regression strength)
4. + US4 → validate AS5 (continuation evidence + change-scoped proposed wording; canonical promotion lands in Phase 8 via T015)
5. + US5 → validate AS6 (workflow overlay)
6. Phase 8: promotion → traceability sync → cleanup → full suite (AS7) → scope audit

### Parallel Team Strategy

With multiple developers: US1–US5 owners work concurrently on their stories; only `trail-plan.scala` requires merge coordination (single-file project), while T012/T013 need none.

---

## Notes

- [P] tasks = different files, no incomplete dependencies; the single-file production shape (Constitution II) suppresses [P] among `trail-plan.scala` tasks
- Canonical owners updated: `docs/PRODUCT_CONTRACT.md` `PC-SELECT-01` already carries the approved materiality text (done during plan/clarify); `docs/ARCHITECTURE.md` update lands via T015 (T012 is a preliminary draft until T011 passes, finalized only by reconciliation with T011 evidence — file existence alone does not complete it); matrix sync is T016
- Every newly established invariant has a deterministic regression (T003/T006, T007, T011); if any promotion proves structurally untestable, report BLOCKED per Constitution V instead of normal completion
- Commit after each task or logical group; stop at any checkpoint to validate a story independently via quickstart.md

---

## Phase 9: Convergence

- [X] T021 Align the documented default-suite run instructions with the actual CLI argument contract so the AS7/T018 verification command is reproducible as written: update the change-scoped `specs/001-truth-review-fixes/quickstart.md` "Run the default regression suite" command to include the required `--valhalla-url` argument (the CLI requires it in all modes, trail-plan.scala:4073, while quickstart.md and T018 omit it); do NOT change production CLI parsing (FR-009) per AS7 / T018 verification evidence (partial)

---

## Phase 10: Independent Review Remediation

**Context**: After the Phase 9 convergence pass, an independent technical review confirmed production behavior is correct but found two blocking regression-evidence defects (selector production wiring is not regression-protected; the connector ascent-gate test is not causal) and one non-blocking traceability overstatement (the trace regression does not drive the `fetchTraceAttributes` HTTP-consumption call site). This phase appends remediation tasks only; no completed task checkbox is altered.

- [X] T022 [US1] Add a deterministic production-wiring regression for `PC-SELECT-01` in the default suite of `trail-plan.scala` that drives an ACTUAL production product-selection entry point — preferably `chooseFinal` (trail-plan.scala:2563) if, after inspecting the code, it is the smallest practical surface — with fixtures constructed to pass `eligibleUpgrade` against a suitable baseline so the fed frontier is equivalent to `(transfer=10, candHard=100), (20,80), (30,60), (100,59), (1000,58.999999)` (established elbow `(30,60)`). Required properties: with the production `resolveComfortMateriality` wiring present at the production selection path, the entry point selects the established elbow; if `resolveComfortMateriality` is removed from that production path, this regression must fail (mutation-checked in T025). The test must NOT prove wiring merely by manually composing `selectLocalMarginalDrop(resolveComfortMateriality(...))` in the test body (the reviewer's wiring removal currently keeps the default suite 27/27 green — this task closes that gap). Do not change the 1.0-second product rule or any production selection semantics (contract `PC-SELECT-01` unchanged; FR-009)
- [X] T023 [US4] Replace/augment the connector-continuation regression in the default suite of `trail-plan.scala` with a CAUSAL discriminating `Connector` pair (A, B): A strictly better than B in at least one normal dominance resource (preferably transfer duration), no worse in every other compared monotone resource, and bit-exact-different `ascentM`; B otherwise dominance-eligible if the ascent equality gate were ignored (the current equal-everything-but-ascent fixture is not causal: with the gate disabled in production the suite still passes 27/27). Required assertions: with the real ascent admissibility gate `connectorDominates(A, B) == false` (trail-plan.scala:1984-1986); B survives the `pruneConnectors` output (trail-plan.scala:2031); an appropriate control proves normal dominance still collapses when ascent is bit-exact equal. The fixture must be constructed so that removing/bypassing the ascent equality gate makes the regression fail (mutation-checked in T025). After the regression passes, update the change-scoped promotion evidence `specs/001-truth-review-fixes/promotion/connector-admissibility.md` with the ACTUAL final fixture values and causal evidence (change-scoped only; no `docs/ARCHITECTURE.md` edit). Do NOT change `connectorDominates` production semantics unless implementation reveals current production behavior contradicts the active contract — if so, STOP and report BLOCKED (FR-009)
- [X] T024 Make trace coverage wording strictly honest (documentation/traceability only; the reviewer restored the old route-shape substitution at the `fetchTraceAttributes` call site and the default suite still passed, proving the suite drives `traceResponseShape`/`traceShapeGeometry` directly, not the HTTP-consumption call site): in `docs/TEST_MATRIX.md`, state that the trace response-shape helper surface is directly regression-protected and do NOT claim the `fetchTraceAttributes` HTTP-consumption call site is directly driven unless a regression actually driving it is added; preserve `PC-SAFE-01` as PARTIAL unless substantially broader direct coverage is implemented; inspect the active change contract/verification wording (`contracts/trace-attributes-fail-closed.md`, spec.md, plan.md, quickstart.md) and remove any claim that the `fetchTraceAttributes` call path itself is regression-driven if such a claim exists. Apply `.agents/skills/test-traceability-sync/SKILL.md` for the matrix wording change. Do NOT add a local HTTP server or new Valhalla test infrastructure solely to close this finding — documentation honesty is sufficient for this change
- [X] T025 Final remediation verification: run the full default suite using the repository-documented command (`docs/DEVELOPMENT.md` / quickstart.md) and confirm all tests pass; run the production comparison if Valhalla is reachable (record as skipped-if-unreachable, as in T019); re-check every `docs/TEST_MATRIX.md` classification against its own DIRECT/PARTIAL definition and correct any remaining overstatement; confirm no out-of-scope planner semantics changed (FR-009 scope audit). Blocking completion criterion — mutation-sensitive evidence: (1) removal of the production `resolveComfortMateriality` wiring from the production selection path is caught by the new T022 selector regression; (2) removal/bypass of the `connectorDominates` ascent admissibility gate is caught by the T023 connector regression. Mutation experiments MUST be performed on temporary copies outside the repository; do not leave mutation code in the working tree (Constitution IV/V)
