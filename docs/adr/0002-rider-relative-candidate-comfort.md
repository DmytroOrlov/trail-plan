# ADR-0002: Replace legacy fixed-power comfort with rider-relative candidate comfort

Status: Accepted  
Type: Retrospective reconstruction

## Context

The preserved pre-migration planner state (`trail-plan-0.5.scala`) still carried human-quality semantics built around fixed absolute power metrics. Its connector and rider-quality state included fixed-power exposure/streak fields such as time above 150/200/250 W, longest 150/180 W streaks, and spike load, while the exact human-quality search operated inside a shared `+60 s` transfer envelope.

The following preserved state (`trail-plan-1.scala`) records a deliberate cleanup and migration:

- the legacy fixed-power comfort system is removed from production/search semantics;
- connector generation, semantic dedupe and Pareto dominance use rider-relative candidate-comfort metrics instead;
- candidate-comfort thresholds are derived from rider target power rather than treated as fixed absolute comfort thresholds;
- the hard `180 W / 90 s` wall signal remains, but only as a separate safety rule rather than rider-comfort preference;
- the old `+60 s` solver remains temporarily as a migration-reference mechanism, with its comparator/resources converted to candidate-comfort semantics;
- those legacy `+60 s` references are retained only as no-regression baselines while a standalone final rider-preference policy is validated.

This transition separates two concerns that had previously been entangled:

1. rider-comfort preference;
2. hard rider-safety limits.

It also makes the remaining `+60 s` mechanism explicitly transitional rather than a permanent source of new rider-quality semantics.

## Decision

Use rider-relative candidate comfort as the production rider-comfort model.

Specifically:

1. Rider comfort is measured relative to the configured rider target power rather than by the removed legacy set of fixed absolute comfort thresholds.
2. Connector generation, semantic dedupe, Pareto dominance and rider-quality comparison use the candidate-comfort resource set.
3. The hard `180 W / 90 s` wall rule remains a distinct safety signal and must not be treated as a comfort preference.
4. The legacy shared `+60 s` mechanism may remain only as a temporary migration/no-regression reference while replacement policy is validated.
5. The migration reference must use the new candidate-comfort semantics; it must not preserve the removed legacy fixed-power comfort system indirectly.
6. Temporary migration references are not themselves evidence that the `+60 s` envelope is a permanent product preference.

## Consequences

### Positive

- Rider comfort scales with the rider model rather than being tied to one fixed set of absolute comfort thresholds.
- Safety and comfort have separate ownership: the hard safety wall can remain stable while comfort policy evolves.
- Connector search/deduplication/dominance no longer depend on obsolete fixed-power comfort fields.
- The old solver can be used as a migration baseline without allowing its legacy comfort metrics to remain product semantics.
- A later standalone rider-preference policy can replace the migration references without restoring the removed fixed-power model.

### Constraints

- Candidate-comfort semantics used by connector generation, search resources and comparison must remain internally consistent.
- Hard safety evidence must not be weakened or reclassified as comfort merely because some power values overlap numerically.
- Migration/no-regression references must not silently become permanent search or product policy.
- Removal of a migration reference requires independent validation of its replacement; simply deleting the baseline is not equivalent to proving the new preference policy.
- Changes to connector-search ordering or dominance caused by comfort-resource changes require full product/safety regression evidence because they can change the connector graph.

## Rejected alternatives

### Keep the legacy fixed absolute power comfort system

Rejected. The preserved next state explicitly removes weighted fixed-power effort/suffering, legacy streak-comfort and legacy spike-load semantics from production/search behavior.

### Treat the hard `180 W / 90 s` safety wall as another comfort metric

Rejected. The next state explicitly marks that signal as hard safety only, separate from rider-comfort preference.

### Preserve the old `+60 s` solver as permanent rider-quality policy

Not adopted as the target architecture. In the preserved next state it remains only as a temporary migration/no-regression reference, with explicit follow-up to replace it using an independently validated standalone final preference policy.

### Remove the migration reference immediately without replacement evidence

Rejected as an unsafe migration shortcut. The preserved next state retains the legacy references specifically as no-regression baselines until replacement preference semantics are independently validated.

## Historical evidence

This ADR was reconstructed from preserved source states.

### Previous state: `trail-plan-0.5.scala`

Product build:

`PRODUCT-V4-36H11-REQUIRED-ENDPOINT-CONTRACT56-FIX1-TERRAIN-PRODUCT1-FIX2`

Relevant preserved behavior includes:

- exact human-quality DP inside one shared `+60 s` envelope;
- connector/rider-quality state containing fixed absolute power exposure and streak metrics;
- human-quality behavior still represented through fields such as time above 150/200/250 W, longest 150/180 W streaks and spike load.

### Next state: `trail-plan-1.scala`

Product build:

`MTB-CANONICAL-REFERENCE2`

`BuildInfo.id`:

`MTB-CANONICAL-REFERENCE2-FIX27-CONTRACT-TESTS`

Its cleanup ledger explicitly records:

- removal of legacy fixed-power comfort from production/search semantics;
- removal of weighted 90/150/200/250 W fetch effort, 150/200/250 W suffering, legacy streak comfort and legacy spike-load semantics;
- use of rider-relative candidate comfort for connector generation, semantic dedupe and Pareto dominance;
- separation of the hard `180 W / 90 s` safety wall from comfort preference;
- temporary retention of migration `+60 s`, with its comparator/resources converted to candidate comfort;
- retention of the three legacy `+60 s` references only as current no-regression baselines;
- an explicit future task to replace those references with an independently validated standalone final preference policy.

## Retrospective note

This ADR was written retrospectively from preserved source states.

It documents a decision represented by the transition into the preserved `trail-plan-1.scala` state. It does **not** claim that this ADR file existed when the original implementation change was made.

The ADR intentionally records the durable decision and migration boundary, not every cleanup item or temporary metric that appeared during the transition.
