# Trail Plan Test Matrix

Baseline reviewed: `PRODUCT-V6-GREENFIELD1-FIX54-FREEZE-CANDIDATE`

This file maps established product contracts and architecture invariants to the current default-running regression suite.

It answers:

> **What established statement is protected by which executable regression?**

It does **not** define product requirements, architecture, current/rejected decisions, or historical rationale.

Canonical owners:

- product requirements → `PRODUCT_CONTRACT.md`;
- implementation architecture → `ARCHITECTURE.md`;
- established/rejected/open status → `CURRENT_STATE.md`;
- governance/change discipline → `../.specify/memory/constitution.md`.

Until tests receive stable IDs in source, this matrix identifies them by their exact `ts.test(...)` names. Test numbers are intentionally not treated as stable identifiers.

## Coverage levels

- **DIRECT** — the named default-running regressions, collectively, explicitly assert every material normative clause of the referenced contract/property that the row claims to cover; the default-running regression suite would fail if that property regressed. A fixture merely containing a value is not an assertion; if only a subset of clauses is protected, the class is **PARTIAL**. Wording such as "represented in the fixture" or "boundary X is asserted" for one clause cannot justify `DIRECT` over a larger contract whose remaining clauses are unasserted.
- **PARTIAL** — the test asserts an important part, but one or more normative clauses remain unprotected.
- **INDIRECT** — the property is exercised only as a side effect of a broader test.
- **MISSING** — no deterministic regression in the current default-running suite directly protects the property.
- **BASELINE** — dataset/interface regression that is intentionally not a general product contract.

A production check in code is **not** counted as regression coverage unless a test drives and asserts it.

---

# Product-contract coverage

