# Spec Kit Migration Plan

Status: In progress — initial adoption landed  
Type: Repository and development-process migration plan  
Scope: Trails / `trail-plan.scala` repository  
Framework: GitHub Spec Kit 1.0.3 (recorded in `.specify/init-options.json`)  
Primary agent frontend: OpenCode (current and only adopted integration)

> **Actual adoption state (as of constitution v1.1.0).** Phases 0–6 and 10 have
> effectively landed: Spec Kit 1.0.3 is initialized in this repository
> (`.specify/`, `.opencode/commands/`), the constitution is ratified, and
> `AGENTS.md` is a thin entrypoint/router. Adoption used OpenCode only:
> the Codex-first sequencing in Phases 2–3 was not followed, and Codex is
> **not currently in scope**; treat those phases as superseded historical
> options, not requirements. No `trail-plan.scala`, ADR, product-contract,
> architecture, current-state, or test-matrix semantics changed as part of
> adoption. The remaining phases (first real Spec Kit change, trial
> evaluation, ownership re-audit) have not run.

> This document coordinates a possible migration to GitHub Spec Kit.
> It is **not** a product contract, architecture decision record, or new source of
> truth for planner behavior. Existing canonical owners remain authoritative
> until ownership is explicitly transferred and verified.
>
> The migration must not change `trail-plan.scala`, planner semantics, regression
> semantics, GPX inputs, or production outputs merely as a consequence of adopting
> Spec Kit.

---

## 1. Goal

Adopt GitHub Spec Kit as the repository-level Spec-Driven Development workflow
for future substantive product changes, while preserving the project's existing
high-value governance assets:

- canonical production source: `trail-plan.scala`;
- durable product/architecture decisions: `docs/adr/`;
- executable regression traceability: `docs/TEST_MATRIX.md`;
- established product and architecture knowledge;
- the existing `invariant-promotion` and `test-traceability-sync` mechanisms
  until real Spec Kit usage proves they are redundant or shows how to integrate
  them cleanly.

The intended result is a workflow in which a substantive change moves through
durable repository artifacts instead of depending on chat history:

```text
constitution
    ↓
specify
    ↓
clarify
    ↓
plan
    ↓
checklist
    ↓
tasks
    ↓
analyze
    ↓
implement
    ↓
converge
```

For small, low-risk changes, Spec Kit supports a shorter path, but the full path
should be the default for safety-sensitive planner behavior until experience
shows where shortening is safe.

---

## 2. Why migrate

The project already solved several problems that become especially important in
LLM-assisted development:

- durable product contracts;
- explicit architecture ownership;
- ADRs for historical rationale;
- regression traceability;
- promotion of newly established invariants;
- separation of evidence from canonical behavior.

What is still largely custom is the **change-execution workflow itself**:
turning a user intent into a reviewed specification, clarifying ambiguity,
planning implementation, decomposing work, checking cross-artifact consistency,
executing, and verifying convergence.

Spec Kit provides a standard, agent-agnostic framework for that layer.

The migration is intended to reduce:

- scope drift;
- silent reinterpretation of the requested task;
- implementation before requirements are sufficiently explicit;
- loss of rationale in chat history;
- confusion between product requirements and implementation choices;
- self-approved changes where the same LLM invents a requirement, implements it,
  and then treats its own implementation as proof that the requirement was correct.

---

## 3. Non-goals

This migration must **not**:

1. change planner product behavior;
2. change the exact-search contract;
3. change mandatory GPX semantics;
4. change safety, evidence, selector, reconstruction, or output semantics;
5. modify `trail-plan.scala` simply to fit Spec Kit;
6. retrospectively recreate the entire existing planner as artificial Spec Kit
   feature specs;
7. rewrite ADR-0001 through ADR-0005 into Spec Kit `research.md` files;
8. replace `docs/TEST_MATRIX.md` with Spec Kit checklists;
9. delete `PRODUCT_CONTRACT.md`, `ARCHITECTURE.md`, or `CURRENT_STATE.md` in a
   big-bang migration;
10. immediately delete the existing custom skills;
11. adopt additional Spec Kit extensions or presets before a real gap is proven;
12. automate human approval gates before the manual workflow has been validated.

---

## 4. Current baseline

Current repository governance is approximately:

