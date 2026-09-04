# Feature Specification: Governance Simplification — Back to Stock Spec Kit

**Feature Branch**: `002-governance-simplification`

**Created**: 2026-09-04

**Status**: Completed

**Input**: User description: "Simplify Trail Plan's Spec Kit governance back toward the stock Spec Kit workflow. This is a governance/process-only change. Do not change trail-plan.scala or planner product/routing semantics. Current repository-specific governance was intentionally made stricter during the first Spec Kit change, but that experiment added process machinery that is not justified as a permanent Trail Plan requirement. This change should remove that machinery while preserving the small project-specific rules that are actually useful."

## What This Change Is

This change alters **only how the repository is governed**, not what the planner
product does. The read units are the governance documents and process machinery:
the constitution, the repository router, the project skills, the Spec Kit
workflow overlay, and the traceability map's process pointers.

No product guarantee, architectural truth, established decision, or executable
planner behavior is in scope for modification.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Contributor starts a change with stock Spec Kit (Priority: P1)

A contributor (human or coding agent) begins a substantive Trail Plan change.
They open the repository router, follow it to the constitution, and find a short
statement of durable project rules plus the desired contributor workflow: direct
use of the stock `/speckit.*` commands in sequence —

  /speckit.specify
  -> /speckit.clarify when clarification is needed
  -> /speckit.plan
  -> /speckit.checklist when useful
  -> /speckit.tasks
  -> /speckit.analyze
  -> /speckit.implement
  -> /speckit.converge
  -> ordinary review / commit / PR as appropriate

This sequence is the contributor running the stock commands directly; the
specification makes no claim that the installed bundled/base Spec Kit automation
workflow itself contains every command in this sequence. Nothing in the
governance text tells them to load a project skill, run a repository-owned
workflow overlay, produce a `promotion/` evidence package, or clear a custom
release gate.

**Why this priority**: This is the whole point of the change. If the governance
entry point still pushes contributors into the extra machinery, the experiment
continues by default even after the machinery is deleted.

**Independent Test**: Read only the router and the constitution, then list the
required completion obligations for a new change. The list must contain no
project-specific lifecycle stage, skill-load step, evidence-package step, or
custom gate beyond the stock sequence and the same-change update rules.

**Acceptance Scenarios**:

1. **Given** the simplified governance is in place, **When** a contributor
   enumerates the required workflow for a new substantive change, **Then** the
   result is direct use of the stock `/speckit.*` commands ending in
   `/speckit.converge`, with ordinary review/commit/PR as appropriate and no
   Trail Plan-owned overlay or extra mandatory project lifecycle stage.
2. **Given** the simplified governance is in place, **When** a contributor
   searches the router and constitution for the deleted project skills, the
   repository overlay, the mandatory independent-review stage, the separate
   verification stages, the required `promotion/` package, and the mandatory
   release gate, **Then** no live (current-obligation) reference to any of them
   remains in current governance or current-truth artifacts; mentions inside
   historical records are outside the scope of this check.
3. **Given** a new change that establishes a durable product or architecture
   invariant, **When** the contributor works through it, **Then** the regression
   protection and traceability updates appear as ordinary work inside that
   change, not as separate post-implementation gates.

---

### User Story 2 - Maintainer finds the update rule where it is needed (Priority: P2)

A maintainer changes a normative clause in the product contract or architecture,
or changes the regression coverage that protects such a clause, and wants to know
what else must move with it. The answer is one short rule in the constitution:
the canonical owner and the traceability map are updated in the same change — no
separate procedural document to discover, load, or interpret first.

**Why this priority**: The same-change synchronization rules are the useful part
of the current governance. They only survive this simplification if they are
stated directly where maintainers already look.

**Independent Test**: From the constitution alone, answer these without
consulting any skill file: (a) which document owns a given kind of truth;
(b) what must be updated when a normative clause changes; (c) when a new
invariant needs executable protection; (d) when a "direct" coverage claim is
dishonest.

**Acceptance Scenarios**:

1. **Given** a change that edits a normative product or architecture clause,
   **When** completion is assessed, **Then** the same change is required to
   update the affected canonical owner and the traceability map.
