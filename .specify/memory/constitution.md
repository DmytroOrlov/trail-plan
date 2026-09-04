<!--
Sync Impact Report
- Version change: 1.1.0 -> 2.0.0 (MAJOR)
- Amendment rationale: explicit, user-requested withdrawal of the
  repository-specific process machinery adopted during the first Spec Kit
  change (recorded in specs/002-governance-simplification/). The two project
  process skills and the repository-owned workflow completion extensions are
  withdrawn; only their durable rules survive, restated directly and concisely
  in this constitution. No product guarantee, architecture truth, decision
  status, or planner behavior changed, and historical records remain intact.
- Changed principles:
  - Former Principle I (canonical authority) and the source-precedence and
    persistence sections are condensed into the Core durable rules.
  - Former Principles II-III restated product and architecture semantics; the
    semantics are removed and only the no-silent-weakening governance rule is
    retained.
  - Former Principles IV-V mandated skill handoffs, evidence packages, and a
    separate blocked-verification stage; the durable residue is now the Working
    rules, and the process machinery is withdrawn.
  - The lifecycle's required trailing review and verification stages are
    replaced by stock-command Completion.
- Follow-up TODOs: none.
-->

# Trail Plan Constitution

This constitution is the repository's mandatory governance entry point. It
states only durable, project-specific rules: how canonical truth is owned, how
it may change, and how work is synchronized and completed. It does not own or
restate product or architecture semantics.

## Core durable rules

### Canonical ownership and source precedence

Each kind of knowledge has exactly one canonical owner:

- `docs/PRODUCT_CONTRACT.md` owns current implementation-independent product
  guarantees (`PC-*`).
- `docs/ARCHITECTURE.md` owns the current architecture and representation
  boundaries.
- `docs/CURRENT_STATE.md` owns established, rejected, and open decision status.
- `docs/TEST_MATRIX.md` owns executable regression traceability, coverage
  classification, and the definitions of the coverage classes.
- `docs/adr/` owns durable historical decision rationale.
- `trail-plan.scala` is the only current production implementation and the
  default-running regression suite.
- `specs/<change>/` owns the record of one active Spec Kit change.

A normative fact MUST have one textual owner. Other artifacts MAY reference
that fact but MUST NOT silently redefine it. Where any artifact summarizes or
cites a canonical owner, the owner's current text governs; a divergence is a
defect to fix in the summarizing artifact and is never a basis for changing
behavior.

Substantive work MUST read the relevant canonical owners before specifying,
planning, implementing, or converging.

When information conflicts, precedence is:

1. explicit current user instruction;
2. this constitution;
3. the relevant current canonical owner;
4. current `trail-plan.scala` code and default-running regression evidence;
5. active change artifacts that do not explicitly amend a higher owner;
6. ADR and evidence history for rationale;
7. historical snapshots and prior conversation context.

Historical Scala snapshots, archived change artifacts, temporary diagnostics,
and prior chat context are NOT current authority; they may explain how the
repository once operated but MUST NOT override the canonical owners.

### No silent weakening

Established product, safety, exactness, representation, and fail-closed
behavior MUST NOT be silently weakened, approximated, or replaced because of
implementation convenience.

An intentional change to such behavior MUST be explicit in the active change,
naming the affected truth, and MUST be human-approved before implementation and
synchronized to its canonical owner in the same completed change.

A plan that conflicts with established architecture or a rejected direction
MUST identify the conflict explicitly and MUST NOT resolve it by quietly
weakening a contract.

### Change versus current truth

Active `specs/<change>/` artifacts describe proposed changes; the canonical
documents describe current truth. A proposal is not current truth until it is
approved, implemented, verified, and synchronized back to its owner. A
completed change record remains a historical record and MUST NOT become a
competing current source of truth.

## Working rules

### Same-change owner update

When a change modifies durable product or architecture truth, the same change
MUST update the affected canonical owner.

### Same-change traceability update

When a change modifies normative contract or architecture clauses, or their
regression coverage, the same change MUST update `docs/TEST_MATRIX.md`.

### Regression protection when practically testable

A newly established normative invariant MUST carry deterministic executable
regression protection in the same change when it is practically testable.
Testability is judged by the contributor inside the change. When protection is
not practically testable, the change MUST report the gap honestly in its own
record instead of deferring it to a separate verification stage.

### Coverage honesty

A `DIRECT` coverage claim is permitted only when the default-running suite
would fail after the protected property regressed. Claiming `DIRECT` for a
suite that would stay green is dishonest and the claim MUST be downgraded.
Coverage-class definitions are owned by `docs/TEST_MATRIX.md` and are not
restated here.

## Completion

For a substantive change, completion is the contributor's direct use of the
stock Spec Kit commands:

```text
/speckit.specify
-> /speckit.clarify (when needed)
-> /speckit.plan
-> /speckit.checklist (when useful)
-> /speckit.tasks
-> /speckit.analyze
-> /speckit.implement
-> /speckit.converge
```

followed by ordinary review, commit, and pull-request work as appropriate.
These are contributor-invoked stock commands; no repository-owned workflow
extension or project skill is part of the required path, and bundled/base Spec
Kit material MUST NOT be modified.

Completion additionally requires that implementation, tests, canonical owners,
and traceability are mutually consistent and that convergence reports no
remaining gap.

Small mechanical or documentation-only changes MAY use a shorter stock path
when they do not alter product semantics, architecture, safety, executable
traceability, or established/rejected decision state.

Independent review is permitted and encouraged case-by-case for risky changes;
it is never a mandatory stage. Spec Kit checklists remain available whenever a
contributor finds them useful.

## Amendment

Amendments to this constitution require an explicit rationale, human approval,
and a version record, with review of any affected governance files.

Versioning follows SemVer for governance:

- MAJOR: removes or redefines a binding principle or changes authority or
  ownership incompatibly;
- MINOR: adds a principle or materially expands governance obligations;
- PATCH: clarification with no semantic change.

Version: 2.0.0 | Ratified: 2026-09-02 | Last Amended: 2026-09-04