```text
AGENTS.md
    agent working rules / routing

docs/CURRENT_STATE.md
    established / rejected / open knowledge

docs/PRODUCT_CONTRACT.md
    product requirements

docs/ARCHITECTURE.md
    current implementation architecture

docs/TEST_MATRIX.md
    requirement ↔ executable regression traceability

docs/adr/
    0001 ...
    0002 ...
    0003 ...
    0004 ...
    0005 ...
    durable historical product/architecture decisions

.agents/skills/invariant-promotion/
    newly established durable knowledge → correct owner + regression discipline

.agents/skills/test-traceability-sync/
    mechanical owner ↔ tests ↔ TEST_MATRIX synchronization

trail-plan.scala
    canonical production implementation
```

This baseline remains authoritative during migration.

---

## 5. Target ownership model

The target is **not** “everything moves into Spec Kit.”

Instead, each artifact must have one clear role.

| Knowledge / responsibility | Intended owner after migration |
|---|---|
| Global non-negotiable project principles | `.specify/memory/constitution.md` |
| What a specific future change must accomplish | `specs/<change>/spec.md` |
| Ambiguities resolved for that change | incorporated into `spec.md` by `clarify` |
| How that change will be implemented | `specs/<change>/plan.md` |
| Investigation and change-local trade-off evidence | `specs/<change>/research.md` |
| Requirement-quality review | `specs/<change>/checklists/` |
| Dependency-ordered implementation work | `specs/<change>/tasks.md` |
| Durable historical architecture/product decision | `docs/adr/*.md` |
| Executable product regression traceability | `docs/TEST_MATRIX.md` |
| Canonical production implementation | `trail-plan.scala` |
| Agent bootstrap / repository entry instructions | thin `AGENTS.md` |
| Unexpectedly discovered durable invariant | existing `invariant-promotion` until superseded |
| Mechanical regression/matrix synchronization | existing `test-traceability-sync` until superseded |

Two important constraints follow:

1. Spec Kit feature artifacts are **change-scoped**.
2. Cross-cutting current-product knowledge may still need a global owner even
   after Spec Kit adoption.

Therefore `PRODUCT_CONTRACT.md` and `ARCHITECTURE.md` are not assumed obsolete in
advance. Their final role must be decided after real Spec Kit usage.

---

## 6. Migration safety invariants

The following must hold throughout the migration.

### 6.1 Product source is unchanged

Record the pre-migration blob:

```bash
git rev-parse HEAD:trail-plan.scala
```

After framework-bootstrap commits, the blob must still match.

A simple verification:

```bash
git diff <pre-spec-kit-ref>:trail-plan.scala HEAD:trail-plan.scala
```

Expected result: empty.

### 6.2 Existing ADRs are preserved

ADR-0001 through ADR-0005 describe historical product/architecture decisions and
must not be rewritten merely to fit Spec Kit.

During initial adoption:

```bash
git diff <pre-spec-kit-ref> HEAD -- docs/adr
```

Expected result: empty, except for an independently justified correction unrelated
to Spec Kit migration.

### 6.3 No silent owner duplication

A normative fact must not become simultaneously owned by:

```text
PRODUCT_CONTRACT
+ constitution
+ spec
+ AGENTS
```

Migration is performed fact-by-fact, not file-by-file.

### 6.4 No loss of established invariants

Removing or shrinking an old document requires proving that each durable
statement now has a valid owner.

### 6.5 Spec Kit artifacts do not override existing baseline accidentally

Until an ownership transfer is explicit, the existing canonical repository
documents remain authoritative.

### 6.6 Chat history is not an owner

Neither prior LLM conversation nor migration discussion may override the current
repository state.

---

## 7. Phase 0 — create a reviewable migration baseline

Before installing anything:

```bash
git switch -c adopt-spec-kit
git status --short
```

The working tree must be clean, or all pre-existing work must be explicitly
preserved.

Create a recovery point:

```bash
git branch backup/pre-spec-kit-adoption
git tag backup-pre-spec-kit-adoption
git bundle create ../trail-plan-before-spec-kit.bundle --all
git bundle verify ../trail-plan-before-spec-kit.bundle
```

Record:

```bash
PRE_SPEC_KIT=$(git rev-parse HEAD)
PRE_SPEC_KIT_SOURCE=$(git rev-parse HEAD:trail-plan.scala)
```

