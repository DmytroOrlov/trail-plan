# Proposed Canonical Wording — Connector-Dominance Admissibility Boundary

**Status**: change-scoped evidence and proposed wording only. This file is NOT a
canonical owner. Finalized for T012 after T011 passed (reconciled against the
actual regression evidence recorded below); reconciled again after the T023
causal-fixture remediation (independent review found the original T011 pair
non-causal).

**Deliverable lifecycle (T011 → T012 → T015)**:

1. Before T011 passes, this file is only a preliminary/template draft. — done
2. Mere existence of this file does not complete T012.
3. T011 supplies the deterministic regression evidence (fully explicit
   deterministic `Connector` pair; `connectorDominates(A, B) == false`;
   B present in the `pruneConnectors` output). — done (test passes)
4. After T011 passes, T012 reconciles and finalizes the proposed wording below
   against the actual T011 evidence, including recording the exact synthetic
   `Connector` fixture values used by the regression as change evidence. Those
   fixture values are regression evidence only — not new product semantics and
   not canonical architecture truth. — done (see §Recorded T011 evidence)
5. Only after that reconciliation may T012 be marked complete.
6. This file remains change-scoped and non-canonical throughout.
7. T015 through `.agents/skills/invariant-promotion/SKILL.md` is the only step
   that may perform the canonical landing in `docs/ARCHITECTURE.md`.

**Evidence basis**: T011 regression, made causal in T023 — "connector
continuation retention keeps ascent-distinct alternatives in the pruned graph"
in the default suite of `trail-plan.scala` (connector A strictly better in
normal transfer yet unable to collapse the ascent-distinct alternative B under
the bit-exact ascent admissibility gate, so B is retained in the
`pruneConnectors` output handed to downstream exact eligibility).

**Canonical target**: `docs/ARCHITECTURE.md` §Connector graph.

## Recorded T011/T023 evidence (change evidence only; synthetic values, not
product or architecture truth)

T011 originally used an equal-everything-but-ascent pair, which the independent
review found non-causal (with the ascent gate disabled the suite still passed).
T023 replaced the fixture with the causal pair recorded below: A is strictly
better than B in normal transfer duration and no worse in every other compared
monotone resource, so A would legitimately collapse B if the bit-exact ascent
admissibility gate were removed.

All three fixtures share `from="X"`, `to="Y"`, `edges=Vector.empty`,
`crr=0.010`, `wall=WallMetrics(0,0,0)`, `physicalWall=0.2`,
`evidenceFloor=0.0`, `effectiveWall=0.2`, `evidence=Vector.empty`, and rider
streaks `streak120=Streak(4.0,4.0,6.0,true,20.0)`,
`streak140=Streak(2.0,2.0,3.0,false,20.0)`, `streak180=Streak.Empty`:

| id | profile | geometry (lat/lon/ele) | rawSeconds | rider duration (s) | t120 | t140 | t160 | candHard | spike | roadStressSeconds | ascentM |
|---|---|---|---|---|---|---|---|---|---|---|---|
| `cont-a` | `Profiles(0)` (v20-h0.25-r0.35) | (53.0, 10.0, 100.0) → (53.0, 10.0001, 100.0) | 100.0 | 100.0 | 5.0 | 2.0 | 1.0 | 8.0 | 3.0 | 10.0 | 5.0 |
| `cont-b` | `Profiles(1)` (v20-h0.50-r0.35) | (53.0, 10.0, 100.5) → (53.0, 10.0001, 100.5) | 110.0 | 110.0 | 5.0 | 2.0 | 1.0 | 8.0 | 3.0 | 10.0 | 5.000000001 |
| `cont-c` | `Profiles(2)` (v20-h0.90-r0.35) | (53.00002, 10.0, 100.0) → (53.00002, 10.0001, 100.0) | 110.0 | 110.0 | 5.0 | 2.0 | 1.0 | 8.0 | 3.0 | 10.0 | 5.0 |

Observed regression facts (asserted by the default suite, suite-pass verified):

- `a.rider.duration < b.rider.duration` (100 < 110) and every other compared
  monotone resource is equal — the ascent gate is the only blocker between A
  and B (causality precondition).
- `connectorDominates(cont-a, cont-b) == false`: `cont-a` is no-worse in every
  compared monotone resource with a strict transfer advantage, but the bit-exact
  ascent admissibility gate fails (`doubleToLongBits(5.0) !=
  doubleToLongBits(5.000000001)`), so `cont-b` cannot be collapsed. Without the
  gate, `cont-a` would dominate `cont-b` (no-worse everywhere, strict transfer)
  and this assertion plus the pruning assertion would fail; verified by the
  T025 mutation check on a temporary copy outside the repository.
- `connectorDominates(cont-b, cont-a) == false` symmetrically (worse transfer).
- `connectorDominates(cont-a, cont-c) == true`: bit-exact-equal ascent with a
  strictly worse transfer (100 < 110) — the collapse control stays pruned.
- `pruneConnectors(Vector(cont-a, cont-b, cont-c)).map(_.id).toSet ==
  Set("cont-a", "cont-b")`: the continuation-distinct `cont-b` survives into
  the pre-search connector graph handed to downstream exact eligibility; the
  dominated copy `cont-c` does not.
- Distinct ids, profiles, and geometry bit patterns keep the pair/triple
  separate through the bit-exact semantic dedupe inside `pruneConnectors`.

## Proposed wording (to be adjudicated by invariant-promotion in T015)

Reconciled against the T011 evidence above: the surviving observable is that a
connector which is no-worse in every simple local cost still cannot be
collapsed when any continuity-sensitive compared component (ascent under the
bit-exact gate, or another compared monotone resource) differs, and only the
retention into the `pruneConnectors` output — the pre-search graph handed to
downstream exact eligibility — is claimed. No planner behavior change was
introduced (FR-009).

Pre-search connector dominance may collapse a connector only when the
dominating connector is locally no-worse across all compared monotone
resources with bit-exact equal continuity-sensitive components (e.g., ascent).
A continuation-sensitive alternative that differs in any resource required for
safe interchangeability downstream is admissible and must survive pruning into
the connector graph handed to exact eligibility; locally attractive
alternatives may not be collapsed on transfer/road comparison alone.

**Landing authority**: `.agents/skills/invariant-promotion/SKILL.md` applied in
T015 decides the semantic classification and performs the canonical landing in
`docs/ARCHITECTURE.md`. This draft must not be treated as current
architecture truth before that step.
