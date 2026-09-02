# Spec Kit Ownership Audit — Initial Migration

Status: Adopted; constitution ratified, `AGENTS.md` thinned (steps 1–2 landed together in the initial migration commit)  
Type: Temporary migration analysis  
Scope: Existing Trail Plan governance documents supplied for Spec Kit adoption

## Executive conclusion

The initial migration should **not** replace or delete the current canonical
product/architecture documents.

The repository already has a strong one-owner-per-knowledge-type model. The main
integration gap is that Spec Kit commands automatically load the constitution,
but do not automatically treat the existing Trail Plan documents as their
canonical context.

The safest migration is therefore:

1. make `.specify/memory/constitution.md` the mandatory governance gateway;
2. have the constitution explicitly require Spec Kit phases to read the relevant
   current canonical owners;
3. use Spec Kit feature artifacts as flow-forward change records, not as a second
   set of current product/architecture owners;
4. thin `AGENTS.md` only after the constitution is ratified;
5. retain the current product contract, architecture, current-state document,
   test matrix, ADRs, and two project-specific skills for the trial period.

This avoids two sources of truth without forcing all persistent product semantics
into an oversized constitution.

## Artifact-by-artifact audit

| Current artifact | Unique role found | Initial migration decision | Reason |
|---|---|---|---|
| `AGENTS.md` | Repository navigation, canonical-owner routing, skill routing | **THINNED** | Reduced to a repository entrypoint at ratification (same commit as the constitution). Governance rules and Done criteria moved out; the constitution owns them. |
| `docs/PRODUCT_CONTRACT.md` | Stable `PC-*` implementation-independent product guarantees | **KEEP** | This is already a clean current-product owner. Replacing it with feature specs would fragment cross-cutting guarantees; copying it into the constitution would duplicate truth. |
| `docs/ARCHITECTURE.md` | Current cross-cutting implementation architecture and representation ownership | **KEEP** | Spec Kit `plan.md` is change-scoped and cannot replace a coherent current-system architecture view. |
| `docs/CURRENT_STATE.md` | Established/rejected/open status and reopening policy | **KEEP FOR TRIAL** | It contains unique decision-state information that is neither product contract nor architecture. It may later shrink, but deleting it now would lose explicit rejected/open semantics or force duplication elsewhere. |
| `docs/TEST_MATRIX.md` | Requirement/architecture ↔ executable regression traceability and coverage levels | **KEEP** | Spec Kit requirement checklists do not prove executable regression coverage. |
| `docs/adr/*.md` | Durable historical rationale | **KEEP UNCHANGED** | Spec Kit research/plan artifacts are change-local and do not replace ADR rationale. |
| `.agents/skills/invariant-promotion/SKILL.md` | Semantic classification/promotion of newly established durable knowledge | **KEEP FOR TRIAL** | Spec Kit structures intended changes; this skill handles durable knowledge discovered during investigation. |
| `.agents/skills/test-traceability-sync/SKILL.md` | Mechanical bidirectional synchronization of tests/contracts/architecture/matrix | **KEEP FOR TRIAL** | Spec Kit does not own Trail Plan's coverage classifications or matrix semantics. |
| `trail-plan.scala` | Canonical production code and default-running regression suite | **KEEP / DO NOT TOUCH IN MIGRATION** | Framework adoption must not change product behavior. |

## Key finding: PRODUCT_CONTRACT should not be migrated into constitution

`docs/PRODUCT_CONTRACT.md` contains many stable, cross-cutting guarantees:

- mandatory exact-once/direction/canonical sequence;
- demanding classification and warm-up policy;
- protected-corridor rules and thresholds;
- hard safety and road policy;
- real-ride evidence semantics;
- exact RAW/rider search;
- wall class and endpoint-role semantics;
- rider upgrade/selector rules;
- reconstruction/final audit;
- CLI/output/report behavior.

These are too detailed to duplicate safely in a governance constitution, but too
important to leave invisible to Spec Kit.

The constitution therefore should not restate them. Instead it should make the
product contract a mandatory canonical input for every relevant Spec Kit change.

## Key finding: ARCHITECTURE is not a duplicate of Spec Kit plan.md

`docs/ARCHITECTURE.md` describes the current whole-system model:

```text
canonical input
-> connector construction
-> exact graph search
-> post-search selection
-> reconstruction
-> independent audit
```

It also owns durable representation and boundary facts such as:

