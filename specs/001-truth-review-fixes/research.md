# Research: Current Truth Review Fixes

## Established before implementation

- `fetchTraceAttributes` substitutes the request `/route` shape when the
  response omits `shape` (`case _ => Right(shape)`); this contradicts
  `PC-SAFE-01` and the canonical trace-index ownership boundary.
- The current strongest-local-drop selector (`selectLocalMarginalDrop`) chooses
  `elbow` for the four-point frontier but chooses `tail1` after appending
  `(1000,58.999999)`.
- The existing transfer-physics test proves smoothing and coasting but not the
  specific ~30 m chunk/tail behavior.
- The existing audit test exercises the recomputation helper but not rejection
  through `audit`.
- `connectorDominates` requires bit-exact equal connector ascent before comparing
  monotone resources; `pruneConnectors` runs before RAW/rider search, so a
  locally dominated connector can remove continuation-distinct downstream
  alternatives.
- `specify workflow list`, `info`, and `resolve` show installed reachable workflow
  `speckit` (“Full SDD Cycle”) with steps `specify`, `review-spec`, `plan`,
  `review-plan`, `tasks`, `implement`.
  Its base is marked `source: bundled`; `specify workflow resolve speckit`
  currently attributes every step to `[base]` with no project layer present.

## Baseline

The pre-change default suite passed 22/22 using the canonical input directory.
Production then stopped at the deliberately unavailable Valhalla `/status`.
After adding the reproduced selector counterexample regression (T003), the
default suite fails on that test, which is the intended pre-fix state.

## Resolved gate — `PC-SELECT-01` materiality rule

**Decision**: Adopt the 1.0-second `candHard` materiality resolution exactly as
stated in `spec.md` §Product Decision / FR-003 and canonicalized in
`PC-SELECT-01` (`docs/PRODUCT_CONTRACT.md`). The text is not restated here; the
canonical owner governs.

**Rationale**: The user adopted this as an explicit product decision
(`spec.md` §Product Decision, 2026-09-03 clarifications), and it is now the
canonical text of `PC-SELECT-01`. It preserves exact search while preventing
floating-point-scale comfort gains from manufacturing a stronger product elbow.
It resolves the reproduced counterexample in both the base and extended frontier
forms, and the boundary behavior (exactly 1.0 s retained, sub-1.0 s coalesced)
is directly testable.

**Alternatives considered**:

- Absolute-gain tolerance on marginal slope — rejected: a materially different
  policy that would produce different results on other frontiers; not adopted by
  the product decision.
- Relative-slope tolerance — rejected: same invents-semantics problem, and
  scale-dependent.
- Leaving the selector ambiguous and stopping the change — superseded: the gate
  required human authority, which has now been given.

## Boundary regression requirement

The clarification session requires a deterministic boundary regression, not just
the counterexample: a frontier step improving `candHard` by exactly 1.0 s is
retained as a new meaningful point; a step just below 1.0 s coalesces into the
same plateau. Boundary fixtures must avoid representations where floating-point
rounding could blur the `>= 1.0 s` comparison; the regression must remain stable
under full-precision `Double` arithmetic.

## Overlay mechanism (`FR-007`)

**Decision**: Add a project-local overlay for the `speckit` workflow via
`specify workflow overlay`, extending the step graph through converge,
independent review, and executable-regression/traceability verification per the
constitution's lifecycle. Verify with `specify workflow overlay list` and
`specify workflow resolve speckit` layer attribution.

**Rationale**: `resolve` proves the bundled base is reachable but stops at
`implement`; the overlay is the supported repository-owned customization layer
and leaves bundled generated material untouched, as required by the 2026-09-03
clarification.

**Alternatives considered**: editing `.specify/workflows/speckit/workflow.yml`
directly (rejected: bundled material is not repository-owned and would be
overwritten by updates); declaring the lifecycle satisfied by manual discipline
without a repository-owned definition (rejected: leaves `workflow list/info`
presenting an incomplete lifecycle as constitutionally complete).
