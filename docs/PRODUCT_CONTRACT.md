# Trail Plan Product Contract

This document defines the normative guarantees of the Trail Plan product.

It answers **what must remain true**, independent of the current implementation.

It intentionally does **not** describe:

- implementation stages, algorithms, data structures, geometry/index representations, or Valhalla API usage — see `ARCHITECTURE.md`;
- which alternatives are currently established, rejected, or open — see `CURRENT_STATE.md`;
- governance and change discipline — see `../.specify/memory/constitution.md`;
- historical experiments or rationale — use ADRs / evidence history.

Contract IDs are intended to remain stable. If implementation changes while a contract remains valid, keep the ID and wording unless the requirement itself changes.

## Mandatory technical trails

### PC-MAND-01 — Independent requirements

Every supplied mandatory technical GPX is an independent required trail.

Satisfying one mandatory trail cannot satisfy another.

### PC-MAND-02 — Exactly once

Every produced route must contain every supplied mandatory technical GPX exactly once.

### PC-MAND-03 — Supplied direction

Every mandatory technical GPX must be traversed in its supplied direction.

A reversed occurrence does not satisfy the requirement.

### PC-MAND-04 — Canonical mandatory sequence

Final reconstructed GPX output must preserve the supplied mandatory point sequence and its canonical elevation values.

Connector-derived data must not replace the canonical mandatory sequence.

## Demanding-trail policy

### PC-DEMAND-01 — Demanding classification

A mandatory trail is demanding when any of the following holds:

- whole-trail downhill grade is at least 10% **and** whole-trail sinuosity is at least 1.10;
- a 60 m window has downhill grade at least 18% **and** sinuosity at least 1.20;
- a 100 m window has downhill grade at least 15% **and** sinuosity at least 1.20.

Whole-trail grade is normalized by travelled horizontal polyline length.

The 60 m and 100 m tests are independent.

### PC-DEMAND-02 — Warm-up before demanding trail

A demanding trail must not be the first mandatory trail of a produced route.

At least one non-demanding mandatory trail must precede the first demanding trail.

A second warm-up and avoidance of demanding-to-demanding adjacency are quality preferences, not hard feasibility requirements.

## Protected corridors

### PC-PROT-01 — No transfer-corridor reuse

Mandatory technical GPXs and explicit avoid GPXs are protected from reuse as ordinary transfer corridors.

### PC-PROT-02 — Co-travel, not mere crossing

Protected-corridor policy applies to genuine co-travel.

A perpendicular or sufficiently crossing-like geometric intersection must not be rejected merely because the transfer intersects the protected corridor.

### PC-PROT-03 — Protected overlap threshold

Protected-corridor co-travel longer than 12 m is hard-invalid.

Non-zero protected co-travel up to and including 12 m is a warning rather than a hard failure.

The classification must not depend on incidental segmentation of the routed geometry.

## Hard safety and road policy

### PC-SAFE-01 — Fail closed

When data required to establish a hard safety condition is unavailable, invalid, non-finite, or cannot be mapped to the relevant route evidence, the affected connector or production run must fail closed rather than silently become safe.

### PC-WALL-01 — 30 m wall limit

A connector with maximum exact 30 m uphill grade greater than or equal to 27% is hard-invalid.

### PC-WALL-02 — 100 m wall limit

A connector with maximum exact 100 m uphill grade greater than or equal to 20% is hard-invalid.

### PC-WALL-03 — Sustained power wall limit

A connector with a continuous rider-power streak above 180 W lasting at least 90 seconds is hard-invalid.

### PC-ROAD-01 — Forbidden road/use classes

Motorway, trunk, steps, ferry, rail/rail-ferry, and impassable surface usage are hard-invalid.

### PC-ROAD-02 — Scored primary-road exposure

Finite unprotected-primary exposure is a route-quality cost, not by itself a hard rejection.

If required primary-road exposure cannot be modeled, the connector fails closed.

### PC-EVID-01 — Real-ride evidence is safety-active

Qualified real-ride evidence is safety-active rather than diagnostic-only.

It may raise a connector's effective wall severity, and a connector whose effective severity reaches the hard wall envelope is invalid.

### PC-EVID-02 — Real-ride direction

Real-ride wall evidence must match the ridden transfer direction.

Geometric similarity in the opposite direction is not sufficient.

### PC-RIDER-01 — Technical downhill policy

Mandatory technical GPX downhill rider physics uses the established 6 km/h technical descent cap.

This policy is specific to mandatory technical trail riding and must remain distinct from ordinary transfer downhill physics.

## Exact search

### PC-SEARCH-01 — Exact mandatory ordering

RAW ordering must remain exact on the connector graph it receives.

Search must not discard a feasible optimum through beam search, top-K pruning, epsilon/quantized dominance, arbitrary detour cutoffs, or equivalent approximation.

