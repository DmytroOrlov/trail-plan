# ADR-0005: Select rider products by local marginal-benefit drop, not global-extrema normalization

Status: Accepted  
Type: Retrospective reconstruction

## Context

The preserved state-2 planner used an exact rider search followed by a post-search selector over the exact `(transfer, candHard)` Pareto frontier.

That architectural separation remains valid, but the concrete selector in state 2 normalized both frontier axes between global transfer-first and comfort-first extremes and selected the point maximizing normalized comfort gain minus normalized transfer cost.

Subsequent search-cover evidence expanded the frontier with additional, far comfort-tail alternatives. The selected route could move even when the newly added alternatives did not participate in the selected route and did not change the neighboring tradeoff geometry around the previously established elbow.

The investigation therefore exposed a product-selection instability caused by global endpoint normalization, not by the exact rider search itself.

The later preserved state-2.1/current planner keeps the same search/selection boundary but replaces the global-extrema-normalized knee with a local marginal-benefit-drop selector. It compares neighboring frontier segment slopes and chooses the point immediately before the strongest local collapse in additional comfort benefit per additional transfer second.

The reproduced failure was also promoted into the default-running regression suite: a far, low-marginal-benefit comfort-tail extension must not move an already-established local elbow when the extension does not alter the neighboring tradeoff geometry around that elbow.

## Decision

Use a local marginal-benefit-drop rule for post-search rider product selection.

Specifically:

1. Build the exact eligible `(transfer, candHard)` Pareto frontier after exact rider search.
2. Evaluate the selector from local neighboring frontier geometry rather than from normalization against global frontier extrema.
3. Select the frontier point immediately before the strongest local collapse in marginal `candHard` benefit per additional transfer second.
4. Adding a far, low-marginal-benefit comfort tail must not move an existing local elbow when the new alternatives do not change the neighboring tradeoff geometry around that elbow.
5. This stability rule does not mean that arbitrary new Pareto alternatives may never change the selected product; alternatives that change the relevant local tradeoff may legitimately move the selected point.
6. Selector policy remains downstream of exact search and must not be folded into pruning or otherwise change which eligible terminals exist.

## Consequences

### Positive

- Product selection is stable against irrelevant expansion of distant frontier extrema.
- The selector no longer depends on global endpoint normalization.
- The same-unit tradeoff between modeled transfer seconds and `candHard` seconds can be evaluated directly through local marginal slopes.
- Search-cover experiments can add distant alternatives without changing an unchanged local elbow merely by stretching the normalization range.
- The exact-search architecture from ADR-0003 remains unchanged.

### Constraints

- The selector operates only on the exact post-search eligible frontier.
- Search completeness, rider-state continuation semantics, admissible pruning, and guardrails remain independent from selector policy.
- Regression protection must reproduce the historical global-knee instability and verify the local selector's bounded stability property.
- A broader rule such as "adding any Pareto point may never change the selected route" is not adopted.
- Future selector changes require evidence against the established local-stability property rather than merely matching one canonical output.

## Rejected alternatives

### Keep the global-extrema-normalized Pareto knee

Rejected because expanding only the far comfort tail can alter the normalization range and move the selected product even when the local tradeoff around the prior elbow is unchanged.

### Treat the candidate16 profile expansion as the permanent fix

Rejected. The additional profiles were evidence used to expose selector instability. Later validation showed selected RAW baselines and final routes used none of those added profiles, and production returned to the validated 12-profile cover while retaining the local selector.

### Freeze the selected route or frontier point by identity

Rejected. The durable requirement is local tradeoff stability under an irrelevant far-tail extension, not immutability of the selected route under every legitimate frontier change.

### Move the selector into exact-search pruning

Rejected. ADR-0003 already separates exact rider search from downstream product selection; the selector must not define the explored search space.

## Historical evidence

This ADR was reconstructed from preserved source snapshots.

### Previous state: `trail-plan-2.scala`

Build ID:

`PRODUCT-V6-GREENFIELD1-FIX44-BLOCKER-DEAD-EVIDENCE-CLOSED`

Relevant preserved behavior:

- rider search has no fixed time horizon;
- product selection occurs after exact search;
- eligible rider terminals form a `(transfer, candHard)` Pareto frontier;
- production uses a global-extrema-normalized Pareto knee;
- the selector normalizes between the transfer-first and comfort-first frontier extremes.

### Next state: `trail-plan-2.1.scala`

Build ID:

`PRODUCT-V6-GREENFIELD1-FIX54-FREEZE-CANDIDATE`

Relevant preserved behavior and evidence:

- FIX50 demonstrates that far comfort-tail additions can move the old global-normalized knee even when the selected route uses no added connector;
- FIX51 replaces that selector with local marginal-benefit drop;
- FIX52 promotes the reproduced search-space-extension stability property into the default-running regression suite;
- the regression explicitly proves that the old global selector moves on the historical-style fixture while the local selector remains on the unchanged local elbow;
- FIX53 restores the validated production 12-profile cover after the added candidate profiles are shown unused by RAW baselines and final selected routes;
- exact DP/frontier construction, no-horizon search, and product guardrails remain separate from the selector change.

## Non-ADR transition evidence

The state-2 → state-2.1 transition also contains substantial contract-test hardening, output-surface checks, final GPX gap thresholds, temporary profile-cover experiments, diagnostics, cleanup, and evidence closure.

Those items are not separate architectural decisions in this ADR:

- the 16-profile midpoint expansion is evidence, later removed;
- restoring production12 is the evidence outcome/current search-cover baseline, not a new general selection architecture;
- test-suite rebalancing is regression/contract protection;
- output filename and final-gap assertions are product-contract/test hardening;
- evidence diagnostics and cleanup do not define production semantics.

## Retrospective note

This ADR was written retrospectively from preserved source states.

It documents the durable selector decision represented by the transition from preserved state 2 to preserved state 2.1/current. It does **not** claim that this ADR file existed when the original investigation or implementation changes were made.

The durable decision is the local selector and its bounded stability property. The temporary candidate-profile expansion and diagnostic sequence are retained only as historical evidence explaining why the global-normalized selector was rejected.
