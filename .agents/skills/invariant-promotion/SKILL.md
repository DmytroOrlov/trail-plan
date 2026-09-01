---
name: invariant-promotion
description: Classify newly established knowledge from bugs, investigations, regressions, and architecture changes, promote it to the correct Trail Plan canonical owner, require regression protection for newly established permanent product/architecture invariants, and hand off traceability synchronization without inventing semantics.
---

# Invariant Promotion

Use this skill when an investigation, bug fix, regression, review finding, or architectural change establishes new durable knowledge about the Trail Plan system.

This skill answers:

> **What kind of knowledge did we just establish, where is its canonical owner, and what protection must accompany it?**

It is a semantic promotion gate.

It is **not** a generic documentation cleanup skill.
It is **not** a test-matrix sync skill.
It is **not** permission to invent new product or architecture semantics.

## Canonical owners

Respect the repository ownership model:

- `AGENTS.md` — how agents work;
- `docs/CURRENT_STATE.md` — what is established / rejected / open;
- `docs/PRODUCT_CONTRACT.md` — what the product must guarantee independent of implementation;
- `docs/ARCHITECTURE.md` — how the current implementation realizes those guarantees;
- `docs/TEST_MATRIX.md` — what executable regression protects what;
- ADR / evidence history — why a decision was made or what experiments established it;
- `trail-plan.scala` — executable production implementation and regression suite.

One fact should have one canonical textual owner.

Other files should reference that owner rather than restating the same knowledge.

`trail-plan.scala` is the only canonical current production/test evidence.
Legacy or workspace Scala copies (for example `trail-plan-0.5.scala`, `trail-plan-1.scala`, `trail-plan-2.scala`) must not be used as evidence for current behavior, tests, contracts, architecture, or promotion decisions; report them as stale hazards if encountered. Do not edit or delete them from this skill.

## Trigger conditions

Use this skill when any of the following occurs:

- a reproduced defect reveals a permanent product invariant;
- a reproduced defect reveals a permanent architecture invariant;
- a safety assumption is proven false;
- a representation/index-space assumption is proven unsafe;
- an optimization or pruning rule is shown to change exact semantics;
- an established product or architecture decision is disproven;
- a previously accepted approach becomes a rejected direction;
- a new regression test captures a newly established invariant;
- a code review finds behavior that is important and durable but undocumented;
- a source comment or investigation contains rationale/knowledge that would otherwise be lost;
- a product requirement materially changes;
- a canonical contract or architecture invariant must be added, removed, or reclassified.

Do not use this skill for:

- pure refactoring with no semantic change;
- formatting/documentation-only cleanup;
- a test rename with unchanged meaning;
- mechanical `TEST_MATRIX.md` synchronization;
- one-off diagnostic observations;
- dataset-specific values that are not intended as product guarantees;
- speculative ideas not established by code, tests, product requirements, or reproducible evidence.

For pure traceability synchronization, use:

`.agents/skills/test-traceability-sync/SKILL.md`

## Evidence standard

Do not promote a new invariant merely because:

- a single route happened to succeed;
- a comment claims it;
- a test name sounds like it;
- an implementation detail exists;
- a cleaner design would prefer it;
- a model infers author intent;
- an experiment produced one convenient outcome.

Promotion requires an established basis such as:

- explicit current user/product requirement;
- reproducible counterexample;
- deterministic regression reproducing the defect;
- code/data contract that necessarily establishes the behavior;
- independently reviewed production behavior;
- evidence that an existing assumption is false.

A successful single route is evidence, not proof of correctness.

When evidence is insufficient, do not invent a canonical invariant.
Report the observation as unresolved instead.

## Classification procedure

For each newly established fact, classify it before editing canonical documents.

Use the following decision order.

### 1. Product contract

Ask:

> Must this remain true even if the implementation is completely rewritten?

If yes, it is a candidate for `docs/PRODUCT_CONTRACT.md`.

Typical examples:

- every supplied mandatory trail is independently required;
- every mandatory trail occurs exactly once;
- required direction must be preserved;
- a hard safety condition must fail closed;
- search must remain exact;
- a final hard audit failure prevents successful output;
- a selector must remain stable under a defined irrelevant extension.

