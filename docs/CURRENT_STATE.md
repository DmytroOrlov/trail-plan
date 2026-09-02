# CURRENT_STATE.md

# Trail Plan — Current State

This file describes the **currently established state** of the project.

It is not a history of experiments and not a replacement for tests or ADRs.

When this file conflicts with an explicit current user requirement, the user requirement wins.

---

## Production shape

The single-file production shape is established; see System shape in `docs/ARCHITECTURE.md`.

The current production pipeline, Valhalla boundary, representation ownership, and independent final-audit boundary are established; see `docs/ARCHITECTURE.md`.

---

## Established product semantics

The mandatory-trail, demanding-trail, and protected-corridor contracts are established; see `PC-MAND-*`, `PC-DEMAND-*`, and `PC-PROT-*` in `PRODUCT_CONTRACT.md`.

`family` / `family_id` semantics are not part of the product model; see Family-based mandatory semantics under Closed / rejected directions.

---

## Established search semantics

The exact RAW and rider-quality search contracts are established; see `PC-SEARCH-01` and `PC-SEARCH-02` in `PRODUCT_CONTRACT.md`. Approximate search mechanisms remain rejected; see Approximate ordering/search and Fixed rider-search horizon under Closed / rejected directions.

---

## Established canonical-data semantics

Canonical data/geometry ownership, distinct route/trace index spaces, and canonical protected-corridor safety geometry are established; see `docs/ARCHITECTURE.md`.

---

## Established safety behavior

The hard-safety, road-policy, and real-ride-evidence contracts are established; see `PC-SAFE-*`, `PC-WALL-*`, `PC-ROAD-*`, and `PC-EVID-*` in `PRODUCT_CONTRACT.md`.

The independent final-audit boundary is established; see `docs/ARCHITECTURE.md`.

---

## Established selection behavior

The current production selector is established; see Post-search selection in `docs/ARCHITECTURE.md`. The selector-stability requirement is defined by `PC-SELECT-01` in `PRODUCT_CONTRACT.md`. The global-extrema-normalized knee is rejected; see Global-extrema-normalized selector knee under Closed / rejected directions.

---

## Established engineering discipline

Governance, change discipline, and the definition of done (including the bug/regression workflow) are owned by `.specify/memory/constitution.md`. `AGENTS.md` is only the repository agent entrypoint that routes to the constitution and the canonical owners.

---

# Closed / rejected directions

The following ideas have already been investigated or rejected and are not part of the current design.

Reopening any of them is governed by `Reopening an established decision`.

## Family-based mandatory semantics

Rejected.

Mandatory GPXs are not grouped into interchangeable families; the independence requirement is owned by `PC-MAND-01`.

---

## Approximate ordering/search

Rejected for the current product contract.

The exactness requirements and prohibited approximation classes are defined by `PC-SEARCH-01` and `PC-SEARCH-02`.

---

## Fixed rider-search horizon

Rejected as a general production shortcut.

The no-fixed-horizon guarantee is defined by `PC-SEARCH-02`.

---

## Cross-profile failure pruning

A transition being unusable for one profile is not established evidence that it may be skipped for another profile.

Do not introduce cross-profile pruning from such an assumption.

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