2. **Given** a newly established normative invariant that is practically
   testable, **When** the change completes, **Then** it carries deterministic
   executable regression protection.
3. **Given** a traceability entry claiming direct coverage of a property,
   **When** the default-running suite would still pass after that property
   regressed, **Then** the governance rules require the claim to be downgraded
   rather than left overstated.

---

### User Story 3 - History reader sees the first change intact (Priority: P3)

A reader examining how the first Spec Kit change actually ran opens the
completed change record and sees it exactly as it was finished: its promotion
evidence, its release-gate checklist, its independent-review and verification
steps, and the governance text of that era. The new, looser rules for future
work do not rewrite that record.

**Why this priority**: The removed machinery is genuine evidence about how the
project operated. Losing that evidence would trade one form of drift for
another.

**Independent Test**: Compare the completed change record and the historical
migration records before and after this change; the earlier artifacts are
unchanged.

**Acceptance Scenarios**:

1. **Given** the completed first change record contains a `promotion/`
   directory and a release-gate checklist, **When** this simplification is
   applied, **Then** those artifacts remain present and unmodified.
2. **Given** historical documents describe the stricter process, **When** this
   simplification is applied, **Then** they remain intact as history and are not
   rewritten to match the future process.

---

### Edge Cases

- What if a governance document that is *current* still points at a deleted
  skill or overlay? — Its stale pointer is corrected mechanically, without
  changing any coverage classification, clause mapping, or technical statement.
- What if a reference to deleted machinery appears inside a *historical* change
  or migration record? — It is left as-is; history is not sanitized.
- What if the bundled/base Spec Kit workflow itself already contains review
  gates? — Base workflow material is not modified; only repository-owned overlay
  additions are withdrawn.
- What if a future change is genuinely risky and deserves independent review? —
  Independent review remains permitted and encouraged case-by-case; it simply is
  no longer a mandatory completion phase.
- What if a contributor finds a Spec Kit checklist useful for one change? —
  Checklists remain available; only the mandatory custom release-gate layer is
  withdrawn.
- What if a needed invariant cannot be expressed as a deterministic regression?
  — The rule is scoped to "when practically testable"; the gap is reported
  honestly rather than blocked behind a separate verification stage.
- What if deletion of the overlay is done by hand instead of the supported CLI?
  — The requirement is that the overlay is unregistered/disabled through the
  supported mechanism **and** repository-owned overlay files are removed; file
  deletion alone is not sufficient.
- What if this governance change appears to weaken a "no silent weakening"
  rule? — The rule being retained protects *product, safety, exactness,
  representation, and fail-closed* behavior; deliberately relaxing process
  machinery is explicit, human-requested, and recorded, not silent.

## Requirements *(mandatory)*

### Functional Requirements

**Retained durable governance**

- **FR-001**: The constitution MUST be substantially shorter than today and MUST
  state only durable, project-specific governance.
- **FR-002**: The constitution MUST retain canonical ownership and source
  precedence: each kind of knowledge has one owner, other artifacts may
  reference but not silently redefine it, and historical snapshots, archived
  change artifacts, and prior chat context are not current authority.
- **FR-003**: The constitution MUST retain a no-silent-weakening rule covering
  established product, safety, exactness, representation, and fail-closed
  behavior: such behavior MUST NOT be weakened, approximated, or replaced for
  implementation convenience; an intentional change must be explicit in the
  active change and approved.
- **FR-004**: The constitution MUST retain the separation that active specs
  describe **changes** while canonical docs describe **current truth**, and that
  a completed change record does not become a competing source of truth.
- **FR-005**: Where a change modifies durable product or architecture truth,
  the constitution MUST require the corresponding canonical owner to be updated
  in the same change.
- **FR-006**: Where a change modifies normative contract or architecture
  clauses or their regression coverage, the constitution MUST require
  `docs/TEST_MATRIX.md` to be updated in the same change.
- **FR-007**: The constitution MUST require newly established normative
  invariants to receive deterministic executable regression protection when
  practically testable.
