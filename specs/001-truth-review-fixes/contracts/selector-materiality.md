# Contract: Post-Search Selection with 1.0 s `candHard` Materiality

**Status**: change-scoped testable restatement; canonical owner is
`PC-SELECT-01` (`docs/PRODUCT_CONTRACT.md`). The selector mechanism's stage
placement is owned by Post-search selection in `docs/ARCHITECTURE.md`. This
file defines nothing new and must not diverge.

## Stage contract (in `trail-plan.scala`)

```text
exact rider search (PC-SEARCH-02, unchanged)
  -> eligible terminal collection
  -> exact (transfer, candHard) Pareto frontier        # unchanged
  -> 1.0 s candHard materiality resolution             # added stage
  -> local marginal-drop selection (ADR-0005, unchanged semantics
     operating on the meaningful frontier)
```

## Materiality resolution step

Input: frontier ordered by increasing `transfer`, full-precision `candHard`.

1. Retain the first point as meaningful.
2. For each later point, compute the gain relative to the last retained
   meaningful point. Gain < 1.0 s → same comfort plateau; the point is dropped
   and the lower-transfer retained point represents it.
3. Gain ≥ 1.0 s → new meaningful point (exactly 1.0 s qualifies).
4. Output the retained-point sequence to the existing local marginal-drop
   selector. Degenerate frontiers (fewer than 2 meaningful points) follow the
   existing head-selection behavior.

Prohibited effects: no quantization, rounding, or filtering of rider search,
eligibility (`terminalIsEligibleUpgrade`), dominance, safety calculations, or
any pre-selection state (`FR-003`).

## Verification

Default-suite regressions:

- "local selector preserves an established elbow under a near-zero comfort
  tail" — reproduced counterexample; base and extended frontiers both select
  `elbow` (Acceptance Scenario 2).
- materiality boundary test — a frontier step of exactly 1.0 s gain is retained
  as a new meaningful point; a step below 1.0 s coalesces into the plateau.
- pre-fix evidence record: the extended frontier selected `tail1` under the
  pre-fix selector (captured as temporary diagnostics during T003; removed by
  T017 after adjudication per Principle IV).
