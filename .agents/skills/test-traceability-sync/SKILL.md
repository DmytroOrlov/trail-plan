---
name: test-traceability-sync
description: Keep Trail Plan regression tests and docs/TEST_MATRIX.md synchronized bidirectionally with PRODUCT_CONTRACT.md and ARCHITECTURE.md, without inventing coverage or changing semantics.
---

# Test Traceability Sync

Use this skill whenever a change can alter executable regression traceability.

This includes changes to:

- `trail-plan.scala` tests (`ts.test(...)`);
- fixture/helper functions that `ts.test(...)` bodies depend on
  (synthetic builders, shared test data, helper predicates);
  a change does not need to touch the literal `ts.test(...)` block
  to invalidate traceability;
- assertions inside an existing test;
- test names;
- test removal;
- `docs/TEST_MATRIX.md`;
- `docs/PRODUCT_CONTRACT.md` `PC-*` requirements;
- architecture invariants referenced by `docs/TEST_MATRIX.md`;
- default test execution behavior.

The purpose of this skill is **traceability consistency**, not product design.

## Canonical owners

Respect the repository ownership model:

- `.specify/memory/constitution.md` — repository governance and change-process authority;
- `docs/CURRENT_STATE.md` — established / rejected / open status;
- `docs/PRODUCT_CONTRACT.md` — what the product must guarantee;
- `docs/ARCHITECTURE.md` — how the current implementation works;
- `docs/TEST_MATRIX.md` — which executable regression protects which contract/invariant;
- `trail-plan.scala` — executable production code and regression suite.

`AGENTS.md` is a thin repository entrypoint/router, not a canonical governance owner.

When a Spec Kit change is active, use its `specs/<change>/` artifacts to understand the approved change-scoped intent and implementation work. They do not become permanent current product/architecture owners; durable truth must be synchronized back to the canonical owner required by the constitution.

Never move normative meaning into `TEST_MATRIX.md`.

Never treat `TEST_MATRIX.md` as the owner of a product requirement or architecture invariant.

## Core rule

`docs/TEST_MATRIX.md` and the default-running regression suite are two views of the same protection system.

When either side materially changes, inspect and synchronize the other side in the same change.

Do not wait for a separate user request.

## What counts as a material test change

A test change is material when any of the following happens:

- a `ts.test(...)` is added;
- a test is removed;
- a test is renamed;
- an assertion is added or removed;
- an assertion becomes weaker or stronger;
- fixture construction changes what the test actually proves;
- a test stops running by default;
- a previously indirect property becomes explicitly asserted;
- a test begins or stops protecting a `PC-*` requirement or architecture invariant.

Formatting-only changes are not material.

## Coverage classes

`docs/TEST_MATRIX.md` / `Coverage levels` is the canonical owner of the coverage-class definitions.

Read and apply those definitions before classifying coverage.

Do not redefine or weaken them in this skill.

## Mandatory procedure

### 1. Read canonical sources first

Before changing traceability, read the relevant parts of:

1. `.specify/memory/constitution.md`;
2. the active `specs/<change>/` artifacts, when a Spec Kit change is active;
3. `docs/CURRENT_STATE.md`;
4. `docs/PRODUCT_CONTRACT.md`;
5. `docs/ARCHITECTURE.md`;
6. `docs/TEST_MATRIX.md`;
7. the affected tests in `trail-plan.scala`.

`AGENTS.md` may be used as the repository entrypoint/router, but it does not override the constitution or canonical owners above.

Active Spec Kit artifacts describe the assigned change; do not treat them as permanent current product or architecture truth unless the durable owner is updated through the governing change process.

Do not infer a requirement from the matrix when the canonical owner is available.

### 2. Inventory the current executable suite

`trail-plan.scala` is the only canonical production/test evidence. Workspace or legacy Scala copies (for example `trail-plan-2.scala`) must not participate in test/matrix matching; report them as stale hazards if they could confuse an audit.

Collect every default-running `ts.test(...)` name from the `runSelfTests` suite in `trail-plan.scala`.

- Literal top-level `ts.test(...)` calls in `runSelfTests` are default-running.
- If a test is conditional, wrapped, or name-generated, inspect actual reachability from the default CLI path instead of assuming it runs.
- Exact test names are taken from canonical `trail-plan.scala`.

