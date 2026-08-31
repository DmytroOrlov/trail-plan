# AGENTS.md

# Trail Plan

Production code lives in one file:

`trail-plan.scala`

Keep it that way unless the user explicitly changes this constraint.

## Before changing code

Read, when present:

1. `docs/CURRENT_STATE.md`
2. `docs/ARCHITECTURE.md`
3. `docs/PRODUCT_CONTRACT.md`
4. relevant ADRs
5. relevant tests

Current repository contracts override historical comments and old experiments.

## Change rules

* Make the smallest change that solves the established problem.
* Do not invent new domain semantics or architecture without evidence.
* Preserve the established product, safety, search, and canonical-data semantics recorded in `docs/CURRENT_STATE.md`.
* Prefer removing a disproven heuristic over compensating for it with another heuristic.

For a bug:

1. reproduce it;
2. identify the cause;
3. determine whether it reveals a permanent invariant;
4. if yes, add a regression test;
5. make the minimal production change;
6. run relevant tests and inspect resulting evidence;
7. remove temporary diagnostics/experiments.

A successful single route is not proof of correctness.

## Architecture changes

Do not reopen an established architectural decision merely because another design looks cleaner.

Reconsider it only when:

* the product requirement changed;
* a reproducible counterexample shows it is wrong; or
* an assumption behind it was proven false.

Record established current truth in repository documentation, not only in chat or source-history comments.

## Review

Treat the planner as unfamiliar production/safety-sensitive routing code.

Derive behavior from code, tests, input contracts, and run evidence. Do not try to confirm author intent.

Pay particular attention to:

* silent contract weakening;
* hidden approximation;
* fail-open behavior;
* confusion between different geometry/index spaces;
* DP/reconstruction disagreement;
* behavior that changes because of irrelevant alternatives or segmentation.

## Done

A change is done when:

* the requested problem is addressed;
* relevant contracts still hold;
* every newly established invariant has regression protection;
* relevant tests pass;
* temporary investigation code is removed;
* documentation reflects any changed established truth.

Compilation alone is not sufficient.
