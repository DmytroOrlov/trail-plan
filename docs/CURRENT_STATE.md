# CURRENT_STATE.md

# Trail Plan — Current State

This file describes the **currently established state** of the project.

It is not a history of experiments and not a replacement for tests or ADRs.

When this file conflicts with an explicit current user requirement, the user requirement wins.

---

## Production shape

The current production pipeline, Valhalla boundary, representation ownership, and independent final-audit boundary are established; see `docs/ARCHITECTURE.md`.

---

## Established product semantics

The following are established and must not be changed accidentally:

* Every supplied mandatory technical GPX is independently mandatory.
* Every mandatory technical GPX must be traversed exactly once.
* Every mandatory technical GPX must be traversed in its supplied direction.
* Mandatory trails are not interchangeable.
* `family` / `family_id` semantics are not part of the product model.
* Mandatory and avoid GPXs are protected from reuse as ordinary transfer corridors.
* Routing failure must not be solved by silently weakening established safety or product contracts.

Detailed normative requirements belong in `PRODUCT_CONTRACT.md`.

---

## Established search semantics

* RAW ordering is exact on the connector graph it receives.
* Rider ordering remains exact on the graph/state space defined by the production algorithm.
* Approximate search mechanisms are not part of the current architecture.
* Fixed arbitrary search horizons, beam search, top-K pruning, epsilon pruning, or equivalent shortcuts must not be introduced without a product-level decision.

Search performance improvements must preserve the problem being solved.

---

## Established canonical-data semantics

Canonical data/geometry ownership, distinct route/trace index spaces, and canonical protected-corridor safety geometry are established; see `docs/ARCHITECTURE.md`.

---

## Established safety behavior

The following safety mechanisms are currently part of production behavior:

* protected-corridor exclusion;
* wall constraints;
* road-safety classification;
* real-ride evidence;
* required elevation/data inputs fail closed where absence would invalidate safety reasoning;
* final route auditing is independent from the search decision that produced the route.

Do not weaken these mechanisms merely to make a candidate route feasible.

Exact thresholds and detailed semantics belong in code, tests, `PRODUCT_CONTRACT.md`, or `docs/ARCHITECTURE.md` rather than this snapshot.

---

## Established selection behavior

The current production selector uses a **local marginal-drop** criterion.

The previously used global-extrema-normalized knee was removed because adding an irrelevant distant alternative could change the selected result.

Selection should remain stable under irrelevant extension of the alternative set where that property is covered by the regression suite.

---

## Established engineering discipline

A reproduced defect that establishes a permanent product or architecture invariant must result in regression protection.

The user does not need to separately request such a test.

The expected loop is:

`reproduce → establish cause → identify invariant → add regression → make minimal fix → verify`

Temporary diagnostics and experimental production logic should be removed once the investigation is concluded.

A single successful route is evidence, not proof of correctness.

---

# Closed / rejected directions

The following ideas have already been investigated or rejected and must not be reintroduced without new evidence.

## Family-based mandatory semantics

Rejected.

Mandatory GPXs are independent requirements and are not grouped into interchangeable families.

---

## Approximate ordering/search

Rejected for the current product contract.

Do not replace exact DP/search with beam search, top-K, epsilon dominance, arbitrary detour cutoffs, or equivalent approximations.

---

## Fixed rider-search horizon

Rejected as a general production shortcut.

Do not impose a fixed horizon that can remove an otherwise valid exact solution.

---

## Cross-profile failure pruning

A transition being unusable for one profile is not established evidence that it may be skipped for another profile.

Do not introduce cross-profile pruning from such an assumption without new proof.

---

## Speculative profile/candidate expansion

Additional profiles or candidates are not automatically beneficial.

Temporary candidate expansions used for experiments must not become production architecture without demonstrated product value.

---

## Global-extrema-normalized selector knee

Rejected.

It allowed irrelevant distant alternatives to alter the selected result.

The current local marginal-drop selector replaced it.

---

## Runtime topology guesses

Prefer canonical GPX/data contracts over runtime topology inference when the required fact is already represented by canonical input data.

Do not reintroduce topology heuristics merely to reconstruct information already owned by the input contract.

---

# Open

No architectural change should be treated as open merely because an alternative design exists.

Add an item here only when there is an actual unresolved product or engineering question with current evidence on both sides.

At present, no known open item overrides the established contracts above.

---

# What may change normally

The architecture is not frozen at implementation-detail level.

The following may change when behavior is preserved:

* local function structure;
* naming;
* internal data structures;
* performance implementation;
* diagnostics;
* code organization inside the single Scala script;
* removal of duplication;
* clearer representations with equivalent semantics.

Such changes must preserve established product, safety, canonical-data, and exact-search contracts.

---

# Reopening an established decision

An established or rejected decision should be reconsidered only when at least one of these is true:

1. the product requirement changed;
2. a reproducible counterexample proves the current behavior wrong;
3. an assumption behind the decision was proven false;
4. a new invariant cannot be satisfied by the current architecture.

A different design being cleaner, more generic, or more elegant is not sufficient by itself.

When an established decision changes, update this file so it continues to describe only the current truth.