Do not put implementation mechanics here.

Do not add:

- Scala function/type/field names;
- Valhalla endpoint mechanics;
- route/trace index ownership;
- current profile sets;
- Build IDs;
- dataset counts/names;
- historical alternatives;
- rationale.

A numeric value is normative only because the product requirement fixes that behavior, not because a Scala `val` exists.

Test: would changing this value change a promised product guarantee?

- Yes → it may belong in `docs/PRODUCT_CONTRACT.md`.
- No → it is an implementation/configuration fact, even if numerically identical to some product threshold.

Prefer positive normative statements over historical negations.

Example:

Good product contract:

`Every supplied mandatory technical GPX is an independent required trail.`

Not:

`Do not bring back family semantics.`

The rejected family model belongs in `CURRENT_STATE.md`.

### 2. Architecture invariant

Ask:

> Is this durable knowledge about how the current implementation must be structured or how representations/stages relate?

If yes, it belongs in `docs/ARCHITECTURE.md`.

Typical examples:

- `EdgeAttr` indices belong to trace geometry, not route geometry;
- mandatory canonical elevation wins at reconstruction stitches;
- protected-corridor evaluation uses canonicalized safety geometry;
- RAW search and post-search selection are separate stages;
- rider dominance is valid only for equivalent continuation state;
- transfer rider metrics are computed from route geometry using the established chunking mechanism.

Architecture statements explain **how the current system works**.

Do not turn every current implementation detail into an architecture invariant.

Promote only durable knowledge about the current implementation boundary whose violation would materially affect semantics, correctness, or safety at a high-risk boundary.

Maintainability alone is not sufficient grounds for architecture promotion.

### 3. Current/rejected/open decision state

Ask:

> Is the important fact that a direction is established, rejected, closed, or still open?

If yes, record the status in `docs/CURRENT_STATE.md`.

Typical examples:

- family-based mandatory semantics are rejected;
- approximate ordering/search is rejected;
- fixed rider-search horizon is rejected;
- cross-profile failure pruning is rejected;
- global-extrema-normalized knee is rejected;
- the current local selector direction is established.

Do not restate the full positive product contract or architecture there.

Prefer:

`The exact-search contract is established; see PC-SEARCH-*.`

and separately:

`Approximate ordering/search: Rejected.`

Reopening conditions are owned only by the existing `Reopening an established decision` section.

Do not invent local clauses such as:

- `without new evidence`;
- `unless justified`;
- `unless necessary`;
- `for performance reasons`.

### 4. Baseline / fixture / provenance fact

Ask:

> Is this true only of the current canonical dataset, current environment, current fixture, or current run?

If yes, it is not automatically a product contract or architecture invariant.

Examples:

- canonical input count;
- current demanding-set names;
- Valhalla version/fingerprint;
- current selected routes;
- one-run timings;
- input hashes;
- baseline output summaries.

These belong in baseline/provenance/evidence documentation or baseline regression coverage.

Do not promote them into `PRODUCT_CONTRACT.md` merely because tests assert them.

### 5. Rationale / evidence

Ask:

> Is the durable value primarily why a decision exists, what experiment disproved an alternative, or what evidence established it?

If yes, canonical current docs are not the right owner.

Use an ADR or evidence history/ledger.

Examples:

- why ~30 m physics chunks were chosen;
- why a global normalized knee was abandoned;
- evidence that a profile expansion had zero production value;
- why a runtime topology heuristic was removed.

`ARCHITECTURE.md` may state the current mechanism.
`CURRENT_STATE.md` may state a rejected direction.
ADR/evidence owns the rationale/history.

Preserve rationale outside current-truth docs only when it records durable decision knowledge, such as:

- a disproved alternative;
- a non-obvious external/data limitation;
- a safety margin or its reason;
- evidence required to understand why a current mechanism must not be casually reverted.

Do not create an ADR for a comment that merely restates a mechanism already recorded in `ARCHITECTURE.md`, is obvious from the code, or contains no durable decision rationale.

### 6. Agent workflow

If the fact is about how future agents must work, it belongs in `AGENTS.md`.

