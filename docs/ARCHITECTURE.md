# Trail Plan Architecture

Baseline: `PRODUCT-V6-GREENFIELD1-FIX54-FREEZE-CANDIDATE`

This document describes the current implementation architecture of `trail-plan.scala`.

It is not the product contract and not a history of experiments.

- Current established/rejected decisions belong in `docs/CURRENT_STATE.md`.
- Normative externally visible requirements belong in `docs/PRODUCT_CONTRACT.md`.
- Historical rationale for individual decisions belongs in ADRs.

## System shape

Production code remains one Scala script:

`trail-plan.scala`

The main production flow is:

```text
GPX inputs
  |
  +-- mandatory technical GPXs
  +-- avoid GPXs
  +-- real-ride GPXs
  |
  v
input validation + canonical trail data
  |
  v
real-ride evidence derivation
  |
  v
Valhalla connector generation
  |
  v
connector safety + rider/road metrics
  |
  v
semantic dedupe + connector Pareto pruning
  |
  v
connector graph
  |
  +--> exact RAW DP for LOOP
  +--> exact RAW DP for P2P
  |
  v
wall classes + endpoint assignment
  |
  v
exact rider-quality search per class
  |
  v
post-search local marginal-drop selection
  |
  v
canonical route reconstruction
  |
  v
independent final audit
  |
  v
GPX + human report + debug output
```

The important architectural boundary is between **connector construction**, **exact search**, **post-search product selection**, and **independent reconstruction/audit**. These stages must not silently absorb each other's responsibilities.

## Input roles

### Mandatory technical GPXs

Mandatory GPXs are canonical trail data and participate in the solver graph.

Their supplied point sequence and elevation are preserved for final reconstruction (`PC-MAND-04`).

Canonical mandatory elevation comes from supplied GPX `<ele>`.

### Avoid GPXs

Avoid GPXs do not participate in route ordering.

They become protected corridors used to prevent transfer reuse.

### Real-ride GPXs

Real rides are validated inputs used to derive safety-active evidence.

Evidence is attached to recognized mandatory-to-mandatory transfer labels. A recording is not treated as a generic safety corridor for unrelated connectors.

## Valhalla boundary

Valhalla is responsible for transfer routing and provides three different kinds of data:

- `/route` — routed transfer geometry and Valhalla trip time;
- `/trace_attributes` with `edge_walk` — edge attributes and the geometry whose shape indices those edges reference;
- `/height` — elevation for the geometry explicitly supplied to it.

The planner owns the semantics built from those responses.

Valhalla `trip.summary.time` is retained as routing provenance. Production transfer optimization uses the planner's rider/terrain physics duration.

## Connector representations

A `Connector` intentionally contains two different geometries.

They are not interchangeable.

| Representation | Source | Owns |
|---|---|---|
| `geometry` | `/route`, segment-wise resampled to <=10 m, then `/height` | connector wall metrics, rider physics, ascent, real-ride evidence application, protected-corridor final check, GPX reconstruction |
| `traceGeometry` | `/trace_attributes` `edge_walk`, then `/height` | edge `begin/end` index space, road classification, modeled road-run duration, road stress |

`EdgeAttr.begin` and `EdgeAttr.end` refer only to `traceGeometry`.

Never use those indices against `geometry`.

Never replace `geometry` with `traceGeometry` for reconstruction or wall/physics calculations.

## Protected-corridor safety

Mandatory and avoid GPXs are both represented as `ProtectedCorridor`s during connector construction.

Protected-corridor policy (`PC-PROT-*`) is evaluated on a canonical safety representation produced from `/route` geometry by segment-wise resampling to at most 10 m.

Continuous co-travel is measured geometrically against the protected corridor tube. It is not endpoint-only and must not depend on arbitrary raw Valhalla vertex spacing.

Connector construction follows this loop:

```text
Valhalla /route
  |
  v
canonical <=10 m safety geometry
  |
  v
protected-corridor overlap?
  | no
  |------------------------------+
  |                              |
 yes                             v
  |                       /trace_attributes
  v
derive blocker point
  |
  v
add avoid_location
  |
  +------> reroute with /route
```

Blocker points are derived from the actual offending routed shape.

If blocker derivation cannot make progress, the connector variant is rejected.

After elevation and all connector metrics are built, protected-corridor overlap is checked again on final connector `geometry`.

## Road and safety classification

Road safety uses `EdgeAttr` plus `traceGeometry`.

Hard-invalid road/use/surface conditions (`PC-ROAD-01`) are rejected during connector construction. Missing data required to establish road safety also fails closed (`PC-SAFE-01`).

Finite road exposure that belongs to the scored road policy (`PC-ROAD-02`) remains a cost (`roadStressSeconds`) rather than being converted into a new hard graph deletion.

This distinction matters: **hard safety determines connector existence; scored exposure participates in dominance and route quality**.

## Transfer rider physics

Modeled transfer duration and rider metrics are computed from connector `geometry` in ~30 m grade chunks; a trailing chunk shorter than half the window is merged into the previous chunk.

Downhill chunks coast when gravity can sustain practical speed and contribute zero rider power. Mandatory technical descents apply the `PC-RIDER-01` cap within the same physics. Exact 30 m / 100 m wall grades (`PC-WALL-01`, `PC-WALL-02`) are evaluated independently of this chunking.

Power-streak metrics concatenate across component boundaries: prefix, suffix, and cross-boundary durations merge so that a streak continuing across a chunk or component boundary is measured as one continuous streak.

## Real-ride evidence

Real-ride evidence is derived before connector graph construction.

