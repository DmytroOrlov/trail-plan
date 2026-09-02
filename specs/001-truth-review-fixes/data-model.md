# Data Model: Current Truth Review Fixes

This change introduces no new permanent domain entities. `docs/ARCHITECTURE.md`
remains the canonical owner of representation boundaries (Connector
representations, Rider-quality search, Post-search selection, Independent final
audit). This file records only the change-scoped structures the new
regressions/behavior touch.

## Existing structures referenced (owner: `docs/ARCHITECTURE.md`)

- `RiderTerminal` — frontier point with `transfer` (s) and `rider.candHard` (s);
  full precision everywhere pre-selection.
- Trace attributes response — Valhalla `/trace_attributes` JSON with its own
  `shape` and `edges`; distinct from the request `/route` shape.
- `Connector` — pre-search graph edge subject to `connectorDominates` pruning.

## Change-introduced intermediate: meaningful product frontier

Derived inside post-search selection only, per `PC-SELECT-01`.

| Field | Type | Rule |
|---|---|---|
| source point | `RiderTerminal` | member of the exact eligible Pareto frontier; unmodified |
| retained-as-meaningful | boolean | true for the first point; true for a later point only if `candHard` gain ≥ 1.0 s vs. the last retained point |
| plateau representative | `RiderTerminal` | the lower-transfer retained point that a sub-1.0 s gain coalesces into |

Constraints:

- Formation order is increasing `transfer`.
- Comparison is `>= 1.0 s` (exactly 1.0 s gain is retained).
- `candHard` values are never mutated or quantized; the rule only chooses which
  points are meaningful inputs to the existing local marginal-drop selector.
- The frontier this stage consumes is exactly the frontier `PC-SEARCH-02`
  produces; no search-state variant of it exists.

## Validation rules used by new fixtures

### Trace-shape fixtures (`FR-001`, Acceptance Scenario 1)

A `/trace_attributes` response body is valid only if its own `shape` decodes to
a non-degenerate polyline: present, well-typed string, finite decoded lat/lon,
in range (lat ∈ [-90, 90], lon ∈ [-180, 180]), and at least 2 points. Any
violation (missing, malformed, non-finite, out-of-range, degenerate) fails
closed; the request `/route` shape is never substituted.

### Selector materiality fixtures (`FR-002`, `FR-003`)

| Fixture | Frontier points (transfer, candHard) | Expected |
|---|---|---|
| counterexample base | (10,100),(20,80),(30,60),(100,59) | selects `elbow` (30,60) |
| counterexample extended | base + (1000,58.999999) | selects `elbow` (sub-1.0 s gain coalesced) |
| boundary retained | …,(30,60),(100,59.0) | final point is a new meaningful point (gain exactly 1.0 s) |
| boundary coalesced | …,(30,60),(100,59.000001) | final point plateaus onto the (30,60)-representative chain (gain 0.999999 s < 1.0 s) |

Boundary fixture values must be exactly representable comparisons under
`Double` (e.g., 60.0 − 59.0 = 1.0 holds bit-exactly); avoid values whose
difference wobbles around the 1.0 threshold.

### Transfer-physics fixtures (`FR-004`, Acceptance Scenario 3)

Source-derived current behavior being made behaviorally necessary
(implementation source under test: `trail-plan.scala`; canonical architecture
ownership remains in `docs/ARCHITECTURE.md`; these fixtures pin executable
behavior and are regression evidence, not a canonical owner — and they
introduce no new rider policy):

- `PhysicsGradeWindowM = 30.0` (trail-plan.scala:143); a chunk flushes when
  accumulated horizontal distance `>= 30 m` or at route end
  (trail-plan.scala:740); chunk grade is the anchor→flush-point net elevation
  delta divided by chunk distance (trail-plan.scala:742, 753).
- Short-tail merge runs only when there are `>= 2` chunks and the final chunk
  distance is `< 15.0 m` (`PhysicsGradeWindowM * 0.5`,
  trail-plan.scala:747-750), summing distance and elevation delta into the
  previous chunk.

