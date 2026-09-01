# ADR-0001: RAW terrain owns wall classes and endpoint assignment

Status: Accepted  
Type: Retrospective reconstruction

## Context

The preserved pre-migration planner snapshot (`trail-plan-0.5.scala`) already had an exact terrain frontier, but product class usefulness was still evaluated through the human-quality policy:

- human quality used one calibrated shared `+60 s` transfer envelope for both mandatory-order changes and connector-variant choices;
- a wall level was considered useful only after applying that same human-quality policy;
- endpoint role was selected from endpoint-specific useful frontiers.

This coupled product topology — the C1/C2/C3 terrain classes and endpoint-role decision — to rider-quality policy.

The following preserved snapshot (`trail-plan-1.scala`) separates those responsibilities:

- wall usefulness is derived from RAW terrain;
- a useful RAW wall level is the first reachable level, or a later level with at least 3 minutes of RAW transfer improvement, or a natural RAW mandatory-order change;
- wall classes are derived from the union of RAW-useful terrain breakpoints;
- endpoint role is selected independently from reachable class/endpoint pairings by total RAW transfer, then road stress, then class index as a deterministic tie-breaker;
- rider-quality policy remains downstream and may improve the selected rider route without defining terrain classes or endpoint placement.

## Decision

Product terrain topology is owned by the exact RAW terrain layer.

Specifically:

1. C1/C2/C3 wall usefulness is derived from RAW terrain behavior, not from rider-quality or human-quality policy.
2. Endpoint role is independent of class identity and is selected from reachable class/endpoint pairings using RAW transfer behavior.
3. Rider-quality selection is downstream of RAW class derivation and endpoint assignment. Rider policy may choose a better rider route inside the established product topology, but it does not define that topology.

## Consequences

### Positive

- Changes to rider-comfort policy do not, by themselves, redefine terrain wall classes.
- Changes to rider-comfort policy do not, by themselves, decide which class is P2P.
- Terrain class derivation can be reasoned about and tested independently from rider preference.
- Endpoint assignment is evaluated independently from the C1/C2/C3 class number.
- Migration or experimental rider-quality policies can be replaced without requiring product terrain classes to be redefined solely because the rider policy changed.

### Constraints

- RAW terrain search must remain exact on the connector graph it receives.
- Wall usefulness must be computed from RAW terrain evidence rather than reconstructed from downstream rider-selected routes.
- Endpoint assignment must not be hardcoded to a specific class number.
- Downstream rider selection must not silently become pruning that changes the RAW terrain frontier.

## Rejected alternatives

### Keep the shared `+60 s` human-quality policy as the owner of wall usefulness

Rejected because it couples terrain-class identity to a rider-quality policy. A rider-policy change could then alter C1/C2/C3 even when RAW terrain behavior is unchanged.

### Select endpoint role from endpoint-specific human-quality useful frontiers

Rejected in favor of selecting endpoint role independently from reachable RAW class/endpoint pairings.

### Hardcode P2P to a specific C-class

Rejected. Endpoint role and terrain severity are separate product dimensions.

## Historical evidence

This ADR was reconstructed from preserved source snapshots:

- previous state: `trail-plan-0.5.scala`
  - product build: `PRODUCT-V4-36H11-REQUIRED-ENDPOINT-CONTRACT56-FIX1-TERRAIN-PRODUCT1-FIX2`
  - human quality owns a shared `+60 s` envelope;
  - wall usefulness is evaluated after the same human-quality policy;
  - endpoint selection uses endpoint-specific useful frontiers.

- next state: `trail-plan-1.scala`
  - product build: `MTB-CANONICAL-REFERENCE2`
  - `BuildInfo.id`: `MTB-CANONICAL-REFERENCE2-FIX27-CONTRACT-TESTS`
  - wall usefulness is explicitly derived from RAW terrain;
  - wall classes come from RAW-useful terrain breakpoints;
  - endpoint role is selected independently from reachable class/endpoint pairings by total RAW transfer, then road stress, then class index as a deterministic tie-breaker;
  - cleanup notes state that legacy `+60` influence on wall classes and endpoint assignment has been removed.

## Retrospective note

This ADR was written retrospectively from preserved source states.

It documents the architectural decision represented by the transition from the preserved `trail-plan-0.5.scala` state to the preserved `trail-plan-1.scala` state. It does **not** claim that this ADR file existed when the original implementation change was made.