| Contract | Coverage | Current regression(s) | Coverage note |
|---|---|---|---|
| `PC-MAND-01` | DIRECT | `RAW DP visits every mandatory exactly once`; `reconstruction preserves every mandatory exactly once in supplied direction` | Synthetic fixtures treat multiple mandatory trails as independent solver/reconstruction obligations. |
| `PC-MAND-02` | DIRECT | same two tests | Exact-once set/order semantics are asserted in both search and reconstruction fixtures. |
| `PC-MAND-03` | DIRECT | `reconstruction preserves every mandatory exactly once in supplied direction` | Forward occurrence is required and reversed mandatory occurrence is rejected. |
| `PC-MAND-04` | DIRECT | `reconstruction preserves every mandatory exactly once in supplied direction` | Canonical mandatory points/elevations win at stitches and occur exactly once. |
| `PC-DEMAND-01` | PARTIAL | `demanding whole-trail classification uses ridden length`; `demanding local 60m and 100m windows are independent` | Whole-trail ridden-length denominator and structural 60 m/100 m independence are asserted directly. Threshold boundaries of all three classification rules (and the sinuosity conditions) are exercised only from the positive side; a relaxation of the decision rule would keep the current fixtures green. |
| `PC-DEMAND-02` | MISSING | — | Current suite has no direct first-demanding-trail assertion. |
| `PC-PROT-01` | PARTIAL | `protected corridor continuous tube geometry invariants`; `corridor safety sampling canonicalizes raw segmentation` | Geometry semantics are protected, but the suite does not drive connector generation proving that both mandatory and avoid GPXs are enforced as protected corridors. |
| `PC-PROT-02` | DIRECT | `protected corridor continuous tube geometry invariants` | Perpendicular and oblique crossings are distinguished from genuine co-travel. |
| `PC-PROT-03` | PARTIAL | `protected corridor continuous tube geometry invariants`; `corridor safety sampling canonicalizes raw segmentation` | Continuous measurement and segmentation invariance are direct; the hard/warning threshold classification is not directly asserted. |
| `PC-SAFE-01` | PARTIAL | `trace attributes fail closed on missing or invalid returned shape` | The `/trace_attributes` response-shape helper surface (`traceResponseShape` / `traceShapeGeometry`) is directly regression-protected: missing, non-string, malformed, empty, degenerate, out-of-range, and non-finite shapes each fail closed naming the trace response, and no valid decode path exists that returns the request `/route` shape. The `fetchTraceAttributes` HTTP-consumption call site itself is not directly driven by a default-running regression. Other fail-closed surfaces (missing/invalid `edges` arrays, unmodelable primary duration) are produced by code but not driven by a default-running regression. |
| `PC-WALL-01` | PARTIAL | `hard wall thresholds are 27pct/30m, 20pct/100m and 180W/90s` | The 27% threshold predicate is asserted at and immediately below the boundary, but only on hand-constructed `WallMetrics`; no default-running test derives the exact 30 m maximum uphill grade from connector geometry. |
| `PC-WALL-02` | PARTIAL | same test | The 20% threshold predicate is asserted at and immediately below the boundary; the exact 100 m maximum grade derivation from geometry is not driven by any test. |
| `PC-WALL-03` | PARTIAL | same test | The 90 s boundary predicate is asserted on hand-constructed streak duration; no default-running test derives a positive above-threshold sustained-power streak from rider physics (only the spike-smoothing negative case in the physics test touches streak derivation). |
| `PC-ROAD-01` | PARTIAL | `road policy separates hard invalid roads from scored primary exposure` | Motorway and trunk are directly asserted; steps/ferry/rail/rail-ferry/impassable are not individually covered. |
| `PC-ROAD-02` | PARTIAL | same test | Finite unprotected-primary exposure is directly shown to remain scored; unmodelable primary exposure fail-closed behavior is not directly asserted. |
| `PC-EVID-01` | MISSING | — | Direction is tested separately, but no current regression proves that qualified real-ride evidence raises effective wall and can hard-reject a connector. |
| `PC-EVID-02` | DIRECT | `real-ride wall evidence is directional` | Forward local evidence matches; reversed reference is rejected. |
| `PC-RIDER-01` | PARTIAL | `audit rider recomputation preserves technical mandatory policy` | Cap application, its binding effect on the fixture, and distinctness from ordinary transfer downhill physics are asserted. The established cap value itself is referenced through the constant, never asserted; value drift that keeps the cap binding would not fail the suite. |
| `PC-SEARCH-01` | PARTIAL | `RAW DP visits every mandatory exactly once` | The named fixture recovers a complete exact-once order from a known optimum. The graph carries only one connector per transition, so the prohibition on beam/top-K/quantized dominance or detour cutoffs is not challenged under label pressure. |
| `PC-SEARCH-02` | PARTIAL | `exact no-horizon rider search + local marginal-drop selector` | No-fixed-horizon behavior is directly protected. The broader admissibility of every baseline-pruning condition is not independently regression-tested. |
| `PC-CLASS-01` | PARTIAL | `wall breakpoint sweep preserves useful wall/order changes` | The fixture contains a 200 s improvement step and an order-change step, but the 180 s threshold is not pinned at its boundary and the two usefulness clauses can mask each other (a lowered threshold keeps the fixture green while the order-change clause becomes dead). Only LOOP mode is exercised. |
| `PC-CLASS-02` | MISSING | — | Current suite does not directly exercise C1/C2/C3 derivation from the union of useful severities or the fail-if-<3 rule. |
| `PC-ENDPOINT-01` | PARTIAL | `endpoint assignment chooses exactly one P2P class` | Two LOOP / one P2P and a selected P2P placement are asserted; total-road and deterministic tie-break semantics are not exhaustively covered. |
| `PC-QUALITY-01` | PARTIAL | `rider product selector preserves guardrails while improving candHard` | Strict `candHard` improvement plus road/spike/max-ascent guardrails are covered. Low/high streak, upward, roughness, warm-up, demanding adjacency, and genuine C2/C3 wall-use clauses are not all directly asserted. |
| `PC-SELECT-01` | PARTIAL | `exact no-horizon rider search + local marginal-drop selector`; `local selector preserves an established elbow under a near-zero comfort tail`; `materiality resolution retains exactly 1.0 s gains and coalesces sub-1.0 s gains`; `production product selection applies the materiality resolution wiring` | The stability clause and the amended materiality clauses are asserted bit-exactly (first point retained, exactly-1.0 s gain retained, sub-1.0 s gain coalesced onto the lower-transfer plateau representative, selector fed by the meaningful frontier), and both production selection entry points (`chooseFinal` and the `riderDp` product-selection output) are driven directly by a regression that fails if the materiality stage is removed from either production path. The "post-search only / must not quantize search, eligibility, dominance, safety, or pre-selection state" clause is protected only indirectly by the unchanged `PC-SEARCH-02`/`PC-QUALITY-01` regressions. |
| `PC-RECON-01` | PARTIAL | `reconstruction preserves every mandatory exactly once in supplied direction` | Mandatory order and canonical stitches are directly protected; preservation of the complete selected connector sequence is not asserted as a separate property. |
| `PC-AUDIT-01` | PARTIAL | `independent final audit rejects inconsistent stored rider metrics` | The real audit path is directly asserted to reject inconsistent stored rider metrics with `rider metrics recomputation mismatch` (consistent control passes that check). The production clause that a hard audit failure prevents successful output is not exercised end-to-end by the default-running suite. |
| `PC-ENDPOINT-02` | MISSING | — | The current suite does not directly assert the 5 m start/finish continuity limits. |
| `PC-GAP-01` | DIRECT | `final GPX gap thresholds are WARN at 100m and FAIL at 250m` | All boundary classes around 100 m and 250 m are asserted. |
| `PC-CLI-01` | PARTIAL | `CLI tests run by default; --no-test skips; --self-test is removed` | Parsing-level default-on and explicit `--no-test` opt-out are asserted. The execution wiring itself (suite actually runs before production routing, and a failing suite prevents production) is not exercised by any default-running regression. |
| `PC-OUT-01` | PARTIAL | `human report and exact five output filenames remain stable` | Exact planner-owned filename set is asserted; a real successful production run creating all five files is not exercised by the synthetic test. |
| `PC-OUT-02` | PARTIAL | same test | C3 P2P human-report endpoint meaning is asserted; the complete LOOP/P2P start/finish semantics for all three outputs are not directly covered. |
| `PC-REPORT-01` | PARTIAL | same test | The report explicitly exposes the planning-time convention as report-only; the exact 3 min + 3 min arithmetic and non-interference with optimization/GPX are not independently asserted. |