- `/route` `geometry` vs `/trace_attributes` `traceGeometry`;
- edge index-space ownership;
- protected-corridor processing;
- exact RAW and rider-search continuation semantics;
- selector/search separation;
- reconstruction and final-audit boundaries.

A feature `plan.md` explains how one change will be implemented. It should cite
and conform to the current architecture rather than replace it.

## Key finding: CURRENT_STATE still has a unique role

`docs/CURRENT_STATE.md` is mostly references to the product contract and
architecture, but its rejected-direction sections are unique:

- family-based mandatory semantics;
- approximate ordering/search;
- fixed rider-search horizon;
- cross-profile failure pruning;
- speculative profile/candidate expansion;
- global-extrema-normalized selector knee;
- runtime topology guesses.

It also uniquely owns the reopening rule for established decisions.

During the Spec Kit trial, the constitution should require relevant change phases
to read this file. After 2–3 real changes, reassess whether rejected-state
ownership is better represented by a smaller document or another standard
mechanism.

## Key finding: TEST_MATRIX remains outside Spec Kit

The matrix has a stricter purpose than a Spec Kit requirements checklist:

```text
canonical requirement / architecture invariant
↔ exact default-running Scala regression
↔ DIRECT / PARTIAL / INDIRECT / MISSING
```

It explicitly refuses to infer coverage from implementation checks or fixture
presence. That is valuable project-specific governance and should remain.

## Key finding: the two custom skills are narrow enough to retain

### invariant-promotion

Its unique function is:

```text
investigation result
-> classify product / architecture / status / baseline / rationale / workflow
-> establish canonical owner
-> require regression when appropriate
-> hand off traceability synchronization
```

This is not equivalent to normal Spec Kit feature planning.

### test-traceability-sync

Its unique function is mechanical consistency between:

```text
canonical owners
<-> exact default-running tests
<-> TEST_MATRIX coverage claims
```

This also is not a built-in Spec Kit responsibility.

Both skills currently hard-code the pre-Spec-Kit owner model. After the
constitution and thin `AGENTS.md` are ratified, they should receive a **minimal
integration edit** so they recognize Spec Kit active-change artifacts without
making those artifacts new current owners.

Do not rewrite them yet.

## Recommended source-of-truth model

During the trial:

```text
.specify/memory/constitution.md
    governance gateway + source precedence
            |
            +--> docs/PRODUCT_CONTRACT.md   current product truth
            +--> docs/ARCHITECTURE.md       current architecture truth
            +--> docs/CURRENT_STATE.md      established/rejected/open status
            +--> docs/adr/                  durable rationale
            +--> docs/TEST_MATRIX.md        executable traceability
            +--> trail-plan.scala           implementation + regressions

specs/<change>/
    approved change intent / plan / research / tasks
    historical flow-forward record after completion
```

This prevents the feature spec from becoming a second permanent product contract.

## Migration sequence after this audit

### Steps 1–2 — DONE (initial migration commit)

Review, ratify, and adopt:

```text
.specify/memory/constitution.md
AGENTS.md (reduced to a thin entrypoint/router)
```

Actual landing differed from the original sequence: the thin `AGENTS.md` was
created in the same ratified migration step as the constitution, not in a
separate later step. The governance content that previously lived in
`AGENTS.md` now has a single owner — the constitution.

### Step 3

Make minimal integration edits to the two custom skills only where their current
read-order/owner language needs to acknowledge the Spec Kit constitution and
active change artifacts.

Do not change their semantic responsibilities.

### Step 4

Run the first real bounded planner change through Spec Kit.

Require the feature spec/plan to identify relevant `PC-*`, architecture sections,
current-state decisions, ADRs, and test-matrix rows.

### Step 5 — after 2–3 real changes

Re-audit whether:

- `CURRENT_STATE.md` can be reduced;
- any AGENTS content remains duplicated;
- either custom skill is redundant or should become a Spec Kit extension;
- flow-forward specs are working as intended.

Do not pre-decide those outcomes.

## No files were removed in the initial migration commit

The constitution bootstrap is successful when Spec Kit gains a binding path to
the existing canonical knowledge without changing that knowledge.

That means the initial migration change added/ratified the constitution,
reduced `AGENTS.md` to the thin entrypoint described above, and left these
files semantically untouched:

```text
docs/PRODUCT_CONTRACT.md
docs/ARCHITECTURE.md
docs/CURRENT_STATE.md
docs/TEST_MATRIX.md
docs/adr/*
trail-plan.scala
```