Examples:

- newly established invariants require regression protection;
- do not wait for a separate user request;
- performance work must preserve the problem being solved;
- do not weaken product/safety contracts to make a route succeed.

Do not put product semantics into `AGENTS.md`.

## Product vs architecture test

When uncertain between product contract and architecture, apply this rewrite test:

> If the whole planner were reimplemented using different algorithms and representations, must this statement remain literally true?

- **Yes** → product contract.
- **No, but the current implementation depends on it for correctness, and violating it would materially affect semantics, correctness, or safety at a high-risk boundary** → architecture.
- **No, and the current implementation merely uses it** → implementation detail; do not promote.
- **Only the fact that this design was selected/rejected matters** → current state.
- **Only the reason/history matters** → ADR/evidence.
- **Only the current dataset/run exhibits it** → baseline/provenance.

For architecture candidates, apply the canonical materiality rule from step 2 of the classification procedure; this test does not define a separate architecture policy.

If still uncertain, do not guess.
Report the classification ambiguity.

## Promotion procedure

### 1. Reproduce or establish the fact

For bugs:

1. reproduce the failure;
2. identify the actual cause;
3. distinguish symptom from invariant.

For review findings:

1. inspect actual production code;
2. inspect relevant tests/data/contracts;
3. derive actual behavior rather than author intent.

Do not promote the symptom when the durable knowledge is deeper.

Promote the narrowest durable invariant that explains the reproduced failure and is supported by the evidence.

Do not promote a broader desirable property merely because it would prevent the observed bug. If a broader property appears desirable but was not established, report it as unresolved.

### 2. State the invariant in one sentence

Before editing docs, formulate the candidate invariant without implementation history.

Examples:

Symptom:

`Candidate 16 changed C2.`

Invariant:

`Adding a far low-marginal-benefit alternative must not move an established local tradeoff elbow when neighboring tradeoff geometry is unchanged.`

Symptom:

`Road audit indexed the wrong points.`

Invariant:

`Edge-derived shape indices belong to the trace geometry that produced them and are not interchangeable with route-geometry indices.`

This one-sentence form is only a working classification aid.
Do not create a new document just to store it.

### 3. Identify the canonical owner

Choose exactly one primary owner using the classification procedure above.

Do not place full copies in multiple canonical documents.

### 4. Check whether the knowledge already exists

Before adding anything:

- search the canonical owner;
- search related `PC-*` IDs;
- search relevant architecture sections;
- search rejected/open/current-state sections;
- inspect existing regression tests.

Do not create a second invariant with slightly different wording.

If an existing invariant already covers the finding, update protection/evidence rather than adding a duplicate.

### 5. Decide whether a regression is required

Every newly established permanent product or architecture invariant requires regression protection, regardless of how it was established — reproduced defect, independent review, code/data contract, or any other accepted evidence source. This is not discretionary and agrees with the `AGENTS.md` Done criteria.

A reproduced defect is a common way an invariant becomes established, but it does not create a different regression standard. Whether knowledge is truly permanent and established is decided by the earlier evidence and classification stages, not here.

For every newly established permanent product or architecture invariant:

1. prefer minimally strengthening an existing regression;
2. otherwise add the smallest executable regression that protects the invariant;
3. do not waive protection because the invariant came from review rather than a reproduced defect;
4. implementation inconvenience, fixture complexity, missing standalone mode, or a missing end-to-end harness is **not** sufficient reason to waive regression protection.

Do not create a test solely to make documentation green, and do not invent a fake or low-value test merely to satisfy the rule.

If a required executable protection is genuinely impossible now because of a concrete structural blocker:

- status is `BLOCKED`, not `NOT APPLICABLE`;
- state the exact structural blocker;
- report the unresolved protection gap explicitly in the final report;
- do not treat the promotion/change as satisfying the normal Done criteria.

Separately, executable regression does not apply to some knowledge classes at all:

- rationale/history only;
- a repository workflow rule;
- a purely documentary ownership convention;
- a provenance/baseline fact whose protection belongs elsewhere.