Treat the exact test name as the current matrix identifier until stable test IDs exist.

Before validating existence or coverage, resolve matrix shorthand (`same test`, `same two tests`) back to the exact names of the referenced anchor row. Shorthand itself does not need rewriting merely for style.

Confirm whether each named test in `docs/TEST_MATRIX.md` actually exists and runs by default.

### 3. Validate product-contract references

For every product-contract row in `docs/TEST_MATRIX.md`:

- confirm the referenced `PC-*` ID exists in `docs/PRODUCT_CONTRACT.md`;
- read the full canonical requirement;
- inspect the named test assertions and fixtures;
- classify coverage from evidence, not intention.

Do not copy the full requirement into the matrix.

### 4. Validate architecture references

For every architecture-invariant row:

- confirm the referenced section/property exists in `docs/ARCHITECTURE.md`;
- read the canonical architecture statement;
- inspect the named test assertions and fixtures;
- ensure the matrix note does not become a second architecture definition.

If a matrix row refers to an architecture property that has no canonical statement in `ARCHITECTURE.md`:

- report the unresolved owner gap;
- do not invent or add the canonical statement from inside this skill;
- require a separate architecture/invariant-promotion change owned by the appropriate canonical document;
- update `docs/CURRENT_STATE.md` only when established/rejected/open status actually changes;
- once that owning change exists, this skill may synchronize the matrix against it.

This skill never originates a new canonical architecture invariant or product requirement merely to satisfy traceability; the same rule applies to `docs/PRODUCT_CONTRACT.md`.

The skill may validate canonical-owner changes that are already part of the assigned repository change, but it must not create them solely because the matrix needs a target.

### 5. Reclassify only when evidence changed

Change `DIRECT`, `PARTIAL`, `INDIRECT`, or `MISSING` only when the executable evidence changed or the canonical requirement changed.

Examples:

- new assertion covers the previously missing final clause → `PARTIAL` may become `DIRECT`;
- assertion removed → `DIRECT` may become `PARTIAL` or `MISSING`;
- production code now checks a condition but tests do not → coverage does **not** improve;
- integration test happens to traverse a property but never asserts it → normally `INDIRECT`, not `DIRECT`.

Never strengthen coverage merely because a test name sounds relevant.

Apply the canonical `DIRECT` rule from `docs/TEST_MATRIX.md` / `Coverage levels`.

### 6. Handle test additions

For each new default-running test:

1. identify what it actually asserts;
2. determine whether that assertion protects:
   - an existing `PC-*`;
   - an existing architecture invariant;
   - a baseline/interface regression;
   - none of the above;
3. update `docs/TEST_MATRIX.md` only where traceability is real.

A new test does not automatically require a new `PC-*`.

A new test does not automatically require a new architecture invariant.

If the test appears to establish a genuinely new permanent invariant, stop treating this as mere traceability sync and follow the repository's invariant-promotion/change discipline instead.

### 7. Handle test removals or renames

If a test is renamed:

- update every exact-name reference in `docs/TEST_MATRIX.md`;
- do not alter coverage classification unless the assertions also changed.

If a test is removed:

- find every matrix row that depended on it;
- recalculate coverage using remaining default-running tests;
- downgrade to `PARTIAL`, `INDIRECT`, or `MISSING` as required.

Never leave stale test names in the matrix.

### 8. Handle `PC-*` changes

`PC-*` requirements are authored by `docs/PRODUCT_CONTRACT.md` and its owning change, never by this skill; this skill only synchronizes the matrix against changes already assigned.

If a `PC-*` requirement is added:

- add/adjust matrix traceability;
- use `MISSING` if no current default-running regression protects it;
- do not fabricate a test.

If a `PC-*` requirement is materially expanded:

- re-evaluate existing `DIRECT` claims;
- downgrade to `PARTIAL` when the test protects only the old subset.

If a `PC-*` requirement is removed or replaced:

- remove/update its matrix row;
- preserve relevant baseline or architecture coverage if it still has another canonical owner.

Do not renumber unrelated stable `PC-*` IDs.

### 9. Handle architecture changes

If an established architecture invariant changes:

- update its canonical statement in `docs/ARCHITECTURE.md` as part of the owning change;
- then re-evaluate the corresponding matrix coverage.

Do not use `TEST_MATRIX.md` to define the new architecture.

### 10. Validate baseline regressions separately