---

# Architecture-invariant coverage

The architecture document owns the full statements below. This table records only their executable protection.

| `ARCHITECTURE.md` area | Coverage | Current regression(s) | Coverage note |
|---|---|---|---|
| **Connector representations** — route geometry and edge-walk/index geometry have distinct ownership | MISSING | — | No current test deliberately creates differing route/trace index spaces and proves road logic uses only the trace index space while wall/reconstruction use route geometry. |
| **Protected-corridor safety** — continuous tube clipping must not manufacture false overlap | DIRECT | `protected corridor continuous tube geometry invariants` | Includes the historical empty-intersection regression plus crossing/co-travel behavior. |
| **Protected-corridor safety** — result is independent of incidental raw route segmentation | DIRECT | `corridor safety sampling canonicalizes raw segmentation` | Sparse and already-dense equivalent routes produce the same safety result. |
| **Connector graph** — semantic dedupe is bit-exact, not rounded | DIRECT | `semantic connector duplicate is bit exact` | Nearby-but-different elevation remains semantically distinct. |
| **Connector graph** — dominance may collapse only variants interchangeable in continuity-sensitive components | DIRECT | `connector continuation retention keeps ascent-distinct alternatives in the pruned graph` | Causal fixture: a connector strictly better in normal transfer still cannot collapse an ascent-distinct alternative (bit-exact admissibility gate; disabling the gate prunes the alternative and fails this regression), while the bit-exact-equal-ascent strictly-worse control variant stays dominated in the `pruneConnectors` output. |
| **Transfer rider physics** — ~30 m grade chunks, short-tail merge, and downhill coasting | DIRECT | `transfer physics uses 30m grade chunks, short-tail merging, and downhill coasting` | The chunk window and the half-window tail-merge boundary are behaviorally necessary: 20 m/40 m-window and merge/bracket-pair worlds produce materially different pinned `duration`/`t120/t140/t160`/`candHard`/`spike`/`streak180.localMax` results; downhill coasting is asserted. |
| **Transfer rider physics** — streaks concatenate across component boundaries | DIRECT | `streak concatenation crosses component boundaries` | Boundary-crossing streak duration is directly asserted. |
| **RAW search** — exact complete mandatory order on the received graph | PARTIAL | `RAW DP visits every mandatory exactly once` | Known synthetic optimum is recovered. The single-connector fixture cannot distinguish exact DP from any heuristic that happens to find the same order; see `PC-SEARCH-01`. |
| **Rider-quality search** — continuation state includes climb history | PARTIAL | `incremental climb-shape update`; `exact no-horizon rider search + local marginal-drop selector` | Incremental climb semantics and no-horizon search are tested, but exact state-group separation by continuation history is not directly challenged. |
| **Post-search selection** — local selector is a product-selection stage, not search pruning | PARTIAL | `exact no-horizon rider search + local marginal-drop selector` | Selector behavior and distant no-horizon candidate survival are protected; stage-separation itself is not directly asserted. |
| **Post-search selection** — 1.0 s candHard materiality resolution stage between the exact frontier and the local marginal-drop selector | DIRECT | `local selector preserves an established elbow under a near-zero comfort tail`; `materiality resolution retains exactly 1.0 s gains and coalesces sub-1.0 s gains`; `production product selection applies the materiality resolution wiring` | The staged pipeline (exact frontier → materiality resolution → selector) and its boundaries are asserted directly, including through both production selection entry points driven end-to-end; removing the materiality stage from a production path fails the wiring regression. |
| **Reconstruction** — canonical mandatory geometry/elevation wins at stitch boundaries | DIRECT | `reconstruction preserves every mandatory exactly once in supplied direction` | Connector boundary elevations are deliberately different from canonical mandatory elevations. |
| **Independent final audit** — mandatory technical rider physics is recomputed independently | DIRECT | `audit rider recomputation preserves technical mandatory policy` | Audit helper must reproduce the technical downhill policy independently from stored route metrics. |
| **Independent final audit** — inconsistent stored rider metrics are rejected through the real audit path | DIRECT | `independent final audit rejects inconsistent stored rider metrics` | `audit` itself (not the recomputation helper) yields `rider metrics recomputation mismatch` for a deliberately inconsistent stored terminal, with a consistent control fixture asserted not to trip the check. |
| **Connector representations** — `EdgeAttr` indices belong to their trace geometry | MISSING | — | Production checks correspondence, but no current regression protects against using those indices with the wrong geometry representation. |