The implementation first recognizes mandatory-trail occurrences inside rides and extracts the transfer segments between them. Evidence is then derived only for transfer labels supported by the required repeated recordings.

When evidence applies to a connector:

```text
physical wall severity
                     max --> effectiveWall
          /
real-ride evidence floor
```

`effectiveWall` is the wall value seen by connector pruning and route search.

Evidence is safety-active (`PC-EVID-01`): if the resulting effective wall reaches the hard envelope, the connector is rejected.

## Connector graph

The graph contains logical transitions:

```text
START -> mandatory
mandatory -> different mandatory
mandatory -> FINISH_LOOP
mandatory -> FINISH_P2P
```

Each logical transition may have multiple connector variants generated using the current production Valhalla profile cover.

Before entering the solver, variants are reduced in two distinct ways:

1. **semantic dedupe is bit-exact**;
2. remaining variants are removed only by defined connector Pareto dominance.

Near-identical geometry is not treated as semantic equality by rounding.

Changing the production profile cover changes the connector graph search space and is therefore not a local tuning change.

## RAW search

RAW search is an exact DP over the connector graph it receives (`PC-SEARCH-01`).

Its primary continuation state is:

```text
(mask of visited mandatory trails, last mandatory trail)
```

For each state it maintains the non-dominated `(effective wall, transfer)` labels required by RAW semantics.

RAW is run independently for both endpoint modes:

- `LOOP`
- `P2P`

Only complete orders become RAW terminals.

The RAW frontiers are then used to derive useful wall breakpoints and the three product wall classes.

Endpoint assignment is chosen only after those RAW frontiers exist.

## Rider-quality search

For each selected wall class and endpoint mode, the fastest RAW route is the baseline.

Rider search then performs an exact search for guard-safe strict `candHard` improvements over that baseline.

Its continuation state includes more than `(mask, last)`. In particular, climb-history fields that affect future semantics are part of the exact state grouping.

Dominance is only performed inside equivalent future-continuation states.

The search has no fixed time horizon (`PC-SEARCH-02`).

Pruning against the RAW baseline is allowed only for resources whose future contribution is monotone and which therefore can no longer recover into an eligible upgrade. This is the admissible pruning permitted by `PC-SEARCH-02`, not beam/top-K/epsilon approximation.

## Post-search selection

Product selection happens after exact rider search.

Eligible terminal upgrades form an exact two-dimensional Pareto frontier:

```text
transfer seconds
vs.
candHard seconds
```

Production selects the local marginal-drop elbow.

The selector compares neighboring frontier segment slopes and chooses the point before the strongest local collapse in marginal comfort benefit.

This stage is a **product selector**, not a search-space pruning mechanism.

Changing this selector must not change what rider states are explored.

## Reconstruction

Search results contain an order of canonical mandatory trails plus connector objects.

Final GPX geometry is reconstructed by alternating:

```text
connector.geometry
mandatory canonical GPX
connector.geometry
mandatory canonical GPX
...
finish connector.geometry
```

At connector/mandatory stitch points, canonical mandatory GPX geometry and elevation win.

Reconstruction must not replace or resample the supplied mandatory point sequence (`PC-MAND-04`, `PC-RECON-01`).

## Independent final audit

The final audit runs after reconstruction and is a separate correctness boundary.

It independently checks/recomputes, among other things:

- route mode and connector roles;
- mandatory order/set consistency;
- mandatory supplied sequences in reconstructed geometry;
- demanding classification;
- transfer, rider and climb metrics;
- road stress from `traceGeometry`;
- connector wall metrics;
- real-ride evidence and effective wall;
- protected-corridor overlap;
- wall-class compliance;
- reconstructed GPX gaps (`PC-GAP-01`).

Hard audit failures prevent successful production output (`PC-AUDIT-01`).

The audit should remain independent enough to detect disagreement between search state, connector metadata, and reconstructed output.

## Execution and observability

Default CLI execution runs the contract/regression suite before production routing (`PC-CLI-01`); the suite is defined and executed in-file.

Production execution records live and structured diagnostics in `day.debug.txt`, including graph construction, rejections, RAW frontiers, wall classes, endpoint assignment, rider search and final audits.

Diagnostics are observability. They must not become hidden inputs to production semantics.

## High-risk coupling points

Changes in these areas require special care:

| Change | Architectural blast radius |
|---|---|
| GPX parsing / canonical elevation | mandatory reconstruction, rider physics, demanding classification, audit |
| `resampleConnectorPhysics` / safety sampling | wall physics, elevation sampling, protected-corridor decisions, reconstruction |
| `traceGeometry` or edge index handling | road safety and road stress |
| protected-corridor matcher/blocker logic | connector graph topology |
| `semanticKey` / connector dominance | connector graph search space |
| RAW DP state/dominance | exact wall/transfer frontier and derived classes |
| rider state key/dominance/pruning | exact rider search |
| local marginal-drop selector | final product choice only |
| reconstruction stitch logic | canonical mandatory geometry/elevation |
| final audit | last independent safety/correctness boundary |

When changing one of these, do not treat nearby code as interchangeable merely because it uses the same coordinates or metrics.

## Architectural rule of thumb

Prefer explicit ownership boundaries over runtime inference.

When a fact already has a canonical owner, use that owner instead of reconstructing or guessing the fact from another representation.

The planner should stay conceptually divided into:

```text
canonical input
-> connector construction
-> exact graph search
-> post-search selection
-> reconstruction
-> independent audit
```

A change that blurs these boundaries should be treated as an architectural change, not a local refactor.