Keep dataset/interface regressions separate from general product contracts.

Examples include:

- canonical input counts;
- canonical trail identities;
- current demanding-set identity;
- removed/retained CLI surface;
- fixture-specific expected results.

Do not promote these into `PRODUCT_CONTRACT.md` unless they genuinely become normative product requirements.

## Bidirectional consistency audit

Before finishing, perform all of these checks.

### Matrix → source

For every row/reference in `docs/TEST_MATRIX.md`:

- `PC-*` exists;
- architecture section/property exists;
- named test exists;
- named test runs by default when claimed as executable coverage;
- coverage note matches actual assertions;
- every `DIRECT` claim satisfies the canonical definitions in `docs/TEST_MATRIX.md` / `Coverage levels`.

### Source → matrix

For every default-running `ts.test(...)`:

- determine whether it protects a product contract;
- determine whether it protects an architecture invariant;
- determine whether it is baseline-only;
- ensure required traceability exists.

Any default-running regression that materially protects an existing `PC-*`, an established architecture invariant, or an intentional baseline/interface regression must be represented in `docs/TEST_MATRIX.md`.

This does not require one matrix row per test: a test may appear as evidence in an existing contract/invariant row.

A test may remain absent from the matrix only when it protects no established product contract, architecture invariant, or intentional baseline/interface regression; state that determination explicitly in the final report.

### Coverage gap audit

Review all `MISSING` and `PARTIAL` entries after the change.

Do not automatically add tests for them.

Only report newly created, closed, or materially altered gaps.

### Pre-existing inconsistencies

Drift discovered outside the assigned change is reported, not silently repaired, unless repairing traceability inconsistency is itself the assigned task. This prevents an unrelated rename or refactor from turning into uncontrolled repository cleanup.

## Editing discipline

Make the smallest human-reviewable diff.

Do not:

- rewrite `TEST_MATRIX.md` for style;
- reorder unrelated rows;
- restate numeric thresholds in matrix notes when a `PC-*` owns them;
- restate architecture mechanics when `ARCHITECTURE.md` owns them;
- create new product semantics;
- create tests solely to make the matrix green;
- convert a production guard into claimed test coverage;
- change `CURRENT_STATE.md` unless the actual established/rejected/open status changed;
- add historical rationale to these five owner documents.

Prefer references over duplicated prose.

## When to escalate beyond this skill

This skill is not sufficient when the change establishes a new permanent invariant.

Examples:

- a reproduced bug reveals a new product guarantee;
- a representation assumption is proven unsafe;
- an architecture decision is disproven;
- a previously accepted approximation is shown incorrect;
- a new rejected direction is established.

In those cases, first classify and promote the new invariant to its canonical owner, add regression protection as required by `.specify/memory/constitution.md` and the invariant-promotion discipline, and then use this skill to synchronize `docs/TEST_MATRIX.md`.

## Completion criteria

Traceability sync is complete only when:

- every changed/renamed/removed relevant test is reflected correctly;
- every changed `PC-*` row has been re-evaluated;
- every changed referenced architecture invariant has been re-evaluated;
- all matrix test names resolve to current default-running tests;
- all matrix `PC-*` references resolve;
- all architecture references resolve;
- no `DIRECT` classification overclaims actual assertions;
- new or changed gaps are explicitly visible as `PARTIAL` or `MISSING`;
- no normative requirement or architecture definition was duplicated into the matrix;
- unrelated documentation and code were not changed.

## Required final report

After applying changes, report concisely:

1. tests added / removed / renamed / materially changed;
2. matrix rows changed and why;
3. coverage classifications changed and the exact evidence for each;
4. new or closed `MISSING` / `PARTIAL` gaps;
5. unresolved references, if any;
6. confirmation that no coverage claim was strengthened without executable evidence;
7. confirmation that canonical owners remain:
   - governance / change process → `.specify/memory/constitution.md`;
   - product semantics → `PRODUCT_CONTRACT.md`;
   - implementation architecture → `ARCHITECTURE.md`;
   - decision status → `CURRENT_STATE.md`;
   - executable traceability → `TEST_MATRIX.md`;
   - `AGENTS.md` → thin repository entrypoint/router only;
   - active `specs/<change>/` artifacts → change-scoped intent/plan/research/tasks only, not permanent current product/architecture ownership.
