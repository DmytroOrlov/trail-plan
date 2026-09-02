<!--
Sync Impact Report
- Version change: 1.0.0 -> 1.1.0
- Amendment rationale: remove restated product/architecture semantics from
  Principle III so this constitution governs how canonical truth may change
  instead of independently owning what that truth is (per
  docs/migrations/spec-kit-ownership-audit.md); align governance with the
  actual adoption state (constitution and thin AGENTS.md landed together in
  the ratified initial migration).
- Changed principles:
  - III. Exactness, Safety, and Architectural Boundaries -> now a
    no-silent-weakening governance rule pointing at the Principle I owners.
- Follow-up TODOs: none.
  - Later reassessment is owned by docs/migrations/spec-kit-ownership-audit.md
    (post-trial re-audit), not by this file.
-->

# Trail Plan Constitution

## Core Principles

### I. Canonical Repository Authority

Repository truth MUST be taken from the canonical owner for each kind of knowledge.

- `docs/PRODUCT_CONTRACT.md` owns current implementation-independent product guarantees.
- `docs/ARCHITECTURE.md` owns the current implementation architecture and representation boundaries.
- `docs/CURRENT_STATE.md` owns established, rejected, and open decision status.
- `docs/TEST_MATRIX.md` owns executable regression traceability and coverage classification.
- `docs/adr/` owns durable historical decision rationale.
- `trail-plan.scala` is the only canonical current production implementation and
  executable regression source.
- Historical Scala snapshots, prior chat context, temporary diagnostics, and
  archived Spec Kit feature artifacts MUST NOT override current canonical owners.

All substantive Spec Kit work MUST read the relevant canonical owners before
creating or changing `spec.md`, `plan.md`, `tasks.md`, implementation, or
convergence findings.

A normative fact MUST have one canonical textual owner. Other artifacts MAY
reference that fact but MUST NOT silently redefine it.

### II. Preserve Product Semantics and Production Shape

The current product contract MUST be preserved unless an approved change
explicitly changes it.

The production deliverable is the single Scala script `trail-plan.scala`.
Auxiliary analysis, diagnostics, shell commands, temporary scripts, or other
artifacts MAY be used to investigate a problem, but they MUST NOT substitute for
a requested production change in `trail-plan.scala`.

The established single-file production shape MUST be preserved unless the user
explicitly approves changing that architectural constraint.

A Spec Kit feature that intentionally changes an existing product guarantee MUST:

1. identify the affected `PC-*` contract or state explicitly that a genuinely
   new product guarantee is being introduced;
2. make the semantic change explicit in the feature specification;
3. obtain human approval before implementation; and
4. update the canonical product owner in the same completed change.

Until those conditions are satisfied, the existing product contract remains
authoritative.

### III. Canonical Contracts Must Not Be Silently Weakened

Changes MUST preserve established correctness and safety semantics.

The concrete product, architecture, and decision semantics are owned by the
canonical owners named in Principle I — `docs/PRODUCT_CONTRACT.md`,
`docs/ARCHITECTURE.md`, and `docs/CURRENT_STATE.md` — not by this
constitution. This principle therefore states only the governance rule:

- an existing canonical contract MUST NOT be silently weakened, approximated,
  or replaced because of implementation convenience;
- an intentional change to a product guarantee MUST follow the procedure in
  Principle II; an intentional change to established architecture or
  decision state MUST be explicit in the active change spec, human-approved
  before implementation, and synchronized back to its canonical owner in the
  same completed change;
- where any governance artifact summarizes or cites a canonical owner, the
  owner's current text governs; a divergence is a defect to fix in the
  summarizing artifact and is never a basis for changing behavior.

A plan that conflicts with an established architecture or rejected direction
MUST identify the conflict explicitly. It MUST NOT resolve the conflict by
quietly weakening a contract or by treating a cleaner implementation as evidence
that the current decision is wrong.

### IV. Evidence Before Promotion

New durable product or architecture semantics MUST NOT be invented from
implementation convenience, one successful route, a test name, a comment, or an
LLM inference about author intent.

A new permanent invariant requires an established basis such as:

- an explicit current user/product requirement;
- a reproducible counterexample;
- a deterministic regression reproducing the defect;
- a code/data contract that necessarily establishes the behavior; or
- independently reviewed production evidence.

Investigation-only findings MUST remain investigation evidence until established.

When work establishes new durable product/architecture knowledge or rejects an
established direction, the repository MUST use
`.agents/skills/invariant-promotion/SKILL.md` before traceability synchronization.

