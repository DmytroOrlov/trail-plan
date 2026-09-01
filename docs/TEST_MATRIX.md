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
- test/change discipline → `../AGENTS.md`.

Until tests receive stable IDs in source, this matrix identifies them by their exact `ts.test(...)` names. Test numbers are intentionally not treated as stable identifiers.

## Coverage levels

- **DIRECT** — the current test explicitly asserts the contract/property itself; the default-running regression suite would fail if that property regressed.
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
| `PC-DEMAND-01` | DIRECT | `demanding whole-trail classification uses ridden length`; `demanding local 60m and 100m windows are independent` | Whole-trail denominator and independent 60 m / 100 m semantics are asserted. |
| `PC-DEMAND-02` | MISSING | — | Current suite has no direct first-demanding-trail assertion. |
| `PC-PROT-01` | PARTIAL | `protected corridor continuous tube geometry invariants`; `corridor safety sampling canonicalizes raw segmentation` | Geometry semantics are protected, but the suite does not drive connector generation proving that both mandatory and avoid GPXs are enforced as protected corridors. |
| `PC-PROT-02` | DIRECT | `protected corridor continuous tube geometry invariants` | Perpendicular and oblique crossings are distinguished from genuine co-travel. |
| `PC-PROT-03` | PARTIAL | `protected corridor continuous tube geometry invariants`; `corridor safety sampling canonicalizes raw segmentation` | Continuous measurement and segmentation invariance are direct; the hard/warning threshold classification is not directly asserted. |
| `PC-SAFE-01` | MISSING | — | No current test systematically injects missing/invalid/non-finite safety evidence and asserts fail-closed behavior. |
| `PC-WALL-01` | DIRECT | `hard wall thresholds are 27pct/30m, 20pct/100m and 180W/90s` | 27% boundary and immediately-below case are asserted. |
| `PC-WALL-02` | DIRECT | same test | 20% boundary and immediately-below case are asserted. |
| `PC-WALL-03` | DIRECT | same test | 90 s boundary and immediately-below case are asserted. |
| `PC-ROAD-01` | PARTIAL | `road policy separates hard invalid roads from scored primary exposure` | Motorway and trunk are directly asserted; steps/ferry/rail/rail-ferry/impassable are not individually covered. |
| `PC-ROAD-02` | PARTIAL | same test | Finite unprotected-primary exposure is directly shown to remain scored; unmodelable primary exposure fail-closed behavior is not directly asserted. |
| `PC-EVID-01` | MISSING | — | Direction is tested separately, but no current regression proves that qualified real-ride evidence raises effective wall and can hard-reject a connector. |
| `PC-EVID-02` | DIRECT | `real-ride wall evidence is directional` | Forward local evidence matches; reversed reference is rejected. |
| `PC-RIDER-01` | DIRECT | `audit rider recomputation preserves technical mandatory policy` | Fixture distinguishes technical mandatory downhill physics from ordinary transfer physics and checks audit recomputation uses the technical policy. |
| `PC-SEARCH-01` | DIRECT | `RAW DP visits every mandatory exactly once` | The synthetic graph has a known complete optimum and RAW DP must recover the complete exact order. |
| `PC-SEARCH-02` | PARTIAL | `exact no-horizon rider search + local marginal-drop selector` | No-fixed-horizon behavior is directly protected. The broader admissibility of every baseline-pruning condition is not independently regression-tested. |
| `PC-CLASS-01` | DIRECT | `wall breakpoint sweep preserves useful wall/order changes` | Both >=180 s transfer improvement and order-change usefulness are represented in the fixture. |
| `PC-CLASS-02` | MISSING | — | Current suite does not directly exercise C1/C2/C3 derivation from the union of useful severities or the fail-if-<3 rule. |
| `PC-ENDPOINT-01` | PARTIAL | `endpoint assignment chooses exactly one P2P class` | Two LOOP / one P2P and a selected P2P placement are asserted; total-road and deterministic tie-break semantics are not exhaustively covered. |
| `PC-QUALITY-01` | PARTIAL | `rider product selector preserves guardrails while improving candHard` | Strict `candHard` improvement plus road/spike/max-ascent guardrails are covered. Low/high streak, upward, roughness, warm-up, demanding adjacency, and genuine C2/C3 wall-use clauses are not all directly asserted. |
| `PC-SELECT-01` | DIRECT | `exact no-horizon rider search + local marginal-drop selector` | Reproduces the old global-tail instability and proves the local elbow remains unchanged under the specified far low-benefit extension. |
| `PC-RECON-01` | PARTIAL | `reconstruction preserves every mandatory exactly once in supplied direction` | Mandatory order and canonical stitches are directly protected; preservation of the complete selected connector sequence is not asserted as a separate property. |
| `PC-AUDIT-01` | MISSING | — | Audit recomputation pieces are tested, but no regression drives a hard final-audit failure through production and proves successful output is prevented. |
| `PC-ENDPOINT-02` | MISSING | — | The current suite does not directly assert the 5 m start/finish continuity limits. |
| `PC-GAP-01` | DIRECT | `final GPX gap thresholds are WARN at 100m and FAIL at 250m` | All boundary classes around 100 m and 250 m are asserted. |
| `PC-CLI-01` | DIRECT | `CLI tests run by default; --no-test skips; --self-test is removed` | Default tests-on behavior and explicit `--no-test` opt-out are asserted. |
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
| **Transfer rider physics** — ~30 m grade chunks and downhill coasting | DIRECT | `transfer physics uses 30m grade chunks and downhill coasting` | Both chunk smoothing behavior and downhill coasting are asserted. |
| **Transfer rider physics** — streaks concatenate across component boundaries | DIRECT | `streak concatenation crosses component boundaries` | Boundary-crossing streak duration is directly asserted. |
| **RAW search** — exact complete mandatory order on the received graph | DIRECT | `RAW DP visits every mandatory exactly once` | Known synthetic optimum is recovered. |
| **Rider-quality search** — continuation state includes climb history | PARTIAL | `incremental climb-shape update`; `exact no-horizon rider search + local marginal-drop selector` | Incremental climb semantics and no-horizon search are tested, but exact state-group separation by continuation history is not directly challenged. |
| **Post-search selection** — local selector is a product-selection stage, not search pruning | PARTIAL | `exact no-horizon rider search + local marginal-drop selector` | Selector behavior and distant no-horizon candidate survival are protected; stage-separation itself is not directly asserted. |
| **Reconstruction** — canonical mandatory geometry/elevation wins at stitch boundaries | DIRECT | `reconstruction preserves every mandatory exactly once in supplied direction` | Connector boundary elevations are deliberately different from canonical mandatory elevations. |
| **Independent final audit** — mandatory technical rider physics is recomputed independently | DIRECT | `audit rider recomputation preserves technical mandatory policy` | Audit helper must reproduce the technical downhill policy independently from stored route metrics. |
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

This section is diagnostic. `MISSING` or `PARTIAL` does **not** automatically mean “add a new test”. Follow `AGENTS.md`: first decide whether the property is a permanent invariant, then prefer extending an existing contract test when that is the same architectural contract.

## Missing direct product-contract protection

Highest-value current gaps:

1. **`PC-SAFE-01`** — explicit fail-closed regression for unavailable/invalid safety evidence.
2. **`PC-EVID-01`** — safety-active evidence promotion to hard rejection.
3. **`PC-AUDIT-01`** — end-to-end hard-audit rejection of production output.
4. **`PC-DEMAND-02`** — first-demanding-trail protection.
5. **`PC-CLASS-02`** — C1/C2/C3 derivation and fail-if-fewer-than-three behavior.
6. **`PC-ENDPOINT-02`** — reconstructed start/finish continuity boundaries.

## Missing direct architecture protection

1. **Route geometry vs trace/index geometry ownership.**
2. **Edge index correspondence must never be interpreted in the route-geometry index space.**

These are high-risk because `ARCHITECTURE.md` identifies representation mixing as a major coupling hazard.

## Important partial coverage

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

See `../AGENTS.md` for repository change discipline.
