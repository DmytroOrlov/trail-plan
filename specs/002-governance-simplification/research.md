# Research: Governance Simplification — Back to Stock Spec Kit

Phase 0 output for [plan.md](./plan.md). Every Technical Context entry in the
plan is resolved; no NEEDS CLARIFICATION remains. Each decision records
Decision / Rationale / Alternatives considered, with evidence gathered from the
current tree (reads of the constitution 1.1.0, both skills, the overlay files,
`AGENTS.md`, the docs set, `specs/001-truth-review-fixes/`, and live
`specify workflow` CLI queries against `specify 1.0.3`).

## D1. Baseline surface actually in scope

**Decision**: The live (current-obligation) machinery surface is exactly:
`.specify/memory/constitution.md` (Principles IV/V skill handoffs, §Spec Kit
Change Lifecycle trailing stages, §Governance entry-point wording),
`AGENTS.md:30-31` (skill routing), `docs/TEST_MATRIX.md:113` (skill pointer in
§Coverage gaps), `.agents/skills/invariant-promotion/SKILL.md`,
`.agents/skills/test-traceability-sync/SKILL.md`,
`.specify/workflows/overlays/speckit.yml`,
`.specify/workflows/overlays/speckit/trail-plan-speckit-completion.yml`, and the
registered project overlay `trail-plan-speckit-completion`. Nothing else in the
repository presents deleted machinery as a current obligation.

**Rationale**: A tree-wide search for the deleted-machinery terms
(`invariant-promotion`, `test-traceability-sync`, `.agents/skills`,
`speckit-completion`, `overlay`, `promotion/`, `release gate`, `independent
review`) excluding `specs/001-truth-review-fixes/**` and `docs/migrations/**`
returns only those files plus this change's own `spec.md`. Bundled material
(`.specify/templates/**`, `.specify/scripts/**`, `.opencode/commands/**`)
contains no repository-specific process references at all, so FR-014 is
satisfied by simply not touching it.

**Alternatives considered**: Treating the `docs/migrations/*` narrative and the
`specs/001` `promotion/` + `checklists/release-gate.md` artifacts as live
references requiring rewrite — rejected by FR-023/SC-006 and by the SC-003
historical carve-out; rewriting them would replace one form of drift with
another (User Story 3).

## D2. Amendment mechanism and version step

**Decision**: The simplified text is adopted as a **MAJOR** amendment of the
same constitution file: `Version: 1.1.0 → 2.0.0`, keeping the leading
`Sync Impact Report` comment with version change, amendment rationale, changed
principles, and follow-up status, plus the trailing version/ratified/amended
line. Human approval is the explicit user instruction embedded in the feature
input and recorded in the spec.

**Rationale**: The amendment rules in force (constitution 1.1.0 §Governance)
make a MAJOR step the case that "removes or redefines a binding principle", and
require rationale, human approval, a version bump, and review of affected
governance files. The spec's Assumptions already anticipate this reading. Doing
it inside the existing file (rather than replacing the file) keeps a single
governance entry point (SC-007) and preserves the amendment trail that Principle
III depends on for the "explicit, not silent" property.

**Alternatives considered**: (a) MINOR bump — rejected; principles and the
lifecycle are removed/redefined, not expanded. (b) PATCH "clarification" —
rejected; dishonest about the size of the change and would leave the version
header implying continuity of withdrawn obligations. (c) Draft a fresh
constitution without a Sync Impact Report — rejected; it drops the audit trail
the ownership audit (`docs/migrations/spec-kit-ownership-audit.md`) established
and would read as a silent rewrite.

## D3. Overlay withdrawal through the supported CLI

