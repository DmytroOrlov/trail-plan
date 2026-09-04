# Implementation Plan: Governance Simplification — Back to Stock Spec Kit

**Branch**: `002-governance-simplification` | **Date**: 2026-09-04 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/002-governance-simplification/spec.md`

## Summary

Collapse Trail Plan's repository-specific Spec Kit governance back toward the
stock workflow: rewrite `.specify/memory/constitution.md` as a short, durable
rule set (canonical ownership/precedence, no-silent-weakening, change-vs-truth
separation, same-change owner + traceability updates, regression-honesty,
stock-command completion, amendment procedure), delete the two project process
skills, unregister and delete the repository-owned Spec Kit completion overlay,
re-point the two remaining live pointers (`AGENTS.md`,
`docs/TEST_MATRIX.md` §Coverage gaps), and leave every product/architecture
artifact, the planner, and all historical records byte-identical.

## Technical Context

**Language/Version**: Markdown + YAML governance artifacts; no application code
change. Spec Kit CLI `specify 1.0.3` (bundled `speckit` workflow v1.0.0,
integration `opencode`, `invoke_separator: "."`).

**Primary Dependencies**: Stock `/speckit.*` commands in `.opencode/commands/`
(specify, clarify, plan, checklist, tasks, analyze, implement, converge);
`specify workflow overlay remove|list|disable` and `specify workflow resolve`
for overlay state.

**Storage**: Files only — `.specify/memory/constitution.md`,
`.specify/workflows/overlays/**`, the CLI-managed
`.specify/workflows/workflow-registry.json`, `AGENTS.md`, `docs/*.md`,
`.agents/skills/**`. No planner data, fixtures, or runtime required.

**Testing**: Mechanical verification of governance state (grep for live
references, `specify workflow resolve speckit`, path-scoped `git diff`) plus the
existing default regression suite invoked through `./trail-plan.scala`
(`SELF-TESTS: …` line) as the behavioral non-change control. No new test
harness is added (FR-026).

**Target Platform**: Repository working tree, macOS/Linux shell; contributors
are humans and coding agents using the stock Spec Kit command set.

**Project Type**: Governance/process-only documentation change (single-owner
Markdown artifacts plus Spec Kit workflow configuration); no compiled component.

**Performance Goals**: N/A — no runtime path changes. Review is a qualitative
diff read: the constitution is materially simpler than the current version and
states only the durable rules retained by FR-002 through FR-010 (FR-001,
SC-002); no numeric line-count budget applies.

**Constraints**: Must not modify bundled/base Spec Kit material
(`.specify/templates/**`, `.specify/workflows/speckit/workflow.yml`,
`.opencode/commands/**`) (FR-014); must not modify `trail-plan.scala` or any
planner/product/architecture semantics (FR-024, FR-025); must not touch
`specs/001-truth-review-fixes/**` or `docs/migrations/**` (FR-023, SC-006); must
not introduce replacement machinery (FR-026); must proceed under the currently
effective constitution 1.1.0 and its amendment procedure (FR-027); overlay
removal must go through the supported CLI **and** delete repository-owned
overlay files (FR-013).

**Scale/Scope**: 1 rewritten governance document, 1 reduced router, 1
mechanical pointer fix, 2 deleted skills, 2 deleted overlay files plus 1 CLI
unregistration (which may modify the tracked CLI-managed
`.specify/workflows/workflow-registry.json` if the registry state requires it —
a conditional, permitted side effect, not a required one; unchanged here),
1 self-record change
directory. 8 success criteria, all mechanically checkable.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Baseline authority: `.specify/memory/constitution.md` **1.1.0** (FR-027 — this
change is governed by the rules it is amending, and the simplified text becomes
governance only on completion).

| Gate | Requirement in constitution 1.1.0 | Evaluation |
|---|---|---|
| I. Canonical Repository Authority | Substantive work must read the relevant canonical owners first; one owner per normative fact | **PASS** — spec, constitution, router, both skills, overlay, docs set, and specs/001 were read before planning (see research.md §D1). Governance facts stay owned by the constitution; no product/architecture fact is restated into it. |
| II. Preserve Product Semantics and Production Shape | Product contract preserved; `trail-plan.scala` is the production deliverable and single-file shape preserved | **PASS** — FR-024/FR-025 and SC-005 make non-change explicit. No `PC-*` is edited and no Scala edit is planned (expected diff: 0 lines). |
| III. Canonical Contracts Must Not Be Silently Weakened | Canonical contracts must not be weakened for convenience; intentional change must be explicit + human-approved | **PASS via amendment** — the withdrawn obligations are *process* machinery, not product/architecture/decision semantics; the relaxation is explicit, user-requested in the feature input, and recorded as a constitution amendment rather than silent drift. |
| IV. Evidence Before Promotion | New durable semantics need an established basis; when durable knowledge is established the `invariant-promotion` skill must be used before traceability sync | **PASS via amendment** — the basis is an explicit current user instruction; the knowledge class is governance/change-process, whose owner is the constitution itself (recorded in the amendment header). The 1.1.0 skill-handoff clause is removed/redefined by this change, which is exactly the MAJOR case in §Governance versioning. |
| V. Executable Regression and Traceability | Newly established permanent invariants need deterministic regression in the same change; `test-traceability-sync` must be applied when mappings materially change; `BLOCKED` reporting | **PASS** — this change establishes no new product/architecture invariant (governance rules are a regression-inapplicable knowledge class: repository workflow rules), so the "when practically testable" scope is not met (FR-007); no matrix row, coverage classification, or mapping changes (only a diagnostic pointer is corrected, FR-022/SC-003), so the sync obligation is not triggered; the honesty rule itself is folded into the constitution (FR-008). The default suite must still produce the identical pass/fail result (SC-005). |
| Spec Kit Change Lifecycle | 1.1.0 default workflow ends in converge → independent review → executable-regression/traceability verification; plan must include a Constitution Check | **PASS, amended** — this plan carries the Constitution Check and runs the stock command sequence through `/speckit.converge`. The trailing project-specific stages are withdrawn by this change (FR-009, FR-015, FR-016); independent review remains available case-by-case, and this change's own convergence report is the completeness check. |
| Scope Discipline | Change must solve the approved spec, no silent broadening/narrowing | **PASS** — scope is fixed by FR-020…FR-027 boundaries; no new artifact class is added and no governance entry point grows (SC-007). |
| Governance (amendment) | Explicit rationale, human approval, version bump, review of affected governance files | **PASS** — see research.md §D2: MAJOR bump 1.1.0 → 2.0.0 with a Sync Impact Report header; affected governance files enumerated in the deliverable set below. |

**Gate result: no violations requiring Complexity Tracking justification.**

Re-check after Phase 1 design: research.md resolves every Technical Context
unknown (no NEEDS CLARIFICATION left); data-model.md, contracts/, and
quickstart.md introduce no new governance entry point, no product/architecture
semantic edit, and no base-material edit. **Gates still PASS.**

## Project Structure

### Documentation (this feature)

```text
specs/002-governance-simplification/
├── spec.md                  # Existing feature specification (input)
├── checklists/requirements.md  # Existing stock Spec Kit quality checklist
├── plan.md                  # This file
├── research.md              # Phase 0: decisions on amendment, removal, routing, honesty rules
├── data-model.md            # Phase 1: governance artifacts, states, transitions, pointer graph
├── quickstart.md            # Phase 1: mechanical validation + non-change control
└── contracts/
    ├── constitution-rules.md      # Required clauses and forbidden live obligations
    ├── machinery-removal.md       # Skill/overlay/gate withdrawal + supported-CLI end state
    └── preservation-scope.md      # Historical, base-material, and planner non-change contract
```

`tasks.md` is produced later by `/speckit.tasks` and is not part of this plan.

### Governance layout (repository root)

```text
.specify/
├── memory/constitution.md                        # REWRITE: sole durable governance owner (2.0.0)
├── workflows/
│   ├── speckit/workflow.yml                      # BASE — untouched (FR-014)
│   ├── overlays/speckit.yml                      # DELETE: repository-owned overlay source
│   ├── overlays/speckit/trail-plan-speckit-completion.yml  # DELETE: registered overlay
│   └── workflow-registry.json                    # CLI-managed — updated by the supported overlay removal only if required; shows no project overlay (diff conditional, not required)
├── templates/**                                   # BASE — untouched
└── scripts/**                                     # BASE — untouched

.agents/
└── skills/
    ├── invariant-promotion/SKILL.md               # DELETE (FR-011)
    └── test-traceability-sync/SKILL.md            # DELETE (FR-012)

AGENTS.md                                          # EDIT: thin router, skill routing removed (FR-021)
docs/
├── PRODUCT_CONTRACT.md   # UNCHANGED (governance pointer already constitution-only)
├── ARCHITECTURE.md       # UNCHANGED
├── CURRENT_STATE.md      # UNCHANGED (governance pointer already constitution-only)
├── TEST_MATRIX.md        # EDIT: §Coverage gaps pointer → constitution (mechanical)
├── DEVELOPMENT.md        # UNCHANGED
├── adr/**                # UNCHANGED
└── migrations/**         # HISTORICAL — untouched (FR-023)

trail-plan.scala                                   # UNCHANGED (FR-025)
specs/001-truth-review-fixes/**                    # HISTORICAL — untouched (FR-023, SC-006)
```

**Structure Decision**: No new directories and no new artifact classes. The
deliverable is a strict subset of existing governance paths plus this change's
own record: one rewritten constitution, one reduced router, one mechanically
re-pointed matrix line, two deleted skills, two deleted overlay files with one
CLI unregistration. Every governance obligation left behind must be satisfiable
by stock commands and the retained docs set (FR-019).

## Complexity Tracking

> No constitution gate failed, so this section records nothing that needs
> justification.

| Violation | Why Needed | Simpler Alternative Rejected Because |
|---|---|---|
| None | — | — |
