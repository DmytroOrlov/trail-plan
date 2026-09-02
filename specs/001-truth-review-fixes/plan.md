# Implementation Plan: Current Truth Review Fixes

**Feature**: `001-truth-review-fixes`  
**Date**: 2026-09-03  
**Spec**: `spec.md`

## Summary

Add deterministic regressions first, make only authority-determined production
changes, promote the connector-dominance architecture boundary, then synchronize
traceability and verify the full default suite.

The former selector blocker is resolved: the user adopted a 1.0-second
`candHard` materiality rule, which is now canonical in `PC-SELECT-01`. The plan
proceeds on that authority instead of stopping at the gate.

## Technical Context

- Scala 3.3.7 single-file production/test source: `trail-plan.scala`.
- ujson 4.4.3 and Java 21.
- Spec Kit 1.0.3 with OpenCode integration.
- Canonical tests are the default-running in-file `ts.test(...)` suite.
- Workflow customization layer: `specify workflow overlay` (project-local), with
  the bundled `speckit` workflow resolved as base layer only.
- No remaining NEEDS CLARIFICATION items.

## Constitution Check

Evaluated against `.specify/memory/constitution.md` 1.1.0 after the selector
product decision landed.

### Principle I — Canonical Repository Authority

- Relevant canonical owners read: `docs/PRODUCT_CONTRACT.md`,
  `docs/ARCHITECTURE.md`, `docs/CURRENT_STATE.md`, `docs/TEST_MATRIX.md`,
  `docs/adr/0003..0005`, `trail-plan.scala`.
- `PC-SELECT-01` is the single owner of the selector guarantee and now carries
  the approved materiality rule. Design artifacts in this change reference it and
  do not restate it as new truth.
- **Pass.**

### Principle II — Preserve Product Semantics and Production Shape

- The selector semantics change followed the required procedure: affected
  contract `PC-SELECT-01` identified, change made explicit in `spec.md`
  (`Product Decision`), human approval obtained via the 2026-09-03 clarifications,
  and the canonical product owner updated with the same text.
- The production deliverable remains the single `trail-plan.scala` script.
- No other product guarantee is altered (`FR-009` fences unrelated semantics).
- **Pass.**

### Principle III — No Silent Weakening

- `PC-SELECT-01` is amended explicitly, not approximated for convenience.
- `FR-001` brings trace-shape handling into conformance with the existing
  `PC-SAFE-01` fail-closed requirement; canonical contract text is unchanged.
- The selector resolution is confined to post-search product selection and must
  not quantize search, eligibility, dominance, safety, or pre-selection state
  (`FR-003`), preserving `PC-SEARCH-02` exactness.
- Conflicts with ADR-0005 local-marginal-drop rationale are additive only (the
  rule operates on the frontier before local selection); no ADR is rewritten.
- The materiality stage's placement between the exact eligible frontier and the
  local marginal-drop selector is a durable architecture boundary owned by
  `docs/ARCHITECTURE.md` §Post-search selection; this change must synchronize
  that section in the same completed change via the invariant-promotion step
  (stage placement only; product semantics stay owned by `PC-SELECT-01`).
- **Pass.**

### Principle IV — Evidence Before Promotion

- The selector fix is grounded in a reproducible counterexample plus a
  deterministic pre-fix regression already recorded in the suite.
- The connector-dominance admissibility boundary is promoted from a
  reproducible continuation-sensitivity regression, not from implementation
  convenience, via the invariant-promotion skill before traceability sync.
- Temporary diagnostics (pre-fix selection dump) are removed after adjudication.
- **Pass.**

### Principle V — Executable Regression and Traceability

- Every new permanent invariant has a planned deterministic regression
  (materiality rule including the exact 1.0 s boundary, trace fail-closed,
  chunk/tail necessity, real audit rejection, connector continuation
  retention).
- `docs/TEST_MATRIX.md` synchronization runs through
  `.agents/skills/test-traceability-sync/SKILL.md` in this change.
- If deterministic regression protection for any newly established permanent
  invariant is structurally impossible, this change must report that gap as
  `BLOCKED` per constitution Principle V rather than present normal completion.
- **Pass.**

### Other gates

- Workflow correction uses the supported project-local overlay layer; bundled
  generated material is not edited as if repository-owned (`FR-007`).
- Full default suite must pass before normal completion (`FR-009`,
  Acceptance Scenario 7).

No constitutional violation is planned; no exception is requested.

## Implementation Order

1. Reproduce the exact selector counterexample; record old selection as evidence
   (`FR-002`; done: suite test with base vs. extended frontier).
2. Implement the `PC-SELECT-01` 1.0-second `candHard` materiality resolution
   between frontier formation and local marginal-drop selection, plus the
   deterministic boundary regression (exactly 1.0 s retained; sub-1.0 s
   coalesced) (`FR-003`, `AS2`).
3. Add trace-shape fail-closed parser regression and minimally remove the
   `/route`-shape substitution in `fetchTraceAttributes` (`FR-001`).
4. Strengthen transfer chunk/short-tail and real-audit regressions
   (`FR-004`, `FR-005`).
5. Add connector-dominance continuation regression and prepare the admissibility
   boundary promotion into `docs/ARCHITECTURE.md`, landing it canonically through
   the invariant-promotion step (`FR-006`, `FR-008`).
6. Add the project-local `speckit` workflow overlay extending the lifecycle
   through converge, independent review, and traceability verification; verify
   with `specify workflow resolve` layer attribution (`FR-007`).
7. Apply invariant promotion — classify and canonically land BOTH architecture
   concerns: (A) the selector materiality-stage placement into
   `docs/ARCHITECTURE.md` §Post-search selection (exact eligible frontier →
   materiality resolution → local marginal-drop selector; this is architecture
   synchronization of the already-approved `PC-SELECT-01` product decision,
   NOT a re-promotion of `PC-SELECT-01`) and (B) the evidence-backed connector
   admissibility boundary into §Connector graph — then test-traceability
   synchronization (`FR-008`).
8. Run the full default suite (`AS7`); add the canonical production comparison when
   Valhalla is available (baseline evidence, not a completion gate).
