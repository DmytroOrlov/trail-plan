# Data Model: Governance Simplification — Back to Stock Spec Kit

Phase 1 output for [plan.md](./plan.md). This change has no runtime entities;
the "data" is the governance artifact graph — which artifact exists, what it
owns, which pointers it publishes, and what state transitions this change
performs on it. Semantics of product/architecture owners are referenced, never
restated (constitution 1.1.0 Principle I, retained by FR-002).

## Entities

### Constitution

| Attribute | Type | Notes |
|---|---|---|
| `path` | fixed | `.specify/memory/constitution.md` — sole governance entry point (FR-020) |
| `version` | SemVer string | `1.1.0` → `2.0.0` (MAJOR; D2) |
| `ratified` / `last_amended` | dates | `2026-09-02` / set to this change's completion date |
| `sync_impact_report` | leading HTML comment | version change, amendment rationale, changed principles, follow-up status (retained mechanism) |
| `owned_rules` | rule set | ownership/precedence, no-silent-weakening, change-vs-current-truth separation, same-change owner update, same-change `TEST_MATRIX` update, regression-when-practically-testable, DIRECT honesty, stock-command completion, amendment procedure (FR-001…FR-010) |
| `forbidden_content` | negative rule set | product/architecture semantics; any obligation only satisfiable by deleted skills/overlay/gates/packages (FR-019); restated coverage-class definitions (owned by `docs/TEST_MATRIX.md`) |
| `size` | qualitative | materially simpler than the current version; carries only the durable rules retained by FR-002 through FR-010; no withdrawn machinery remains as a live obligation (FR-001, SC-002; D5) |

Validation rules: every MUST in the text is satisfiable by stock commands,
retained files, or contributor judgment; no clause names
`invariant-promotion`, `test-traceability-sync`, `.agents/skills/**`,
`trail-plan-speckit-completion`, a mandatory independent-review stage, separate
verification stages, a required `promotion/` package, or a mandatory release
gate.

### Repository Router (`AGENTS.md`)

| Attribute | Type | Notes |
|---|---|---|
| `role` | enum | `navigation-only`; owns zero governance rules (FR-021) |
| `routes` | ordered list | constitution → active `specs/<change>/` → canonical owners named by the constitution → `docs/DEVELOPMENT.md` |
| `removed_routes` | list | `Skill routing` section naming the two project skills |
| `history_note` | text | retains "historical snapshots / archived artifacts / prior chat are not current authority" as navigation, pointing at the constitution for the rule |

Validation rules: remains a thin navigation-only router; no MUST/SHALL
obligation statements; zero live
references to deleted machinery.

### Canonical Owners (docs set + implementation)

| Artifact | Owns | Change in this change |
|---|---|---|
| `docs/PRODUCT_CONTRACT.md` | implementation-independent product guarantees (`PC-*`) | none (FR-024); `:11` governance pointer already correct |
| `docs/ARCHITECTURE.md` | current architecture and representation boundaries | none |
| `docs/CURRENT_STATE.md` | established/rejected/open decision status | none; `:57` governance pointer already correct |
| `docs/TEST_MATRIX.md` | traceability map **and** coverage-class definitions | §Coverage gaps pointer → constitution; zero classification/mapping change (FR-022, D8) |
| `docs/DEVELOPMENT.md` | build/run/test tooling | none |
| `docs/adr/**` | durable decision rationale | none |
| `trail-plan.scala` | production implementation + default regression suite | none (FR-025; expected diff 0 lines) |

Relationships: each is referenced-by the constitution and the router; each may
reference another owner but must not redefine its facts (one-normative-fact,
one-owner).

### Project Skills (deleted)