**Decision**: Withdraw the overlay in two enforced steps and verify a third:
(1) `specify workflow overlay remove speckit trail-plan-speckit-completion`
(the supported unregistration; the CLI may update the tracked,
CLI-managed `.specify/workflows/workflow-registry.json` if the registry state
requires it — such a diff, when produced, is a permitted narrowly scoped
CLI-managed side effect rather than scope drift, but a registry diff is NOT
required: if the pre-change registry already represents the correct
bundled-only state, no registry diff is expected or required, which is what
happened in this implementation); (2) delete both
repository-owned overlay files
`.specify/workflows/overlays/speckit.yml` and
`.specify/workflows/overlays/speckit/trail-plan-speckit-completion.yml`;
(3) verify `specify workflow overlay list speckit` reports no project overlay
and `specify workflow resolve speckit` shows only the base layer and its actual
base steps (`specify`, `review-spec`, `plan`, `review-plan`, `tasks`,
`implement`). The installed bundled/base `speckit` workflow does **not** supply
`converge`; `converge` is supplied today only by the project overlay and must
NOT be claimed as a base workflow step after removal. Contributors invoke the
stock `/speckit.converge` command directly (the stock command exists in
`.opencode/commands/speckit.converge.md`); a stock command available for direct
invocation is distinct from the installed bundled/base workflow graph.
`specify workflow list` still shows the bundled `Full SDD Cycle`. Bundled
`speckit/workflow.yml` and its base review gates are not edited.

**Rationale**: `specify workflow resolve speckit` today shows a
`project-overlay` layer at priority 10 contributing `converge`,
`independent-review`, `executable-regression-verification`, and
`traceability-verification`. FR-013 plus the edge case "file deletion alone is
not sufficient" require both the registration and the files to go. Note that
`converge` is currently supplied **by the overlay**, so the post-removal
workflow text must describe the contributor running `/speckit.converge` directly
(the stock command exists in `.opencode/commands/speckit.converge.md`) instead of
relying on the overlay step — which is precisely what FR-009 specifies and what
the spec disclaims in User Story 1.

**Alternatives considered**: (a) `specify workflow overlay disable` — rejected;
a disabled-but-registered overlay keeps `trail-plan-speckit-completion` visible
in `overlay list` and leaves repository-owned files in the tree, so SC-004 fails.
(b) Hand-delete the YAML files without the CLI — rejected by FR-013; registry
state and file state would disagree. (c) Edit the overlay to drop only the gate
steps and keep `converge` — rejected; that preserves repository-owned workflow
machinery, which FR-009/FR-013 withdraw entirely.

## D4. Which skill rules survive, and where

**Decision**: Fold only durable rules into the constitution and drop all
procedural machinery. Retained (already required by FR-002…FR-010): canonical
ownership and precedence; "one normative fact, one textual owner"; history and
chat context are not current authority; no-silent-weakening for product, safety,
exactness, representation, and fail-closed behavior; active specs describe
changes, canonical docs describe current truth; same-change update of the
affected canonical owner; same-change `docs/TEST_MATRIX.md` update when normative
clauses or their coverage change; deterministic regression protection for newly
established normative invariants when practically testable; the DIRECT
honesty rule; stock-command completion; amendment procedure. Dropped: skill
trigger lists, the classification decision trees, product-vs-architecture
rewrite tests, editing-discipline checklists, `Required final report` formats,
the promotion-before-sync ordering ceremony, `promotion/` evidence-package
patterns, and the release-gate checklist concept.

**Rationale**: FR-011/FR-012 name the durable residue explicitly and require it
"directly and concisely" in the constitution, and FR-026 forbids replacements.
The dropped material is procedure that a contributor can re-derive from the
retained rules plus the canonical owners; keeping it would be the machinery this
change withdraws. `docs/TEST_MATRIX.md` §Coverage levels stays the owner of
coverage-class definitions, so the constitution states the honesty rule and
references that owner instead of restating class text (Principle I discipline).

**Alternatives considered**: (a) Keep the skills as optional guidance —
rejected by FR-011/FR-012 and SC-003: `AGENTS.md` and the constitution would
still route contributors into them. (b) Move the skill text into a
`docs/GOVERNANCE_PROCEDURE.md` — rejected by FR-026/SC-007 (new governance
artifact). (c) Fold full skill detail into the constitution — rejected by
FR-001/SC-002 ("substantially shorter than today"; the two skills alone are
~970 lines).

## D5. Target constitution shape

**Decision**: Structure the 2.0.0 text as five short sections — Core durable
rules (ownership/precedence, no-silent-weakening, change-vs-current-truth
separation), Working rules (same-change owner + traceability updates,
regression protection when practically testable, DIRECT honesty), Completion
(stock `/speckit.*` sequence through `/speckit.converge`, mutual consistency of
implementation/tests/owners/traceability, no remaining gaps at convergence),
Amendment/governance (rationale, human approval, version record, SemVer
classes), and an explicit non-authority note for history/chat/archived
artifacts. The 2.0.0 text is materially simpler than the current version,
carries only the durable rules retained by FR-002 through FR-010, leaves no
withdrawn machinery as a live obligation, and reproduces no product or
architecture semantics; no numeric line-count budget applies.

