# Feature Specification: Current Truth Review Fixes

**Feature ID**: `001-truth-review-fixes`  
**Working Branch**: `master` (no branch change requested)  
**Created**: 2026-09-03  
**Status**: In progress — selector product decision resolved; implementation may continue

## Purpose

Resolve only the material current-code ↔ repository-truth findings accepted by
the user, without redesigning planner policy or weakening an existing contract.

## Clarifications

### Session 2026-09-03

- Q: When the `Full SDD Cycle` verification finds the registered workflow reachable
  but stopping at `implement`, what repository-owned correction is expected? → A:
  Add a project-local overlay workflow extending the cycle through the
  constitution's later stages; bundled material stays untouched.
- Q: Must the regression suite pin the exact materiality boundary (a frontier
  step of exactly 1.0 s is retained; just under 1.0 s coalesces), or is the
  counterexample in Acceptance Scenario 2 sufficient protection? → A: Require a
  deterministic boundary regression: exactly 1.0 s gain retained, sub-1.0 s gain
  coalesced.

## Requirements

- **FR-001**: A `/trace_attributes` response without its own valid usable shape
  MUST fail closed. Route geometry MUST NOT substitute for trace geometry. This
  is conforming enforcement of the existing `PC-SAFE-01` text: FR-001 does not
  amend `PC-SAFE-01`, and no Principle-II product-contract amendment is required
  for FR-001. This scope note applies to FR-001 only; other canonical landings
  in this change still follow their own promotion rules.
- **FR-002**: Reproduce the reported `PC-SELECT-01` frontier counterexample as a
  deterministic regression before changing selector production code.
- **FR-003**: Post-search product selection MUST apply a 1.0-second
  `candHard` materiality resolution after the exact eligible Pareto frontier is
  formed and before local marginal-drop selection. Traversing that frontier in
  increasing transfer order, the first point is retained and a later point
  creates a new meaningful comfort point only when it improves `candHard` by at
  least 1.0 second relative to the last retained meaningful point. Otherwise it
  belongs to the same comfort plateau, represented by the lower-transfer retained
  point. This resolution MUST NOT quantize rider search, eligibility, dominance,
  safety calculations, or any pre-selection state.
- **FR-004**: Strengthen the transfer-physics regression so ~30 m chunking and
  short-tail merging are behaviorally necessary.
- **FR-005**: Strengthen the final-audit regression so the real audit path rejects
  deliberately inconsistent stored rider metrics.
- **FR-006**: Document only the durable admissibility boundary for pre-search
  connector dominance, with a regression showing why continuation-sensitive
  connector alternatives cannot be collapsed.
- **FR-007**: Verify whether the registered `Full SDD Cycle` is reachable. If it
  is, correct the repository-owned integration point by adding a project-local
  overlay workflow that extends the lifecycle through the constitution's later
  stages (converge, independent review, traceability verification), without
  editing bundled generated workflow material as though it were the owner.
- **FR-008**: Apply invariant promotion before test-traceability synchronization.
- **FR-009**: Do not change `chooseAssignment` or any unrelated planner semantics,
  historical rationale, migration history, or unrelated matrix gaps. The only
  exception is the behavior-preserving mechanical adjustment forced by a direct
  compile/test dependency, as bounded by §Out of Scope.


## Product Decision — `candHard` Materiality

Adopted by the user on 2026-09-03 as the intentional amendment of
`PC-SELECT-01` (constitution Principle II: affected contract identified in
§Canonical References and FR-002, change explicit in FR-003 and this section,
human approval recorded here, canonical owner updated in
`docs/PRODUCT_CONTRACT.md` in this change).

`candHard` remains full-precision throughout exact rider search and all safety
and eligibility logic.

Only the post-search product-selection stage interprets comfort at a 1.0-second
materiality resolution. Sub-second additional comfort is not a new human-meaningful
tradeoff when it requires more transfer; it belongs to the same comfort plateau,
whose representative is the lower-transfer alternative.

