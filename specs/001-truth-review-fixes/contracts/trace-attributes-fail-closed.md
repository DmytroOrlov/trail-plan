# Contract: `/trace_attributes` Shape Fail-Closed Handling

**Status**: change-scoped testable restatement; canonical owner is `PC-SAFE-01`
(`docs/PRODUCT_CONTRACT.md`) plus the route-vs-trace index-space ownership in
`docs/ARCHITECTURE.md`. This file defines nothing new and must not diverge.

## Interface

Valhalla HTTP `POST /trace_attributes` as consumed by
`fetchTraceAttributes` in `trail-plan.scala`.

## Guarantees

1. A successful HTTP response (2xx) is accepted only when its own body carries
   a valid trace shape:
   - `shape` key present, string-typed, non-empty;
   - decodes (polyline6) to ≥ 2 points;
   - all decoded coordinates finite and within lat ∈ [-90, 90],
     lon ∈ [-180, 180].
2. On any violation in (1), the result is `Left` (fail closed) naming the trace
   response defect. The request `/route` shape is never used as a substitute.
3. A missing/untyped `edges` array continues to fail closed as today.
4. Non-2xx responses continue to fail closed as service failures, distinct from
   legitimate `/route` no-route handling.

## Verification

Default-suite regression: "trace attributes fail closed on missing or invalid
returned shape" covering missing, malformed, non-finite, out-of-range, and
degenerate cases, each asserting rejection and no route-shape inheritance
(Acceptance Scenario 1).