| Attribute | Before | After |
|---|---|---|
| `.agents/skills/invariant-promotion/SKILL.md` | present, 621 lines; referenced by constitution IV, `AGENTS.md`, `docs/TEST_MATRIX.md:113`, overlay gate text | **deleted** (FR-011); durable residue lives in the constitution |
| `.agents/skills/test-traceability-sync/SKILL.md` | present, 345 lines; referenced by constitution V, `AGENTS.md`, overlay gate text | **deleted** (FR-012); replaced by the same-change rule (FR-006) |
| `.agents/skills/` directory | contains the two skills | contains no Trail Plan-owned process machinery (SC-004) |

### Workflow Overlay (withdrawn)

| Attribute | Before | After |
|---|---|---|
| `overlay_id` | `trail-plan-speckit-completion`, `extends: speckit`, `priority: 10` | unregistered |
| `registration` | `specify workflow overlay list speckit` → `… (priority=10, source=project:trail-plan-speckit-completion, enabled)` | no overlays listed |
| `layer attribution` | `specify workflow resolve speckit` → `[project-overlay]` above `[base]`; contributes `converge`, `independent-review`, `executable-regression-verification`, `traceability-verification` | single `[base]` layer; base steps `specify`, `review-spec`, `plan`, `review-plan`, `tasks`, `implement` unchanged |
| repository-owned files | `.specify/workflows/overlays/speckit.yml` (hand-authored source) **and** `.specify/workflows/overlays/speckit/trail-plan-speckit-completion.yml` (CLI-normalized registered copy) | both deleted (FR-013) |
| CLI-managed registry | `.specify/workflows/workflow-registry.json` (tracked) | retained CLI-managed state; **conditionally MODIFIED** by the supported `specify workflow overlay remove` run only if the registry state requires it (a diff, if produced, is a permitted narrowly scoped CLI-managed side effect, not scope drift); names the bundled `speckit` entry only and no project overlay; unchanged in this actual implementation because the pre-change tracked content already represented bundled-only registry state |
| base material | `.specify/workflows/speckit/workflow.yml`, `.specify/templates/**`, `.specify/scripts/**`, `.opencode/commands/**` | untouched (FR-014) |

### Active Change Record

`specs/002-governance-simplification/` — flow-forward record for this change:
`spec.md`, `checklists/requirements.md`, `plan.md`, `research.md`,
`data-model.md`, `contracts/*`, `quickstart.md`, later `tasks.md`. It describes
a proposed change and never becomes current truth (retained separation rule).
This directory may name deleted machinery because its purpose is recording the
withdrawal.

### Historical Change Record

`specs/001-truth-review-fixes/**` (12 files, including `promotion/connector-admissibility.md`
and `checklists/release-gate.md`) and `docs/migrations/**` (`adopt-spec-kit.md`,
`spec-kit-ownership-audit.md`). Immutable under this change: 0 added, 0 changed,
0 deleted (FR-023, SC-006).

## Relationships (pointer graph after the change)

```text
AGENTS.md ──routes──▶ .specify/memory/constitution.md
                          │ owns rules: ownership/precedence, no-silent-weakening,
                          │ change-vs-truth, same-change updates, honesty, completion, amendment
                          ├──points-to──▶ docs/PRODUCT_CONTRACT.md   (PC-* semantics)
                          ├──points-to──▶ docs/ARCHITECTURE.md        (architecture semantics)
                          ├──points-to──▶ docs/CURRENT_STATE.md       (decision status)
                          ├──points-to──▶ docs/TEST_MATRIX.md         (traceability + coverage-class definitions)
                          ├──points-to──▶ docs/adr/**                 (durable rationale)
                          └──points-to──▶ trail-plan.scala            (production + regression)
docs/TEST_MATRIX.md:18,160 ──governance pointer──▶ constitution   (unchanged)
docs/TEST_MATRIX.md:113 ──────governance pointer──▶ constitution  (was: deleted skill path)
docs/CURRENT_STATE.md:57 ─────governance pointer──▶ constitution  (unchanged)
docs/PRODUCT_CONTRACT.md:11 ──governance pointer──▶ constitution  (unchanged)
AGENTS.md ──────────dev tooling pointer──▶ docs/DEVELOPMENT.md
.stock /speckit.* commands ───invoke against──▶ specs/<change>/ artifacts
.specify/workflows/overlays/** ──deleted──▶ (no repository-owned overlay remains)
.specify/workflows/workflow-registry.json ──CLI-managed, unchanged here; CLI updates it only if required──▶ (bundled speckit entry only)
```