---

# Baseline and interface regressions

These tests intentionally protect the current canonical fixture or interface surface rather than a general `PC-*` requirement.

| Regression | Type | What it protects |
|---|---|---|
| `canonical input counts, NFC identities and demanding set` | BASELINE | Current canonical dataset shape: 10 mandatory, 10 avoid, 4 real; NFC mandatory identities; expected demanding set (`Feuerlöscher`, `LittleWhistlerB`). |
| `CLI tests run by default; --no-test skips; --self-test is removed` | BASELINE + contract | In addition to `PC-CLI-01`, preserves the current removal of the historical `--self-test` interface. |

Dataset-specific facts in this section must not be promoted into `PRODUCT_CONTRACT.md` unless they become genuine product requirements.

---

# Coverage gaps

This section is diagnostic. `MISSING` or `PARTIAL` does **not** automatically mean “add a new test”. Follow `../.agents/skills/invariant-promotion/SKILL.md`: first decide whether the property is a permanent invariant, then prefer extending an existing contract test when that is the same architectural contract.

## Missing direct product-contract protection

Highest-value current gaps:

1. **`PC-EVID-01`** — safety-active evidence promotion to hard rejection.
2. **`PC-DEMAND-02`** — first-demanding-trail protection.
3. **`PC-CLASS-02`** — C1/C2/C3 derivation and fail-if-fewer-than-three behavior.
4. **`PC-ENDPOINT-02`** — reconstructed start/finish continuity boundaries.

## Missing direct architecture protection

1. **Route geometry vs trace/index geometry ownership.**
2. **Edge index correspondence must never be interpreted in the route-geometry index space.**

These are high-risk because `ARCHITECTURE.md` identifies representation mixing as a major coupling hazard.

## Important partial coverage

- `PC-SAFE-01` — the `/trace_attributes` response-shape helper surface fail-closed behavior is directly regression-protected; the `fetchTraceAttributes` HTTP-consumption call site and the remaining fail-closed surfaces (missing/invalid `edges` arrays, unmodelable primary duration) are not driven by the default suite.
- `PC-AUDIT-01` — audit rejection of inconsistent stored rider metrics is directly asserted; end-to-end prevention of successful production output by a hard audit failure is not.
- `PC-SELECT-01` — materiality boundaries and both production selection entry points are directly asserted (including the production wiring regression); the "post-search only / no pre-selection quantization" scope clause is protected only indirectly by the unchanged search/eligibility regressions.
- `PC-DEMAND-01` — classification threshold boundaries exercised only from the positive side.
- `PC-WALL-01` / `PC-WALL-02` / `PC-WALL-03` — exact 30 m / 100 m grade derivation from connector geometry, and positive sustained-power streak derivation, behind the pinned threshold predicates.
- `PC-CLASS-01` — 180 s improvement threshold not pinned at its boundary; clause masking; LOOP mode only.
- `PC-SEARCH-01` — exactness under competing connector variants / label pressure (also the matching architecture row).
- `PC-RIDER-01` — exact technical descent cap value.
- `PC-CLI-01` — suite-before-production execution wiring and failure gate.
- `PC-PROT-01` / `PC-PROT-03` — full connector-generation enforcement and the hard/warning threshold classification.
- `PC-ROAD-01` / `PC-ROAD-02` — all forbidden classes and unmodelable-primary fail-closed behavior.
- `PC-SEARCH-02` — admissibility of every baseline-pruning guard.
- `PC-ENDPOINT-01` — road/tie-break portions of assignment semantics.
- `PC-QUALITY-01` — complete guarded-resource set and genuine C2/C3 wall requirement.
- `PC-RECON-01` — full selected connector-sequence preservation.
- `PC-OUT-01` / `PC-OUT-02` / `PC-REPORT-01` — end-to-end output creation and complete reporting/endpoint semantics.

---

# Maintenance rule

This matrix and the executable test suite are two views of the same protection system.

When a test is added, removed, renamed, or materially changes what it proves, update this file in the same change.

When a `PC-*` requirement or architecture invariant is added, removed, or materially changed, review this matrix in the same change and explicitly classify its coverage.

See `../.specify/memory/constitution.md` for repository governance and change discipline.