For these, status is `NOT APPLICABLE`: no structural blocker is required, and a brief reason naming the knowledge class is sufficient. `NOT APPLICABLE` alone does not prevent normal Done status.

Do not broaden this list to permanent product/architecture correctness or safety invariants, and do not use `NOT APPLICABLE` for a testable permanent correctness/safety invariant merely because building its test is inconvenient.

### 6. Make the minimal production change

For bug fixes:

- prefer removing a disproven heuristic over adding compensating heuristics;
- preserve established exactness/safety/product contracts;
- do not broaden scope into architecture redesign unless the evidence requires it;
- do not weaken contracts to recover route feasibility.

### 7. Update canonical documentation

Update only the owner(s) whose truth actually changed.

Typical promotion combinations:

#### New product invariant

- `PRODUCT_CONTRACT.md` — normative `PC-*`;
- `CURRENT_STATE.md` — only if status/rejected/open state materially changes;
- `trail-plan.scala` — regression and minimal implementation fix;
- `TEST_MATRIX.md` — traceability via sync skill.

#### New architecture invariant

- `ARCHITECTURE.md` — canonical implementation invariant;
- `CURRENT_STATE.md` — only if a design direction becomes established/rejected/open;
- `trail-plan.scala` — regression/fix per the regression rule above;
- `TEST_MATRIX.md` — traceability via sync skill.

#### Newly rejected direction

- `CURRENT_STATE.md` — rejection/status;
- `PRODUCT_CONTRACT.md` only if a positive normative guarantee must also be introduced;
- `ARCHITECTURE.md` only if current implementation structure changed;
- regression when the rejected behavior corresponds to a reproducible correctness issue.

#### Rationale discovered

- ADR/evidence history;
- current docs only if current truth itself was missing.

### 8. Synchronize traceability

After the canonical owner and regression state are correct, use:

`.agents/skills/test-traceability-sync/SKILL.md`

to synchronize `docs/TEST_MATRIX.md`.

Invariant promotion decides **what the knowledge means**.

Traceability sync decides **what executable evidence protects it**.

Do not reverse this order.

`TEST_MATRIX.md` must not become the place where a new invariant is first defined.

## Stable contract IDs

When a genuinely new product requirement is required:

- before materially expanding an existing `PC-*`, apply a semantic-independence check:
  if the new guarantee can be false while all existing clauses of the current `PC-*` remain true, prefer a new stable `PC-*` ID instead of silently broadening the old contract;
- this is not a mandate for microscopic IDs; judge by semantic independence, traceability clarity, and whether one coverage classification can still meaningfully describe the combined contract;
- add a new stable `PC-*` ID in the appropriate existing family when practical;
- do not renumber unrelated IDs;
- do not reuse an old ID for a different requirement;
- keep one contract ID focused enough that coverage classification is meaningful.

If an existing `PC-*` is materially expanded, re-evaluate whether its current tests still cover every material clause.

A documentation-only clarification that does not change normative meaning does not require a new ID.

## Rejected-direction discipline

When an investigation disproves an approach:

- record the rejection in `CURRENT_STATE.md`;
- record the positive normative property in `PRODUCT_CONTRACT.md` only if one exists;
- record current implementation mechanics in `ARCHITECTURE.md` only if they changed;
- keep experiment/rationale details in ADR/evidence.

Do not encode reopening conditions inside the individual rejected subsection.

Use the repository's canonical `Reopening an established decision` policy.

## Information-loss guard

Before deleting a comment, diagnostic block, old documentation paragraph, or experiment history, ask:

> Does this contain unique durable knowledge that has no canonical owner?

If yes:

1. classify it;
2. move/promote only the durable part to the correct owner;
3. preserve rationale in ADR/evidence if needed;
4. then remove the obsolete duplicate/history from the wrong location.

Do not preserve large forensic histories in production code merely because they are the only copy.

Do not move rationale into `ARCHITECTURE.md` just to avoid losing it.

If a source comment contains unique decision-relevant rationale but the repository currently has no canonical ADR/evidence location:

- report the missing-owner gap;
- do not silently delete the unique rationale;
- do not move it into `ARCHITECTURE.md` merely because that file exists;
- do not invent an ADR hierarchy or file-naming convention from inside this skill; the missing historical owner is a separate repository-governance task.