### Gate

Do not continue unless:

- working tree state is understood;
- backup ref exists;
- bundle verifies;
- current `trail-plan.scala` blob is recorded.

---

## 8. Phase 1 — install a pinned Spec Kit CLI

Use a pinned released `specify-cli`, not a floating development branch.

Example:

```bash
uv tool install specify-cli==<chosen-release>
specify version
```

Record the chosen version in this migration document or the migration commit
message.

The framework version becomes part of the reproducible engineering environment;
upgrading it later should be a deliberate change.

### Gate

Verify:

```bash
specify version
```

and record the exact version used for bootstrap.

---

## 9. Phase 2 — initialize Spec Kit in the existing repository

Initialize using Codex as the first integration:

```bash
specify init --here --force --integration codex
```

`--force` is expected for an existing non-empty repository, but it is only safe
because Phase 0 created a reviewable baseline and recovery point.

Immediately review:

```bash
git status --short
git diff --stat
git diff
specify integration status
```

Do **not** run the constitution workflow yet.

The purpose of this phase is only to understand exactly what Spec Kit generated.

### Gate

Confirm:

- `trail-plan.scala` is untouched;
- ADRs are untouched;
- generated Spec Kit infrastructure is understood;
- no existing custom skill was silently overwritten;
- no generated file has become a second owner of existing product truth.

If any of these fail, stop and resolve before continuing.

---

## 10. Phase 3 — add OpenCode as a second frontend

After Codex initialization, inspect available integrations:

```bash
specify integration list
specify integration info opencode
```

Then install OpenCode:

```bash
specify integration install opencode
```

If Spec Kit refuses because the combination is not declared multi-install-safe,
do not immediately force it.

First inspect:

```bash
specify integration status
git status --short
```

If explicit multi-install is still desired and the generated paths are verified
not to damage the existing Codex integration or repository files:

```bash
specify integration install opencode --force
```

After installation:

```bash
specify integration status
git diff
```

### Important current-framework nuance

Spec Kit tracks one **default integration** even when multiple integrations are
installed. Extensions and presets follow the active/default integration when
they are scaffolded or re-scaffolded.

Therefore the project goal is **not** “Codex and OpenCode have identical agent
directories.”

The goal is:

```text
shared Spec Kit artifacts
+ separate supported agent adapters
+ no duplicated product truth
```

### Gate

Verify that:

- both Codex and OpenCode can see the same Spec Kit project;
- shared `.specify/` and `specs/` state is not duplicated;
- agent-specific generated paths are understood;
- switching the default integration is not required for normal repository truth.

---

## 11. Phase 4 — ownership inventory before writing the constitution

Before copying any current document into Spec Kit, perform a semantic inventory
of:

```text
AGENTS.md
docs/PRODUCT_CONTRACT.md
docs/ARCHITECTURE.md
docs/CURRENT_STATE.md
docs/TEST_MATRIX.md
docs/adr/*.md
.agents/skills/invariant-promotion/SKILL.md
.agents/skills/test-traceability-sync/SKILL.md
```

Classify each material statement as one of:

```text
A. global project principle
B. current product invariant
C. current architecture invariant
D. durable historical decision / rationale
E. regression traceability fact
F. temporary / investigation evidence
G. open work
H. agent-operational instruction
I. workflow rule
```

Assign a proposed future owner, but do not move the text yet.

### Required output

Create a temporary migration table in this document or a separate scratch
artifact:

| Statement / section | Current owner | Classification | Proposed owner | State |
|---|---|---|---|---|
| ... | ... | ... | ... | keep / transfer / unresolved |

### Gate

No constitution drafting until this inventory is reviewed.

---

## 12. Phase 5 — create a minimal constitution

The constitution should contain only principles that:

1. are already true for the repository; or
2. are explicitly approved as new global rules.

It must not become a compressed copy of every current contract.

Likely Trails constitution topics include:

```text
I. Canonical production artifact and repository truth
II. Mandatory technical GPX semantics
III. Exact-search integrity
IV. Safety/evidence fail-closed boundaries where required
V. Canonical representation ownership
VI. Durable-knowledge and regression discipline
VII. Verification / independent review expectations
```

Good constitutional rule:

```text
Exact search must not be silently replaced by beam, top-K, epsilon,
arbitrary-horizon, or other approximate search semantics.
```

Bad constitutional content:

```text
current BuildId
FIX53 runtime
candidate16 diagnostic counts
specific canonical selected route
individual test numbers
temporary evidence outcomes
```

Use:

```text
/speckit.constitution ...
```

only to generate/update a draft from already reviewed principles.

Then manually review the resulting file.

### Gate

For every constitutional clause answer:

- Was this already true, or explicitly approved now?
- Is it global rather than change-specific?
- Does it conflict with an existing ADR or contract?
- Is this the correct owner?
- Will putting it here create duplicate ownership?

---

## 13. Phase 6 — reduce `AGENTS.md` to a thin entrypoint

`AGENTS.md` should stop acting as a parallel end-to-end methodology.

Its target role:

```text
- identify the canonical production source;
- point agents to the Spec Kit workflow;
- identify repository-specific owners that remain outside Spec Kit;
- explain where ADRs and TEST_MATRIX live;
- route unexpected durable discoveries through invariant-promotion;
- route regression/matrix synchronization through test-traceability-sync;
- warn that historical source snapshots and chat history are not current truth.
```

Conceptually:

```text
AGENTS.md
    ↓
Spec Kit constitution + active change artifacts
    ↓
repository-specific durable owners
```

It should not duplicate the constitution.

### Gate

Check line-by-line that every removed AGENTS rule either:

- moved to the constitution;
- is already owned elsewhere;
- was obsolete;
- remains in AGENTS because it is genuinely operational.

---

## 14. Phase 7 — preserve ADR and TEST_MATRIX ownership

### ADR

Keep:

```text
docs/adr/0001-...
...
docs/adr/0005-...
```

Spec Kit `plan.md` and `research.md` may reference them.

They do not replace them.

### TEST_MATRIX

Keep:

```text
docs/TEST_MATRIX.md
```

Spec Kit `/speckit.checklist` validates **requirement quality**. It is not evidence
that implementation exists, and it is not a substitute for executable regression
traceability.

The existing matrix continues to own:

```text
product invariant
↔ concrete executable regression
↔ DIRECT / PARTIAL / other coverage classification
```

### Gate

No TEST_MATRIX row may disappear because a Spec Kit checklist exists.

---

## 15. Phase 8 — keep existing custom skills temporarily

Initially retain:

```text
.agents/skills/invariant-promotion/
.agents/skills/test-traceability-sync/
```

Do not rewrite them just to call Spec Kit commands.

Their continued existence is provisional.

### Hypothesis to test

Spec Kit may cover much of change execution, but likely does not fully cover:

```text
unexpected investigation result
→ durable invariant?
→ correct global owner?
→ regression required?
→ TEST_MATRIX synchronization?
```

After real usage, each skill will receive one of four outcomes:

```text
KEEP
SIMPLIFY
REIMPLEMENT AS SPEC KIT EXTENSION
DELETE AS REDUNDANT
```

No decision is made before evidence from actual changes.

---

## 16. Phase 9 — mark old global docs transitional, do not delete them

Initially retain:

```text
docs/PRODUCT_CONTRACT.md
docs/ARCHITECTURE.md
docs/CURRENT_STATE.md
```

If useful, add a minimal migration notice, but do not change their normative
meaning.

Possible notice:

```text
TRANSITIONAL PRE-SPEC-KIT OWNER.

New substantive changes use Spec Kit change artifacts.
Existing baseline clauses remain authoritative until ownership is explicitly
transferred and verified.
```

### Expected likely outcomes

#### `PRODUCT_CONTRACT.md`

Unresolved.

Spec Kit specs are change-scoped. Trails may still benefit from a global current
product contract.

Do not assume deletion.

#### `ARCHITECTURE.md`

Likely remains useful as current cross-cutting architecture documentation because
individual Spec Kit plans are change-scoped.

Do not assume deletion.

#### `CURRENT_STATE.md`

Strongest candidate for eventual decomposition/removal.

Possible destinations:

```text
established product truth    → product contract / constitution where appropriate
established architecture     → ARCHITECTURE / ADR
durable rejected decision    → ADR
change-local evidence        → archived Spec Kit research
open work                    → active change tasks/spec
```