Temporary diagnostics and experimental mechanisms MUST be removed after their
evidence has been adjudicated unless they are explicitly promoted to production
behavior through the normal product/architecture process.

### V. Executable Regression and Traceability

Every newly established permanent product or architecture invariant MUST receive
deterministic regression protection in the same completed change when it can be
expressed without coupling to incidental run counts or one-off observations.

If such protection is structurally impossible, the change MUST report the gap as
`BLOCKED` rather than presenting normal completion.

`docs/TEST_MATRIX.md` MUST remain a traceability map, not a source of product or
architecture semantics.

Whenever executable test evidence, `PC-*` requirements, tested architecture
invariants, or matrix mappings materially change,
`.agents/skills/test-traceability-sync/SKILL.md` MUST be applied in the same
change.

A successful canonical route, compilation, or a checked Spec Kit requirements
checklist is not by itself proof of correctness or regression coverage.

## Repository Authority and Change Semantics

### Source precedence

When information conflicts, use this order:

1. explicit current user instruction;
2. this constitution;
3. the relevant current canonical repository owner named in Principle I;
4. current canonical `trail-plan.scala` code and default-running regression evidence;
5. active Spec Kit change artifacts that do not explicitly amend a higher owner;
6. ADR/evidence history for rationale;
7. historical snapshots and prior conversation context.

An active Spec Kit specification MAY intentionally propose changing a canonical
owner, but the proposal MUST name the affected truth and is not current product
truth until approved, implemented, verified, and synchronized back to that owner.

### Spec persistence model

Trail Plan uses Spec Kit feature directories as **flow-forward change records**.

After a change is complete:

- current product truth remains in `docs/PRODUCT_CONTRACT.md`;
- current architecture truth remains in `docs/ARCHITECTURE.md`;
- current established/rejected/open status remains in `docs/CURRENT_STATE.md`;
- executable protection remains mapped in `docs/TEST_MATRIX.md`;
- durable rationale remains in ADRs;
- completed `specs/<change>/` artifacts remain historical records of the change
  and MUST NOT become a competing current source of truth.

### Scope discipline

A change MUST solve the approved specification and MUST NOT silently broaden or
narrow the problem.

If investigation shows that the approved specification itself must change, the
specification MUST be amended and reviewed before implementation continues.

Performance work MUST preserve the problem being solved. A route MUST NOT be made
to succeed by weakening an established product or safety contract.

Prefer deleting a disproven heuristic over compensating for it with another
heuristic.

## Spec Kit Change Lifecycle

For substantive planner changes, the default workflow is:

```text
specify
-> clarify
-> plan
-> checklist
-> tasks
-> analyze
-> implement
-> converge
-> independent review
-> executable regression / traceability verification
```

Before planning or implementation, the active change MUST identify the relevant:

- `PC-*` requirements from `docs/PRODUCT_CONTRACT.md`;
- architecture sections from `docs/ARCHITECTURE.md`;
- established/rejected/open decisions from `docs/CURRENT_STATE.md`;
- ADRs containing durable rationale;
- existing regressions and `docs/TEST_MATRIX.md` coverage.

`spec.md` owns the intended WHAT/WHY of the active change.

`plan.md` owns the implementation approach for that change and MUST include a
Constitution Check against this file.

`research.md` MAY contain hypotheses, experiments, and change-local evidence but
does not automatically create current product or architecture truth.

`tasks.md` owns implementation work decomposition.

`/speckit.analyze` findings MUST be resolved at the artifact that owns the
problem; a constitutional conflict MUST NOT be fixed by diluting the
constitution.

`/speckit.converge` verifies completeness against the active change artifacts,
but normal project Done criteria still require relevant tests, traceability, and
independent review.

Small documentation-only or mechanical changes MAY use a shorter workflow when
they do not alter product semantics, architecture, safety, executable
traceability, or established/rejected decision state.

## Governance

This constitution is the mandatory entry point for Spec Kit governance. It
defines how Spec Kit must interact with the repository's existing canonical
owners; it does not replace those owners.

Amendments require:

1. an explicit rationale;
2. human approval;
3. a constitution version bump;
4. review of any affected repository governance files or Spec Kit artifacts.

Versioning follows SemVer for governance:

- MAJOR: removes or redefines a binding principle or changes authority/ownership
  incompatibly;
- MINOR: adds a principle or materially expands governance obligations;
- PATCH: clarification with no semantic change.

Every substantive feature plan and review MUST verify compliance with this
constitution. Conflicts with a `MUST` are blocking until resolved explicitly.

Version: 1.1.0 | Ratified: 2026-09-02 | Last Amended: 2026-09-02