## Scope control

Do not use this skill to:

- perform a broad documentation normalization pass;
- rewrite accepted docs for style;
- reopen established decisions because a different design looks cleaner;
- create speculative abstractions;
- create new `PC-*` requirements from implementation convenience;
- promote dataset-specific facts to product contracts;
- convert every tested helper behavior into architecture;
- add tests for every `MISSING` matrix row automatically;
- modify unrelated rejected/open state;
- sweep historical files or legacy copies unless they are part of the assigned problem.

Make the smallest human-reviewable diff that captures the newly established truth.

## Pre-existing inconsistencies

If you discover unrelated pre-existing documentation or traceability drift while promoting an invariant:

- report it;
- do not silently expand the change to fix it unless that inconsistency blocks correct promotion or is explicitly part of the assigned task.
- if it blocks correct promotion, report the blocker; do not expand scope silently and do not abandon the promotion.

Known unrelated drift must not be used as an excuse to leave the newly promoted invariant inconsistent.

## Completion audit

Before finishing, verify:

### Classification

- the new knowledge has exactly one canonical owner;
- it is not duplicated as full normative prose elsewhere;
- product vs architecture vs status vs baseline vs rationale classification is defensible.

### Evidence

- the invariant is supported by explicit requirement, reproducible evidence, code/data contract, or equivalent strong basis;
- no speculative observation was promoted.

### Regression

- every newly established permanent product/architecture invariant has regression protection, or its status is explicitly `BLOCKED` with the exact structural blocker and the change is not presented as satisfying the normal Done criteria;
- where regression status is `NOT APPLICABLE`, the promoted item is genuinely a regression-inapplicable knowledge class (rationale/workflow/documentary convention/provenance), not a permanent correctness/safety/product/architecture invariant;
- existing regression was strengthened instead of duplicated when appropriate;
- tests were not added merely to make coverage green.

### Documentation

- `PRODUCT_CONTRACT.md` contains only implementation-independent normative guarantees;
- `ARCHITECTURE.md` contains implementation architecture, not historical rationale;
- `CURRENT_STATE.md` contains status/rejections/open questions, not duplicate contracts;
- `AGENTS.md` contains workflow only;
- rationale/history is preserved outside current-truth docs when it matters.

### Traceability

- after promotion, `test-traceability-sync` has been applied when tests/contracts/architecture coverage materially changed;
- `TEST_MATRIX.md` reflects the executable protection honestly;
- `DIRECT` is not claimed for a partially asserted multi-clause requirement.

### Scope

- unrelated docs/code were not rewritten;
- no established decision was reopened without satisfying the repository reopening policy;
- no safety/product contract was weakened merely to make a route feasible.

## Required final report

After applying invariant promotion, report concisely:

1. **Established fact**
   - the one-sentence durable invariant or decision;

2. **Classification**
   - product contract / architecture invariant / current-state decision / baseline / rationale / workflow;

3. **Evidence**
   - exact requirement, reproduction, test, code/data fact, or review finding supporting promotion;

4. **Canonical owner**
   - file and section / `PC-*` ID;

5. **Regression**
   - status: `added` / `strengthened` / `existing` / `BLOCKED` / `NOT APPLICABLE`;
   - `BLOCKED` — exact structural blocker stated, unresolved protection gap exposed, and the change reported as not satisfying the normal Done criteria;
   - `NOT APPLICABLE` — the knowledge class (rationale/history, workflow rule, documentary convention, provenance/baseline) does not require executable regression, with a short reason; never used for a testable permanent correctness/safety invariant merely because its test is difficult to build;

6. **Traceability**
   - `TEST_MATRIX.md` rows changed by `test-traceability-sync`, if applicable;

7. **Rejected/changed direction**
   - any decision newly rejected or reopened;

8. **Information moved**
   - unique durable knowledge moved out of comments/history, if any;

9. **Scope confirmation**
   - no unrelated semantics, architecture, or documentation were changed.

If classification or evidence is insufficient, do not promote.
Report the unresolved question instead.