Construction reuses the existing suite's `east(m, ele)` helper
(`origin = Point(53.0, 10.0, 100.0)`, eastward meter offsets as at
trail-plan.scala:3326-3331) and `physics(points, 0.010)` with no downhill cap.
Haversine cumulative distances for the chosen meter offsets were verified to
land on nominal values to `1e-9 m`; every flush/merge decision margin is at
least 0.4 m, so no boundary can wobble across its threshold.

#### Fixture 1 — the 30 m window is behaviorally necessary

Nodes at 0, 10.5, 21.0, 31.5, 42.0, 52.5, 63.0 m with elevations
0, −1.5, −4.0, 0.0, +4.0, +1.5, 0.0 m.

Canonical 30 m window flushes at 31.5 m and 63.0 m (final), producing two
31.5 m chunks whose net grades are both exactly flat; boundary variants
produce materially different results (verified by running the current code
with the window constant replaced):

| Window | Flush boundaries | Chunks | `duration` (s) | `candHard` (s) | `spike` |
|---|---|---|---|---|---|
| 30 m (current) | 31.5, 63.0 | 31.5 m @ 0%, 31.5 m @ 0% | **13.522297774009317** | **0.0** | **0.0** |
| 20 m variant | 21.0, 42.0, 63.0 | incl. 21 m @ +38% (≥180 W) | 22.430270498 | 61.007496081 | 568.319820 |
| 40 m variant | 42.0, 63.0 | 42 m @ +9.5%, 21 m coast | 41.718883290 | 0.0 | 0.0 |

Asserted observable (with the suite's existing `near(..., 1e-6)` style
tolerance): `duration = 13.522297774009317`, and exact structural zeros for
`t120`, `t140`, `t160`, `candHard`, `spike`, `streak180.localMax`. The
20 m world is caught by `candHard`/`spike`; the 40 m world by `duration`
alone. The test asserts the metrics, never `PhysicsGradeWindowM == 30.0`
directly.

#### Fixture 2 — the short-tail merge is behaviorally necessary

Pair with a flat 30.4 m leading chunk (nodes 0, 15.2, 30.4; elevations 0, 0, 0)
plus one steep +5.0 m tail:

| Case | Tail nodes | Tail chunk distance vs 15.0 m threshold | Merge? | `duration` (s) | `t120=t140=t160` (s) | `candHard` (s) | `spike` |
|---|---|---|---|---|---|---|---|
| merged | 0, 15.2, 30.4, 44.5 (ele 0,0,0,+5) | 14.1 m (margin 0.9 m below) | yes → single 44.5 m chunk @ 11.24% (~111 W) | **43.092596438400820** | **0.0** | **0.0** | **0.0** |
| unmerged | 0, 15.2, 30.4, 46.5 (ele 0,0,0,+5) | 16.1 m (margin 1.1 m above) | no → 30.4 m flat + 16.1 m @ 31.1% (~278 W) | **22.115849829214582** | **15.590804554134040** | **46.772413662402120** | **242.68284506105906** |

The pair brackets the half-window boundary from both sides with ≥0.9 m
margin:

- Removing the merge (or lowering its factor below 14.1/30 = 0.47) flips the
  merged case to `candHard ≈ 40.96 s` and `duration ≈ 20.18 s` (verified).
- Raising the merge threshold above 16.1 m flips the unmerged case's
  `candHard` from 46.77 s to 0.
- The merged case is *slower* in `duration` than the unmerged case
  (43.09 s vs 22.12 s); the regression intentionally pins the full asserted
  metric tuple, not a speed intuition.

### Connector continuation fixture (`FR-006`)

A connector pair (A, B) where A is locally no-worse in transfer/road, but a
continuation-sensitive resource required for safe interchangeability differs
(the bit-exact ascent admissibility gate fails, or another compared monotone
resource is worse), so `connectorDominates(A, B)` is **false**. B's distinct
continuation geometry matters for downstream exact eligibility; the
regression asserts B appears in the `pruneConnectors` output — membership in
that output is the concrete observable, because it is the pre-search connector
graph handed to downstream exact eligibility (no separate downstream probe).
This evidences the admissibility boundary under which
continuation-sensitive alternatives cannot be collapsed; no planner behavior
change is introduced.

### Audit mismatch fixture (`FR-005`)

A stored `RiderTerminal` whose metrics are deliberately inconsistent with
recomputation from its connectors/trails; `audit` must reject with
`rider metrics recomputation mismatch`.
