# Contract: Preservation and Non-Scope

**Change**: `002-governance-simplification` · **Plan**: [../plan.md](../plan.md) ·
**Research basis**: D1, D6, D7, D9

Scope: what MUST remain byte-identical (or semantically unchanged) while the
governance simplification is applied. Each clause states the invariant and the
mechanical check that proves it; checks are collected in
[../quickstart.md](../quickstart.md).

## Historical preservation

| ID | Clause | Spec source | Check |
|---|---|---|---|
| `GOV-P-01` | `specs/001-truth-review-fixes/**` is unchanged: 0 files added, changed, or deleted. | FR-023, SC-006 | path-scoped `git diff --name-status` |
| `GOV-P-02` | `specs/001-truth-review-fixes/promotion/` remains present and unmodified. | FR-023, US-3 AC-1 | path existence + diff |
| `GOV-P-03` | `specs/001-truth-review-fixes/checklists/release-gate.md` remains present and unmodified. | FR-023, US-3 AC-1 | path existence + diff |
| `GOV-P-04` | `docs/migrations/adopt-spec-kit.md` and `docs/migrations/spec-kit-ownership-audit.md` are not rewritten to match the future process. | FR-023 | path-scoped diff |
| `GOV-P-05` | Historical mentions of the deleted skills, overlay, gates, or promotion process are left intact (history is not sanitized). | D6, FR-023 | term search is allowlisted to exclude historical paths |

## Product and planner preservation

| ID | Clause | Spec source | Check |
|---|---|---|---|
| `GOV-P-06` | `trail-plan.scala` has zero diff. | FR-025 | path-scoped diff |
| `GOV-P-07` | The change records no modification to planner product semantics. | FR-024, SC-005 | diff scope + completion report |
| `GOV-P-08` | The default regression suite produces the same pass/fail result before and after the change (same `SELF-TESTS` outcome, no new failures). | SC-005 | run suite; compare result |
| `GOV-P-09` | `docs/PRODUCT_CONTRACT.md`, `docs/ARCHITECTURE.md`, `docs/CURRENT_STATE.md`, and `docs/adr/**` are unchanged. | FR-024 | path-scoped diff |
| `GOV-P-10` | No coverage classification, `PC-*` mapping, architecture mapping, or test name changes in `docs/TEST_MATRIX.md`; only the §Coverage gaps pointer moves. | FR-022, `GOV-X-20` | matrix diff |

## Structure and non-addition

| ID | Clause | Spec source | Check |
|---|---|---|---|
| `GOV-P-11` | The useful project-specific structure is retained: `docs/PRODUCT_CONTRACT.md`, `docs/ARCHITECTURE.md`, `docs/CURRENT_STATE.md`, `docs/TEST_MATRIX.md`, `docs/adr/`, `docs/DEVELOPMENT.md`, thin `AGENTS.md`, `.specify/memory/constitution.md`. | FR-020 | path existence |
| `GOV-P-12` | No new governance artifact, skill, gate, or workflow abstraction is added; governance entry points after the change are a subset of before. | FR-026, SC-007 | added-path check outside the change record |
| `GOV-P-13` | Bundled/base Spec Kit material (`.specify/templates/**`, `.specify/scripts/**`, `.opencode/commands/**`, the `speckit` base workflow) has zero diff. | FR-014 | path-scoped diff |

## Amendment discipline (in force for this change)

| ID | Clause | Spec source |
|---|---|---|
| `GOV-P-14` | This change is itself governed by constitution 1.1.0 and is adopted via its amendment procedure: explicit rationale, human approval (the user's feature input), a MAJOR version bump, and review of affected governance files. | FR-027, D2 |
| `GOV-P-15` | Relaxing process machinery is explicit, human-requested, and recorded — it is not a silent weakening of product/safety/exactness/representation/fail-closed behavior, which `GOV-R-02` continues to protect. | Edge case |