- **FR-008**: The constitution MUST prohibit claiming DIRECT regression coverage
  when the default-running suite would remain green after that property
  regressed.
- **FR-009**: The constitution MUST define completion as the contributor's
  direct use of the stock `/speckit.*` commands through `/speckit.converge`,
  with no Trail Plan-owned workflow overlay and no extra mandatory project
  lifecycle stages, requiring implementation, tests, canonical owners, and
  traceability to be mutually consistent, with convergence reporting no
  remaining gaps. Bundled/base Spec Kit material remains untouched.
- **FR-010**: The constitution MUST retain a short governance/amendment section
  (explicit rationale, human approval, version record) so that future changes to
  governance remain deliberate.

**Machinery removed**

- **FR-011**: The project-specific `invariant-promotion` skill MUST be removed,
  and the useful rules it carried MUST be represented directly and concisely in
  the constitution instead of through a separate procedural skill.
- **FR-012**: The project-specific `test-traceability-sync` skill MUST be
  removed and replaced by the same-change rule in FR-006.
- **FR-013**: The custom Spec Kit workflow overlay MUST be removed completely,
  including all repository-owned overlay files, and the project overlay MUST be
  unregistered or disabled using the supported Spec Kit CLI.
- **FR-014**: Bundled/base Spec Kit workflow material MUST NOT be modified.
- **FR-015**: The repository MUST NOT require a mandatory independent-review
  lifecycle stage; independent review MAY still be used when warranted by a
  risky change.
- **FR-016**: The repository MUST NOT require separate mandatory lifecycle
  stages for executable-regression verification or traceability verification;
  such work belongs in ordinary change tasks and completion criteria.
- **FR-017**: A `promotion/` evidence package MUST NOT be treated as a required
  pattern for new changes.
- **FR-018**: The custom release-gate checklist MUST NOT be treated as
  mandatory; Spec Kit checklists remain available when useful for a particular
  change.
- **FR-019**: The governance text MUST NOT describe any retained obligation that
  can only be satisfied by the deleted skills, overlay, gates, or evidence
  packages.

**Repository structure and routing**

- **FR-020**: The useful project-specific structure MUST be kept:
  `docs/PRODUCT_CONTRACT.md`, `docs/ARCHITECTURE.md`, `docs/CURRENT_STATE.md`,
  `docs/TEST_MATRIX.md`, `docs/adr/`, `docs/DEVELOPMENT.md`, a thin `AGENTS.md`
  router, and `.specify/memory/constitution.md`.
- **FR-021**: `AGENTS.md` MUST be simplified to remove routing to deleted skills
  while remaining a thin router to the constitution, the active spec, the
  canonical owners, and development tooling; it MUST continue to own no
  governance rules itself.
- **FR-022**: Current-state documents that still point at deleted machinery MUST
  have those pointers corrected mechanically, WITHOUT changing any product
  requirement, architecture claim, coverage classification, or mapping.

**Preservation and non-scope**

- **FR-023**: Historical artifacts of completed changes, including
  `specs/001-truth-review-fixes/`, and historical migration records MUST remain
  intact and MUST NOT be retroactively rewritten to match the simplified
  process.
- **FR-024**: This change MUST NOT modify planner product semantics, product
  contract requirements, architecture technical truth, current-state technical
  status, or ADR rationale.
- **FR-025**: This change MUST NOT modify `trail-plan.scala` except where a
  purely mechanical stale governance reference must be removed; the expected
  outcome is no planner or code change at all.
- **FR-026**: This change MUST NOT introduce replacement workflow machinery, new
  skills, new lifecycle gates, or a new governance abstraction.
- **FR-027**: This change proceeds under the currently effective constitution,
  and the simplified rules become repository governance for future work only
  upon completion of this change.

### Key Entities

- **Constitution** — the single governance entry point; after this change it
  holds ownership/precedence, no-silent-weakening, same-change update rules,
  regression-honesty rules, stock-workflow completion, and amendment procedure.
- **Repository Router (`AGENTS.md`)** — navigation only; points at the
  constitution, active change, canonical owners, and development tooling.