**Rationale**: FR-001/SC-002 demand a materially simpler document that states
only durable project-specific rules; User Stories 1 and 2 require the same-change
and ownership answers to be findable from the constitution alone, which favors
fewer, denser sections over the current principle-by-principle expansion with
procedural cross-references.

**Alternatives considered**: (a) Keep the five-principle skeleton and only delete
skill sentences — rejected; the lifecycle and Principle V ceremony would still
present withdrawn stages as obligations (FR-019). (b) Reduce to a stub that
delegates everything to Spec Kit defaults — rejected; FR-002…FR-010 rules are
project-specific and have no stock owner. (c) Fold the routing text from
`AGENTS.md` in — rejected by FR-021's separation of router and governance owner.

## D6. Live vs historical reference discrimination

**Decision**: "Live reference" is decided by artifact class, and SC-003 is proved
by two complementary checks rather than one broad search. The current governance /
current-truth **textual** artifacts — `AGENTS.md`,
`.specify/memory/constitution.md`, `docs/PRODUCT_CONTRACT.md`,
`docs/ARCHITECTURE.md`, `docs/CURRENT_STATE.md`, `docs/TEST_MATRIX.md`,
`docs/DEVELOPMENT.md`, `docs/adr/**` — must contain zero present-tense obligations
naming the deleted skills, overlay, stages, packages, or gates, which the V-2 /
T018 term search establishes. **Active workflow configuration**
(`.specify/workflows/**`) is established instead by the V-3 / T019 workflow-state
checks — supported-CLI registration state, repository-owned overlay-file absence,
and resolved layers — because a text-search regex over the whole workflow tree
would neither be the right instrument nor prove registration state. Historical
artifacts — `specs/001-truth-review-fixes/**`, `docs/migrations/**`, and
`specs/002-governance-simplification/**` (which records the withdrawal itself) —
may mention the machinery freely.

**Rationale**: SC-003 defines the exclusion explicitly, and User Story 3 /
FR-023 require history to stay intact. A path-class rule is mechanically
checkable, which keeps the check out of the interpretation territory that the
withdrawn skills occupied.

**Alternatives considered**: (a) Time-based rule ("mentions of removed machinery
are allowed if the file predates this change") — rejected; not mechanically
checkable. (b) Sanitizing historical mentions — rejected outright by FR-023.
(c) Allowing stale pointers in *any* doc because "docs are just summaries" —
rejected; a current doc that names a nonexistent skill file is a broken pointer
(FR-022) and misleads the next contributor.

## D7. Regression protection for the withdrawn/retained rules

**Decision**: This change adds no deterministic executable regression. The
governance rules it retains or withdraws are repository workflow/documentary
rules, a knowledge class for which executable regression is not applicable;
verification is mechanical and documented in [quickstart.md](./quickstart.md)
(text-search invariance, `specify workflow` state, path-scoped `git diff`). The
planner's default suite is used only as a non-change control (SC-005).

**Rationale**: FR-007 is scoped to "newly established **normative**
[invariant]" behavior "when practically testable"; no product or architecture
invariant is established here, so the rule does not fire. The retained honesty
rule (FR-008) is what stops the next change from converting that judgment into
an unearned `DIRECT` claim. The 1.1.0 alternative of reporting `BLOCKED` as a
separate completion stage is replaced by in-change honest gap reporting, per the
spec's edge case for non-deterministically-testable invariants.

**Alternatives considered**: (a) Add a Scala self-test asserting governance text
state — rejected by FR-024/FR-025 (planner must not gain governance concerns) and
FR-026. (b) Add a repository lint script or CI job — rejected as new machinery
(FR-026) and it would need a new owner. (c) Leave `BLOCKED` stage language in the
constitution — rejected; it is only satisfiable by the withdrawn stages
(FR-019/FR-016).

## D8. Pointer corrections (mechanical only)

**Decision**: Two pointer edits, nothing else in the docs set changes:
`AGENTS.md` drops its skill-routing paragraph and keeps a thin route to the
constitution, active change, canonical owners, and `docs/DEVELOPMENT.md`;
`docs/TEST_MATRIX.md` §Coverage gaps replaces the
`../.agents/skills/invariant-promotion/SKILL.md` citation with
`../.specify/memory/constitution.md` (the file's existing relative-path
convention), keeping the same sentence's meaning (decide permanence first, prefer
extending an existing contract test). `docs/PRODUCT_CONTRACT.md:11`,
`docs/CURRENT_STATE.md:57`, and `docs/TEST_MATRIX.md:18/160` already point at the
constitution and need no edit. No coverage classification, `PC-*` mapping, test
name, or architecture statement changes.

