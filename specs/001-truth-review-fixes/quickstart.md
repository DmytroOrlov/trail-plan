# Quickstart: Validating Current Truth Review Fixes

## Prerequisites

- Java 21, Scala 3.3.7 toolchain usable for the shebang runner (`instant-scala`)
  or an equivalent `scala-cli`/`scala` invocation.
- Canonical input directory: `trails/` in this repository.
- Valhalla is **not** required to validate the regression work in this change;
  the default suite runs before production routing (`PC-CLI-01`) and production
  stops at the Valhalla `/status` boundary when the service is unavailable.

## Run the default regression suite

```bash
./trail-plan.scala --input trails --output /tmp/trail-plan-quickstart --valhalla-url http://localhost:8002
```

Note: `--valhalla-url` is required by the CLI in all modes (including the
default suite run); Valhalla itself need not be reachable — the run executes
the suite first and then stops at the Valhalla `/status` boundary.

Expected:

- `Build id` header and the full `ts.test(...)` suite report.
- Pre-fix baseline was 22/22; after this change all pre-existing tests plus the
  new/renamed regressions below pass, and the run then reaches (and stops at)
  the Valhalla status check with no Valhalla service.

## Targeted validation scenarios

| Scenario (spec.md) | What to observe in suite output |
|---|---|
| AS1 trace fail-closed | test "trace attributes fail closed on missing or invalid returned shape" passes; rejection messages name the trace response defect, never route-shape reuse |
| AS2 selector counterexample + boundary | "local selector preserves an established elbow under a near-zero comfort tail" passes for both base and extended frontiers; materiality boundary test shows exactly-1.0 s gain retained, sub-1.0 s coalesced |
| AS3 transfer physics | strengthened chunk/tail test fails if ~30 m chunking or short-tail merging is materially changed |
| AS4 real audit | audit-path regression yields `rider metrics recomputation mismatch` for the inconsistent stored metrics |
| AS5 connector continuation | continuation-regression passes with pruning active; the continuation-distinct alternative appears in the `pruneConnectors` output handed to search |

Details of the asserted structures and fixtures: `data-model.md`; stage and
response contracts: `contracts/`.

## Workflow overlay check (AS6)

```bash
specify workflow info speckit
specify workflow resolve speckit
```

Expected after this change: layer attribution includes a project-local overlay
on top of the bundled base, and `info`/`resolve` show the step graph extending
beyond `implement` to converge, independent review, and executable-regression /
traceability verification. Before the overlay lands, `resolve` shows `[base]`
only and stops at `implement` (current reproduced state).

## Completion gate

Full default suite passes (Acceptance Scenario 7). With a reachable Valhalla,
additionally run the canonical production comparison and confirm the five
`PC-OUT-01` outputs; this is baseline evidence, not a substitute for the suite.