Invariant: every pointer published by a current artifact resolves to an existing
file, and no current artifact publishes a pointer to a skill, overlay, gate, or
`promotion/` requirement (FR-019, FR-022, SC-003).

## State transitions

| Artifact | From | To | Trigger |
|---|---|---|---|
| `.specify/memory/constitution.md` | 1.1.0, skill/stage obligations | 2.0.0, materially simpler, durable rules only | amendment with human-approved rationale (D2) |
| `AGENTS.md` | router + `## Skill routing` section | thin router only | FR-021 |
| `docs/TEST_MATRIX.md` §Coverage gaps | cites `../.agents/skills/invariant-promotion/SKILL.md` | cites `../.specify/memory/constitution.md` (the file's existing relative-path convention) | FR-022 mechanical fix |
| `.agents/skills/{invariant-promotion,test-traceability-sync}/` | present | removed | FR-011, FR-012 |
| overlay `trail-plan-speckit-completion` | registered + enabled + 2 files | unregistered via `specify workflow overlay remove` (which may update the tracked CLI-managed `.specify/workflows/workflow-registry.json` if the registry state requires it; unchanged here — the pre-change registry already represented bundled-only state) + files deleted | FR-013 (D3) |
| `specify workflow resolve speckit` | project-overlay above base | base only | verification of the above |
| `specs/001-truth-review-fixes/**`, `docs/migrations/**` | historical | historical (unchanged bytes) | FR-023 guard |
| `trail-plan.scala` + default suite result | pass | identical pass result | SC-005 non-change control |
| Contributor workflow | stock commands + mandatory converge/review/verification stages + release gate + promotion package | stock `/speckit.*` sequence through `/speckit.converge` + ordinary review/PR; same-change updates as ordinary work | FR-009, FR-015…FR-018, SC-001 |
| Governance applicability | rules of 1.1.0 | rules of 2.0.0 for future work only | FR-027 (effective on completion of this change) |

## Deletion set (exact)

```text
.agents/skills/invariant-promotion/SKILL.md
.agents/skills/test-traceability-sync/SKILL.md
.specify/workflows/overlays/speckit.yml
.specify/workflows/overlays/speckit/trail-plan-speckit-completion.yml
```

plus the CLI-side unregistration of `trail-plan-speckit-completion` from the
`speckit` workflow. Nothing else is deleted.

## Modification set (exact)

REQUIRED modifications:

```text
.specify/memory/constitution.md            # rewrite to 2.0.0 (rules only; no new owners)
AGENTS.md                                  # remove skill routing
docs/TEST_MATRIX.md                        # §Coverage gaps pointer only
specs/002-governance-simplification/**     # this change's own record
```

Narrowly PERMITTED conditional modification (CLI-managed, only if the registry
state requires it; absence of a diff here is valid, and in this actual
implementation the pre-change tracked content already represented bundled-only
registry state, so `workflow-registry.json` has no diff):

```text
.specify/workflows/workflow-registry.json  # CLI-managed; modified only if the supported overlay removal requires it (FR-013)
```

Any file outside this set appearing in the change diff is a scope defect
(FR-023…FR-025, SC-006, SC-007). The only permitted `.specify/workflows/**`
modification is the CLI-managed `.specify/workflows/workflow-registry.json`
update, and only if the supported overlay removal (FR-013) actually produces
it — a registry diff is permitted when CLI-produced but never required. No
other `.specify/workflows/**` path may be
modified, and `.specify/workflows/speckit/workflow.yml` stays untouched (FR-014).
