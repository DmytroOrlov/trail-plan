# Contract: Machinery Removal and Routing End State

**Change**: `002-governance-simplification` · **Plan**: [../plan.md](../plan.md) ·
**Research basis**: D1, D3, D6, D8

Scope: the repository end state after the withdrawn process machinery — project
skills, repository-owned Spec Kit workflow overlay, mandatory lifecycle stages,
evidence-package and release-gate patterns — plus the router and pointer
corrections. Each clause is stated with the mechanical check that satisfies it;
checks are collected in [../quickstart.md](../quickstart.md).

## Skill withdrawal

| ID | Clause | Spec source | Check |
|---|---|---|---|
| `GOV-X-01` | `.agents/skills/invariant-promotion/SKILL.md` does not exist. | FR-011 | path absence |
| `GOV-X-02` | `.agents/skills/test-traceability-sync/SKILL.md` does not exist. | FR-012 | path absence |
| `GOV-X-03` | `.agents/skills/**` contains no Trail Plan-owned process machinery. | SC-004 | directory listing empty of `SKILL.md` |
| `GOV-X-04` | The durable rules those skills carried appear directly in the constitution (`GOV-R-04`…`GOV-R-07`), not through a separate procedural document. | FR-011, FR-012 | constitution clause presence |

## Overlay withdrawal

| ID | Clause | Spec source | Check |
|---|---|---|---|
| `GOV-X-05` | The project overlay is unregistered through the supported CLI: `specify workflow overlay remove speckit trail-plan-speckit-completion` has been run. | FR-013 | CLI state check |
| `GOV-X-06` | `specify workflow overlay list speckit` reports no overlays. | FR-013, SC-004 | CLI stdout |
| `GOV-X-07` | `specify workflow resolve speckit` shows a single `base` layer, and no step is attributed to `project:trail-plan-speckit-completion`. | SC-004 | CLI stdout |
| `GOV-X-08` | `.specify/workflows/overlays/speckit.yml` and `.specify/workflows/overlays/speckit/trail-plan-speckit-completion.yml` are both removed; file deletion alone never counts as satisfying `GOV-X-05`. | FR-013 | path absence |
| `GOV-X-09` | The bundled workflow remains installed and listed: `specify workflow list` still shows `Full SDD Cycle (speckit) v1.0.0` with `source: bundled`. | FR-014 | CLI stdout |
| `GOV-X-10` | Bundled/base Spec Kit material is unmodified: `.specify/workflows/speckit/workflow.yml`, `.specify/templates/**`, `.specify/scripts/**`, `.opencode/commands/**` have zero diff. | FR-014 | path-scoped diff |

## Lifecycle-stage withdrawal

| ID | Clause | Spec source | Check |
|---|---|---|---|
| `GOV-X-11` | No current governance or current-truth artifact requires a mandatory independent-review stage; permissive case-by-case wording is allowed. | FR-015 | text search with allowance review |
| `GOV-X-12` | No artifact requires separate mandatory executable-regression or traceability verification stages; that work is stated as ordinary in-change tasks/completion criteria. | FR-016 | text search |
| `GOV-X-13` | No artifact requires a `promotion/` evidence package for new changes. | FR-017 | text search (historical records excluded) |
| `GOV-X-14` | No artifact treats a custom release-gate checklist as mandatory; Spec Kit checklists remain optional. | FR-018 | text search |

## Live-reference invariance

| ID | Clause | Spec source | Check |
|---|---|---|---|
| `GOV-X-15` | Zero live **textual** references to any deleted artifact remain in the current governance / current-truth artifact set: `AGENTS.md`, `.specify/memory/constitution.md`, and `docs/**` (historical records excluded). SC-003 is established jointly: this clause covers the textual artifacts (quickstart V-2), and the absence of active project-overlay configuration is established by the workflow-state checks `GOV-X-05`…`GOV-X-10` (quickstart V-3) — supported CLI registration state, overlay-file absence, and resolved layers. No single text-search command proves the workflow-configuration half. | SC-003 (textual half, with `GOV-X-05`…`GOV-X-10`) | term search over current-textual-artifact allowlist |
| `GOV-X-16` | Every pointer published by a current artifact resolves to an existing path. | FR-022 | link/path existence check |

## Router and pointer corrections

| ID | Clause | Spec source | Check |
|---|---|---|---|
| `GOV-X-17` | `AGENTS.md` keeps routing to the constitution, the active `specs/<change>/` artifacts, the canonical owners, and `docs/DEVELOPMENT.md`, and drops skill routing. | FR-021 | text diff review |
| `GOV-X-18` | `AGENTS.md` continues to own no governance rule (no MUST/SHALL obligation statements of its own). | FR-021 | text review |
| `GOV-X-19` | `docs/TEST_MATRIX.md` §Coverage gaps cites `../.specify/memory/constitution.md` (the relative-path convention the file already uses for constitution citations) instead of the deleted skill, with the sentence's guidance meaning preserved. | FR-022, D8 | single-line diff |
| `GOV-X-20` | No coverage classification (`DIRECT`/`PARTIAL`/`INDIRECT`/`MISSING`/`BASELINE`), `PC-*` mapping, architecture mapping, or test name changes anywhere in `docs/TEST_MATRIX.md`. | FR-022 | matrix diff is pointer-only |

## Stock workflow availability

| ID | Clause | Spec source | Check |
|---|---|---|---|
| `GOV-X-21` | Every stock command named by the retained completion sequence exists in this installation: `speckit.specify`, `speckit.clarify`, `speckit.plan`, `speckit.checklist`, `speckit.tasks`, `speckit.analyze`, `speckit.implement`, `speckit.converge`. | FR-009, Assumptions | command file listing |
| `GOV-X-22` | Completion of the stock cycle does not depend on any repository-owned workflow step. | FR-009 | `GOV-X-07` |