- **Canonical Owners** — the docs set (product contract, architecture, current
  state, test matrix, ADRs) plus the single production implementation; unaffected
  in substance by this change.
- **Project Skills** — two process skills that currently mediate invariant
  promotion and traceability sync; withdrawn, with their durable rules folded
  into the constitution.
- **Workflow Overlay** — repository-owned steps appended to the bundled Spec Kit
  cycle (converge, independent-review gate, verification stage, traceability
  gate); withdrawn via the supported CLI.
- **Active Change Record** — a `specs/<change>/` directory describing one
  proposed change; flow-forward record, never current truth.
- **Historical Change Record** — a completed change directory (e.g.
  `specs/001-truth-review-fixes/`) preserved as evidence, including the stricter
  machinery it used.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A contributor who reads only the router and the constitution can
  name the full required workflow for a new change, and that workflow is direct
  use of the stock `/speckit.*` commands with no Trail Plan-owned overlay and no
  repository-specific extra mandatory stage.
- **SC-002**: The simplified constitution is materially simpler than the current
  version: it contains only the durable governance retained by FR-002 through
  FR-010, and the withdrawn skills, workflow stages, gates, evidence-package
  requirements, and other deleted machinery do not remain as live obligations.
- **SC-003**: Zero live references to the deleted skills, the repository
  overlay, the mandatory independent-review stage, the separate verification
  stages, the required `promotion/` package, or the mandatory release gate
  remain in the router, the constitution, the docs set, or the repository's
  active workflow configuration. "Live references" here means references in
  current governance/current-truth artifacts and active workflow configuration
  that present the deleted machinery as a current obligation. Historical records
  are excluded and remain intact, including `specs/001-truth-review-fixes/**`,
  historical migration records, and other explicitly historical change or
  rationale artifacts; their text may continue to mention the removed skills,
  overlay, gates, or promotion process because it records how the repository
  operated at that time. Current governance does not treat those historical
  references as current obligations.
- **SC-004**: The project skills directory and repository overlay configuration
  contain no Trail Plan-owned process machinery, and the supported workflow
  listing shows no active project overlay for the stock cycle.
- **SC-005**: The planner is untouched behaviorally: the change records no
  modification to planner product semantics, and the default regression suite
  produces the same pass/fail result before and after this change.
- **SC-006**: Historical preservation holds: 0 files added, changed, or deleted
  under `specs/001-truth-review-fixes/`, and no historical migration record is
  rewritten.
- **SC-007**: No new governance artifact, skill, gate, or workflow abstraction is
  added: the set of governance entry points after the change is a subset of
  before, plus this change's own record.
- **SC-008**: Convergence of this change reports no remaining gaps between its
  specification, the resulting governance text, and the deleted machinery.

## Assumptions

- Dependencies: the stock Spec Kit command set and its supported workflow
  management mechanism remain available in this repository's installation; the
  docs set, the router, and the constitution exist in their current locations.
  No external service, planner data, or runtime is required to complete this
  change.
- The feature description is itself the human instruction authorizing the
  relaxation; no additional product- or architecture-level approval is implied
  because no product guarantee or technical truth changes.
- Removing or redefining binding governance principles is treated as a major
  governance version step under the amendment rules that are currently in
  effect; the retained amendment section makes that visible in the file header.
- The currently effective governance text (constitution 1.1.0, the two skills,
  and the completion overlay) is the baseline being simplified; the exact wording
  of the simplified constitution is produced during planning/implementation as
  long as it satisfies FR-001 … FR-027.
- `docs/migrations/adopt-spec-kit.md` and
  `docs/migrations/spec-kit-ownership-audit.md` are historical migration
  records and are preserved as-is; this change's own spec/plan/tasks record the
  simplification going forward.
- Where a *current* document (for example the traceability map) cites a deleted
  process skill, only the citation is corrected; the coverage semantics of that
  document are unchanged.
- Stock Spec Kit commands referenced in the desired future workflow remain
  available and unmodified in this repository's Spec Kit installation.
- "Practically testable" for regression protection is judged inside the change
  by the contributor, and the honesty rule in FR-008 is what prevents that
  judgment from becoming an unearned coverage claim.