### PC-SEARCH-02 — Exact rider-quality search

Rider-quality search must remain exact over its defined continuation state and has no fixed transfer-time horizon.

Any pruning must be admissible: it may remove a partial state only when monotone already-consumed resources prove that no continuation can become an eligible result.

## Wall classes and endpoint roles

### PC-CLASS-01 — Useful wall levels

For each endpoint mode, the first reachable wall level is useful.

A later wall level becomes useful when, relative to the previous useful level, it either:

- improves fastest RAW transfer by at least 180 seconds; or
- changes the natural mandatory-trail order.

### PC-CLASS-02 — C1 / C2 / C3 derivation

The product requires three distinct useful wall severities.

From the union of useful RAW wall severities:

- C1 is the lowest useful severity;
- C3 is the highest useful severity;
- C2 is the highest useful severity strictly between C1 and C3.

If three distinct useful severities cannot be established, production must fail rather than invent numeric class targets.

### PC-ENDPOINT-01 — Two LOOP, one P2P

The three product routes contain exactly two LOOP endpoint roles and one P2P endpoint role.

The P2P role is not permanently attached to C1, C2, or C3.

Endpoint assignment is selected across the three possible P2P placements by total RAW transfer, then total RAW road stress, then class index as deterministic tie-breaker.

## Rider-quality upgrades and product selection

### PC-QUALITY-01 — Strict comfort improvement

A rider-quality upgrade over its RAW baseline must strictly improve `candHard`.

It may not worsen the established guarded resources: low/high power streaks, spike load, road stress, climb-shape metrics, warm-up penalty, or demanding-trail adjacency.

For C2/C3, an upgrade must also genuinely require wall severity above the previous class ceiling.

### PC-SELECT-01 — Stable local tradeoff selection

Adding a far, low-marginal-benefit comfort-tail alternative must not move an already-established local tradeoff elbow when the new alternative does not change the neighboring tradeoff geometry around that elbow.

For post-search product selection, `candHard` has a 1.0-second materiality resolution. After the exact eligible Pareto frontier is formed, candidates are considered in increasing transfer order. The first point is retained; a later point creates a new meaningful comfort point only when it improves `candHard` by at least 1.0 second relative to the last retained meaningful point. Otherwise it belongs to the same comfort plateau, represented by the lower-transfer retained point.

The local marginal-drop selector operates on this meaningful tradeoff frontier.

This resolution applies only to post-search product selection. It must not quantize rider search, eligibility, dominance, safety calculations, or any pre-selection state.

This does **not** mean that arbitrary new Pareto alternatives may never change the selected route.

## Reconstruction and final audit

### PC-RECON-01 — Solver/reconstruction agreement

Final reconstruction must preserve the selected mandatory order and connector sequence.

Reconstruction must not change solver semantics while stitching the final GPX.

### PC-AUDIT-01 — Final contract audit

Every produced route must pass the final contract audit.

A hard final-audit failure prevents successful production output.

### PC-ENDPOINT-02 — Endpoint continuity

A reconstructed route must start within 5 m of the configured start point and finish within 5 m of the endpoint required by its selected mode.

### PC-GAP-01 — Final GPX gap levels

A reconstructed GPX point gap:

- below 100 m is normal;
- from 100 m up to but excluding 250 m is a warning;
- at or above 250 m is a hard failure.

## Execution and output surface

### PC-CLI-01 — Tests run by default

The default CLI execution runs the contract/regression suite before production routing.

`--no-test` is the explicit way to skip that pre-run suite.

### PC-OUT-01 — Production outputs

A successful production run produces these planner-owned output files:

- `day.gpx`
- `day.wall-c2.gpx`
- `day.wall-c3.gpx`
- `day.txt`
- `day.debug.txt`

The three GPX files are the C1, C2, and C3 product routes respectively.

### PC-OUT-02 — Endpoint meaning

All three product routes start at the configured S-Neuwiedenthal start role.

A LOOP route finishes at that start role.

A P2P route finishes at the configured S-Heimfeld finish role.

### PC-REPORT-01 — Report-only stop convention

The human report adds 3 minutes before and 3 minutes after every mandatory technical trail to planned time.

This convention is reporting-only and must not affect connector generation, route search, wall classes, rider metrics, or GPX geometry.

## Contract maintenance

A new permanent product invariant discovered through investigation should be added here only when it is genuinely normative rather than dataset-specific or implementation-specific.

Do not add:

- current Build IDs;
- production profile lists;
- canonical-dataset counts or expected trail names;
- internal case classes/functions/state keys;
- route-vs-trace representation details;
- rejected historical experiments;
- one-run performance measurements.

Those belong in current-state, architecture, baseline/provenance, ADR, evidence, or test documentation instead.