But this is decided only after trial changes.

---

## 17. Phase 10 — commit framework bootstrap separately

Recommended first migration commit:

```text
Adopt GitHub Spec Kit workflow
```

It should contain only framework/process scaffolding such as:

```text
.specify/*
Codex integration files
OpenCode integration files
reviewed constitution
thin AGENTS.md
migration-plan updates
```

It should not contain:

```text
trail-plan.scala changes
planner test-semantic changes
ADR rewrites
large contract rewrites
```

### Gate

Verify:

```bash
test "$(git rev-parse HEAD:trail-plan.scala)" = "$PRE_SPEC_KIT_SOURCE"
```

and review the complete diff before committing.

---

## 18. Phase 11 — first real Spec Kit change

The first `specs/001-...` should describe the **next real planner change**.

Do not create a fake feature such as:

```text
001-current-planner
001-migrate-docs
```

merely to populate the framework.

For a substantive planner change, use the full production flow.

### 18.1 Specify

```text
/speckit.specify ...
```

`spec.md` owns WHAT and WHY:

```text
user/problem scenario
requirements
acceptance scenarios
success criteria
non-goals
```

Avoid implementation details here.

### 18.2 Clarify

```text
/speckit.clarify
```

Use this aggressively for questions such as:

```text
What is exact?
Which existing behavior must remain unchanged?
What is the final production artifact?
What evidence would establish success?
Which outputs are in scope?
Which tempting adjacent changes are explicitly out of scope?
```

Resolved answers are folded back into the specification.

### 18.3 Plan

```text
/speckit.plan ...
```

`plan.md` owns implementation approach.

For Trails this should explicitly identify:

```text
affected Scala structures/functions
relevant architecture boundaries
applicable ADRs
product invariants touched
tests that may need strengthening
representation ownership
safety implications
```

### 18.4 Research

Use `research.md` for investigation evidence and alternatives local to the
change.

This is a natural destination for information that historically accumulated in
temporary FIX diagnostics:

```text
hypothesis
experiment
evidence
result
why rejected/promoted
```

Change-local research is not automatically a durable global invariant.

### 18.5 Checklist

```text
/speckit.checklist
```

Use it to review requirements quality.

Do not treat checked items as executable test evidence.

### 18.6 Tasks

```text
/speckit.tasks
```

Tasks should be implementation-ready and dependency ordered.

### 18.7 Analyze

```text
/speckit.analyze
```

This is a pre-implementation gate.

Resolve contradictions at their source:

```text
spec problem  → fix spec
plan problem  → fix plan
task problem  → fix tasks
```

Do not “fix” a contradiction by weakening the constitution.

### 18.8 Implement

```text
/speckit.implement
```

Only after approved requirements, plan, tasks, and analysis.

### 18.9 Converge

```text
/speckit.converge
```

Compare implementation against the Spec Kit artifacts.

If gaps generate new tasks:

```text
converge
→ implement remaining tasks
→ converge again
```

Continue until converged, then perform normal independent code review and project
regression checks.

---

## 19. Phase 12 — integrate existing invariant promotion into the new lifecycle

During a Spec Kit change:

```text
investigation
    ↓
new fact discovered?
    ↓
no → continue change
    ↓
yes
    ↓
hypothesis only?
    ├─ yes → research.md only
    └─ no
         ↓
      invariant-promotion
         ↓
      classify durable owner
         ↓
      regression required?
         ↓
      implement/strengthen regression
         ↓
      test-traceability-sync
         ↓
      TEST_MATRIX
         ↓
      re-run relevant Spec Kit consistency/convergence checks
```

This preserves a crucial distinction:

```text
Spec Kit
    = disciplined execution of an intended change

invariant-promotion
    = governance for durable knowledge discovered while doing the work
```

If later evidence shows Spec Kit extensions can own this cleanly, migrate then.

---

## 20. Phase 13 — human approval gates during trial period

For the first several substantive Spec Kit changes, do not fully automate the
flow.

Require explicit human review at:

```text
spec       → approve/edit/reject
clarify    → approve resolved requirements
plan       → approve/edit/reject
tasks      → approve/edit/reject
analyze    → review findings
implement  → allowed only after prior gates
converge   → review residual gaps
```

The purpose of SDD is not to let the LLM create more Markdown before coding.