This preserves exact search while preventing floating-point-scale comfort gains
from manufacturing a stronger product elbow than an unchanged, established local
tradeoff.

**Architecture synchronization.** The 1.0 s rule remains product semantics
owned by `PC-SELECT-01`. The durable post-search stage placement — exact
eligible Pareto frontier → materiality resolution → local marginal-drop
selector — is a durable architecture boundary owned by `docs/ARCHITECTURE.md`
§Post-search selection, which currently still describes the pre-change frontier
→ selector pipeline. This change must synchronize that section before it can
complete, through the invariant-promotion step; no new product decision is
introduced here, and the architecture landing records the stage placement
only — it must not duplicate the full `PC-SELECT-01` algorithmic wording.

## Acceptance Scenarios

1. A successful trace response with missing, malformed, non-finite, out-of-range,
   or degenerate returned shape is rejected and never inherits `/route` points.
2. The exact counterexample `(10,100),(20,80),(30,60),(100,59)` plus
   `(1000,58.999999)` is preserved as regression evidence (fixture labels:
   `elbow` = `(30,60)`, `tail1` = `(100,59)`): the pre-fix selector
   selects `tail1` after the extension, while the fixed product-selection path
   treats the final sub-second gain as belonging to the comfort plateau already
   represented by `tail1`, leaving the meaningful frontier unchanged, and
   selects the established `elbow` for both the base and extended frontiers.
   A deterministic
   boundary regression additionally confirms that a frontier step improving
   `candHard` by exactly 1.0 s is retained as a new meaningful point while a step
   below 1.0 s coalesces into the same plateau.
3. Transfer physics tests fail if the established chunk boundary or short-tail
   merge behavior is materially changed.
4. A stored rider-metric mismatch reaches `audit` and yields
   `rider metrics recomputation mismatch`.
5. Connector pruning retains a locally attractive but continuation-distinct
   alternative in the `pruneConnectors` output handed to downstream exact
   eligibility.
6. `specify workflow list/info/resolve` cannot present a repository workflow as
   constitutionally complete when it stops at `implement`.
7. The full default regression suite passes before normal completion.

## Canonical References

- Product (canonical owner: `docs/PRODUCT_CONTRACT.md`): `PC-SAFE-01`
  (code conformed to by FR-001; canonical text unchanged), `PC-SEARCH-02` (preserved unchanged by FR-003),
  `PC-SELECT-01` (intentionally amended by FR-002/FR-003; spec text here is the
  Principle II explicit change record, not a second owner), `PC-AUDIT-01`
  (exercised by FR-005).
- Architecture: Connector representations, Transfer rider physics, Connector
  graph, Rider-quality search, Post-search selection, Independent final audit.
- Status: established exact search, selector, representation ownership, and
  final-audit boundary in `docs/CURRENT_STATE.md`.
- Validation dependencies (reference-only; semantics owned by
  `docs/PRODUCT_CONTRACT.md`, not restated or modified by this change):
  `PC-CLI-01` (default-suite execution before production routing, cited by
  quickstart.md) and `PC-OUT-01` (the five production outputs, cited by the
  Valhalla-dependent baseline evidence).
- Rationale only: ADR-0003, ADR-0004, ADR-0005.

## Out of Scope

The following are explicitly out of scope for this change. This list is
self-contained; it does not incorporate any external chat-only list:

- mandatory-trail semantics;
- demanding classification;
- protected-corridor semantics;
- wall thresholds;
- road policy;
- real-ride evidence semantics;
- RAW exactness semantics;
- rider-search continuation state and pruning policy;
- wall-class derivation;
- `chooseAssignment`;
- endpoint policy;
- reconstruction semantics;
- output filenames and reporting semantics;
- unrelated ADRs;
- migration history;
- unrelated `TEST_MATRIX` gaps.

The only exception is a direct, behavior-preserving mechanical adjustment
forced by a compile/test dependency; no other touching of these items is
licensed.
