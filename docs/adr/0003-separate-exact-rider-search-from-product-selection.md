# ADR-0003: Separate exact rider search from post-search product selection

Status: Accepted  
Type: Retrospective reconstruction

## Context

The preserved `trail-plan-1.scala` state still uses transitional rider-quality search controls inherited from the migration phase:

- a migration-reference DP remains exact only inside its legacy `+60 s` envelope;
- V5 promotion search is exact only inside `PromotionSearchSlackCeilingSeconds`;
- the remaining migration references are explicitly retained as temporary no-regression baselines;
- the source itself records the intended next step: replace those migration references with an independently validated standalone final preference policy and then remove the last legacy `+60 s` solver.

That design still lets a transitional migration envelope define which rider alternatives are explored.

The preserved `trail-plan-2.scala` state adopts a different boundary:

- rider-quality search has no fixed time horizon;
- transfer is not capped by `+60`, `+600`, a migration route, or a fixed detour budget;
- pruning is limited to monotone resources that already make a strict RAW-baseline upgrade impossible;
- eligible rider terminals are collected into an exact transfer-vs-`candHard` Pareto frontier;
- product preference is applied only after exact search completes.

The initial preserved state-2 selector used a normalized Pareto-knee rule. That particular selector is not the durable decision recorded by this ADR; later history may replace the selector while retaining the search/selection boundary.

## Decision

Separate rider-search completeness from product preference.

Specifically:

1. Rider-quality search must explore the complete exact state space required by the current connector graph and rider continuation semantics, subject only to admissible monotone pruning.
2. A migration reference, fixed transfer slack, fixed time horizon, percentage detour budget, beam, top-K cutoff, epsilon grouping, weighted score, or similar product-preference mechanism must not define the rider search space.
3. The fastest RAW route for the selected wall class/mode is the rider-upgrade baseline.
4. Rider candidates must satisfy the established guardrails and strictly improve the rider-quality objective before becoming eligible terminal upgrades.
5. Product preference is a downstream selection stage over eligible exact-search terminals.
6. Changing the post-search selector must not change which rider states are explored or which eligible terminals exist.

## Consequences

### Positive

- Search exactness can be reviewed independently from the rider product selector.
- Replacing a selector does not require changing search completeness.
- Product-preference experiments cannot silently remove feasible rider alternatives from the solver.
- Migration references can be deleted without replacing them with another hidden search horizon.
- Search pruning has a clear admissibility standard: a pruned state must already be unable to recover into an eligible upgrade because of monotone resources.

### Constraints

- Continuation state must contain every history component that can affect future rider semantics.
- Dominance is valid only between states with equivalent future continuation semantics.
- Product-selection logic must run after exact terminal generation; it must not be folded back into DP pruning.
- Any new pruning rule requires an admissibility argument, not merely evidence that the current canonical route remains unchanged.
- A post-search selector may change the final chosen route, but it must not alter the underlying exact eligible frontier.

## Rejected alternatives

### Keep the migration `+60 s` references as permanent search boundaries

Rejected. In the preserved state-1 source they are explicitly transitional no-regression baselines, not the target architecture.

### Replace `+60 s` with another arbitrary fixed slack or horizon

Rejected. This changes the numeric cutoff without fixing the architectural problem: product preference would still define search completeness.

### Use beam, top-K, epsilon grouping, objective quantization, or random pruning

Rejected as search approximation. Such mechanisms can remove alternatives without an admissibility proof.

### Use the post-search selector inside the DP

Rejected because it couples product preference to search completeness and makes selector changes alter the explored state space.

### Treat the first state-2 Pareto-knee formula as the permanent architecture

Not adopted by this ADR. The durable decision is the separation between exact search and downstream product selection. The concrete selector is replaceable policy and may be superseded by later evidence.

## Historical evidence

This ADR was reconstructed from preserved source states.

### Previous state: `trail-plan-1.scala`

Product build:

`MTB-CANONICAL-REFERENCE2`

`BuildInfo.id`:

`MTB-CANONICAL-REFERENCE2-FIX27-CONTRACT-TESTS`

Relevant preserved statements include:

- migration-reference DP exact inside the legacy `+60 s` envelope;
- V5 promotion DP exact inside `PromotionSearchSlackCeilingSeconds`;
- three legacy `+60 s` migration references retained only as current no-regression baselines;
- explicit follow-up to replace the migration references with an independently validated standalone final preference policy;
- explicit follow-up to revalidate or replace `PromotionSearchSlackCeilingSeconds` if completeness must extend beyond the validated canonical dataset.

### Next state: `trail-plan-2.scala`

Build ID in the preserved snapshot:

`PRODUCT-V6-GREENFIELD1-FIX44-BLOCKER-DEAD-EVIDENCE-CLOSED`

Relevant preserved behavior includes:

- rider-quality search has no fixed time horizon;
- exact pruning is limited to monotone resources that already make RAW-baseline upgrade eligibility impossible;
- transfer is not capped by a fixed detour budget;
- no `+60`/`+600` window, migration route, fixed time horizon, weighted score, percentage detour budget, beam, top-K, or epsilon search approximation is used;
- product policy is applied only after exact search;
- eligible terminals form an exact two-dimensional `(transfer, candHard)` Pareto frontier before the selector chooses one output.

The preserved state-2 implementation initially selects a normalized Pareto knee from that frontier. That selector is historical evidence of the post-search boundary, not the permanent subject of this ADR.

## Retrospective note

This ADR was written retrospectively from preserved source states.

It documents an architectural decision represented by the transition from the preserved `trail-plan-1.scala` state toward the preserved `trail-plan-2.scala` state. It does **not** claim that this ADR file existed when the original implementation change was made.

The ADR intentionally preserves the durable boundary — exact search first, product selection second — while leaving concrete selector policy to later ADR/evidence when that policy changes.