The purpose is to make intent and technical choices inspectable **before**
implementation.

---

## 21. Phase 14 — cross-agent trial with Codex and OpenCode

> **Deferred.** Actual adoption is OpenCode-only; Codex is not currently in
> scope. Run this phase only if/when a second frontend is deliberately added.
> Until then, the equivalent required check is that a fresh OpenCode session
> can resume work from repository artifacts without chat context.

Use both supported frontends against the same repository artifacts.

Suggested trial:

```text
Codex:
  specify / clarify / plan / implement

OpenCode:
  independent spec-plan-implementation review
```

Then reverse roles on the next substantive change.

Evaluate:

1. Can either agent resume from the repository without old chat context?
2. Do both interpret the same constitution and active spec consistently?
3. Does either agent create a competing source of truth?
4. Can review identify scope drift from spec to plan or plan to implementation?
5. Are agent-specific integration files stable and non-overlapping in practice?
6. Does switching agents require manual restatement of product intent?

Success means repository artifacts, not agent memory, carry the change.

---

## 22. Phase 15 — evaluate after 2–3 real changes

Do not perform final documentation cleanup after only one toy example.

After 2–3 real planner changes, run a second ownership audit.

For each old artifact decide:

### `PRODUCT_CONTRACT.md`

Choose based on evidence:

```text
KEEP as global current-product contract
or
REDUCE because constitution + specs now own most content
or
RETIRE only if every durable clause has a proven owner
```

### `ARCHITECTURE.md`

Choose:

```text
KEEP as cross-cutting current architecture
or
REDUCE to system-wide architecture only
```

Do not replace it with a pile of feature-local `plan.md` files unless those
actually provide an adequate current-system view.

### `CURRENT_STATE.md`

Choose:

```text
KEEP only if it still has a unique role
or
DECOMPOSE and RETIRE
```

### Custom skills

For each:

```text
KEEP
SIMPLIFY
CONVERT TO SPEC KIT EXTENSION
DELETE
```

The decision must be based on observed workflow gaps.

---

## 23. Phase 16 — possible Spec Kit extension migration

Only after the gap is proven, investigate whether:

```text
invariant-promotion
test-traceability-sync
```

should become Spec Kit extensions/presets rather than standalone project
methodology.

Requirements before doing so:

- the extension mechanism can preserve current semantics;
- ownership boundaries remain explicit;
- Codex and OpenCode still behave consistently;
- the custom extension does not fork the core Spec Kit workflow unnecessarily.

Do not create a Spec Kit extension merely for organizational neatness.

---

## 24. Validation scenarios

Before declaring migration successful, run controlled non-production dry-runs.

### Scenario A — pressure to approximate exact search

Prompt a change such as:

```text
Speed up rider search by keeping only top-K labels per state.
```

Expected behavior:

- conflict with established exact-search principles is surfaced;
- the agent does not silently reinterpret exactness;
- implementation is blocked unless the governing product decision is explicitly
  changed through the proper process.

### Scenario B — reintroduce rejected family semantics

Prompt:

```text
Group similar mandatory trails into families to reduce the search space.
```

Expected:

- conflict with established mandatory independent semantics is surfaced;
- the agent does not treat an optimization idea as permission to change the
  product model.

### Scenario C — analysis artifact substituted for production artifact

Ask for a production metric/change while allowing diagnostics.

Expected:

- diagnostics/research may be temporary;
- final implementation still satisfies the production spec;
- `converge` does not accept an auxiliary analysis artifact as completion if the
  spec requires production behavior.

### Scenario D — checklist overclaim

A requirements checklist is fully checked but no matching regression exists.

Expected:

- the checklist is not treated as TEST_MATRIX evidence;
- executable coverage remains governed separately.

These dry-runs should be discarded or archived as migration evidence, not kept as
fake product features.

---

## 25. Rollback plan

If the trial shows that Spec Kit adds more duplication or ambiguity than value:

1. stop creating new Spec Kit changes;
2. preserve any useful migration evidence;
3. restore the pre-adoption branch/tag if necessary;
4. remove generated Spec Kit infrastructure in a dedicated rollback commit;
5. restore the prior thin/full AGENTS behavior as appropriate;
6. keep ADRs, TEST_MATRIX, product source, and existing canonical docs unchanged;
7. document why the evaluation failed before selecting another framework.