**Rationale**: FR-021/FR-022 and the SC-003 carve-out require pointer repair
without semantic drift. Because no matrix row's classification changes, the
same-change traceability rule is satisfied by the pointer fix itself rather than
by a re-audit of the matrix.

**Alternatives considered**: (a) Delete the whole §Coverage gaps sentence —
rejected; it deletes useful guidance unrelated to the deleted skill. (b) Re-audit
matrix classifications "while we're in the file" — rejected as scope creep
(1.1.0 §Scope discipline) and a violation risk against FR-024/FR-022.

## D9. Retained useful structure and non-scope guards

**Decision**: Keep `docs/PRODUCT_CONTRACT.md`, `docs/ARCHITECTURE.md`,
`docs/CURRENT_STATE.md`, `docs/TEST_MATRIX.md`, `docs/adr/**`,
`docs/DEVELOPMENT.md`, a thin `AGENTS.md`, and
`.specify/memory/constitution.md` in their current locations, and keep the
`docs/migrations/**` files untouched. `trail-plan.scala` receives no edit at all;
the mechanical-stale-reference allowance in FR-025 is exercised by nothing in the
planner, so the expected planner diff is zero (confirmed: a search of
`trail-plan.scala` for the deleted-machinery terms returns no matches).

**Rationale**: FR-020/FR-023/FR-024/FR-025 pin the structure and the non-scope;
leaving the file set stable is what makes the "governance-only" claim verifiable
by path-scoped diff instead of by assertion.

**Alternatives considered**: (a) Consolidate the docs set further (e.g. merge
`CURRENT_STATE.md` into the ADRs) — rejected: out of scope and would rewrite
canonical owners this change must not touch. (b) Archive the migrations docs under
`specs/` — rejected: they are the rationale trail for the ownership model and
moving them would create churn in historical records.

## D10. Completion and convergence definition for the simplified process

**Decision**: Completion in the 2.0.0 text is: the contributor's direct use of
the stock sequence — `/speckit.specify` → `/speckit.clarify` (when needed) →
`/speckit.plan` → `/speckit.checklist` (when useful) → `/speckit.tasks` →
`/speckit.analyze` → `/speckit.implement` → `/speckit.converge` — plus ordinary
review/commit/PR as appropriate; completion additionally requires implementation,
tests, canonical owners, and traceability to be mutually consistent, and
convergence to report no remaining gaps. Small mechanical/documentation changes
may use a shorter stock path (retained from 1.1.0 with the same guard that it
must not alter product semantics, architecture, safety, executable traceability,
or established/rejected decision state).

**Rationale**: FR-009/SC-001 require exactly this. The spec disclaims any claim
that the bundled automation graph contains every step, so the text is written as
contributor-run stock commands. Keeping the shorter-workflow allowance avoids
making a governance typo change cost a full cycle, and it has no dependency on
deleted machinery.

**Alternatives considered**: (a) Keep a mandatory independent-review stage as
"cheap insurance" — rejected by FR-015; the spec keeps review permitted
case-by-case, which is stated as permission, not obligation. (b) Make
`/speckit.checklist` mandatory — rejected by FR-018. (c) Bind completion to
`specify workflow run speckit` — rejected: the base workflow ends at `implement`
and the withdrawn overlay was what extended it, so binding completion to the run
would either resurrect the overlay or understate the required steps.

## Resolved open questions

| Question from spec/Technical Context | Resolution |
|---|---|
| Which references are "live" and must go? | D1, D6 |
| Which constitution version step, and how is it recorded? | D2 |
| How is the overlay withdrawn so SC-004 passes? | D3 |
| Which skill rules survive, and where do they live? | D4, D5 |
| Does this change need a regression test? | D7 |
| Which doc edits are mechanical-only? | D8 |
| Is any planner change permitted? | D9 (none) |
| What does completion mean after simplification? | D10 |