Rollback must not require reverting planner behavior because the adoption itself
must not have changed planner behavior.

---

## 26. Migration completion criteria

Migration is complete only when all of the following are true:

- [x] Spec Kit version is intentionally selected and recorded
      (1.0.3, `.specify/init-options.json`).
- [x] Spec Kit is initialized in the existing repository.
- [x] The adopted integration set is deliberate: OpenCode only; Codex is not
      currently in scope (see adoption-status note at the top).
- [x] OpenCode integration works or has an explicitly documented limitation.
- [x] Constitution contains only reviewed global principles (v1.1.0).
- [x] `AGENTS.md` is a thin bootstrap/router rather than a competing methodology.
- [x] ADR-0001 through ADR-0005 remain authoritative historical decisions.
- [x] `docs/TEST_MATRIX.md` remains authoritative for executable regression traceability.
- [x] `trail-plan.scala` did not change as part of framework adoption.
- [ ] At least 2–3 real planner changes were completed through the new workflow.
- [ ] Agent continuation/review from repository artifacts (without chat context)
      was tested for the adopted integration.
- [ ] Old documentation ownership was re-audited after real usage.
- [ ] No durable established invariant was lost.
- [ ] No normative fact has accidental duplicate textual ownership.
- [ ] The future role of `PRODUCT_CONTRACT.md` is explicitly decided.
- [ ] The future role of `ARCHITECTURE.md` is explicitly decided.
- [ ] The future role of `CURRENT_STATE.md` is explicitly decided.
- [ ] Each custom skill has an explicit KEEP / SIMPLIFY / EXTENSION / DELETE decision.
- [ ] Rollback remains possible from the saved baseline.

---

## 27. What not to do

Do not:

- create historical fake specs for versions `0.5`, `1`, `2`, `2.1`;
- convert ADR history into feature research;
- copy every product rule into the constitution;
- delete global contracts before ownership transfer is proven;
- replace executable regression traceability with requirement checklists;
- let generated Spec Kit files modify planner semantics during adoption;
- allow Codex and OpenCode to maintain separate product truths;
- add framework extensions before a concrete gap appears;
- automate every gate before humans have validated the workflow;
- declare migration complete because Spec Kit initialized successfully.

---

## 28. Proposed commit sequence

A clean migration history could look like:

```text
1. Add Spec Kit migration plan
2. Bootstrap GitHub Spec Kit for Codex
3. Add reviewed OpenCode integration
4. Establish project constitution and thin AGENTS entrypoint
5. Mark legacy documentation ownership as transitional
6. <next real planner change through Spec Kit>
7. <second real planner change through Spec Kit>
8. <third real planner change through Spec Kit>
9. Reconcile documentation ownership after Spec Kit trial
10. Simplify/retain/migrate custom governance skills based on evidence
```

No source-code change is required in commits 1–5.

---

## 29. Expected steady state

A successful steady state may look like:

```text
trail-plan.scala
    canonical production implementation

AGENTS.md
    thin repository/agent entrypoint

.specify/
    Spec Kit infrastructure
    constitution

specs/
    active and archived change specifications/plans/research/tasks

docs/PRODUCT_CONTRACT.md
    retained only if a global current-product contract remains useful

docs/ARCHITECTURE.md
    retained as cross-cutting current architecture if useful

docs/adr/
    durable historical product/architecture decisions

docs/TEST_MATRIX.md
    executable regression traceability

.agents/skills/
    only genuinely project-specific gaps not cleanly covered by Spec Kit
```

The desired outcome is not more documentation.

The desired outcome is a clear chain:

```text
human intent
    ↓
reviewed requirements
    ↓
reviewed technical plan
    ↓
reviewed implementation tasks
    ↓
consistency analysis
    ↓
production implementation
    ↓
convergence check
    ↓
independent review + executable regressions
    ↓
durable knowledge promoted to the correct canonical owner
```

---

## 30. Status of this document

This plan is temporary migration coordination.

While migration is in progress:

```text
Status: Proposed / In progress
```

After the trial and final ownership reconciliation, either:

1. move it to a historical migration location; or
2. retain it as a non-normative historical record.

It must not remain a competing source of current product or architecture truth.
