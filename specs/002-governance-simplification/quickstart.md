# Quickstart: Validate the Simplified Governance

Phase 1 validation guide for [plan.md](./plan.md). It proves the eight success
criteria mechanically; it is not an implementation reference. Clause IDs come
from [contracts/constitution-rules.md](./contracts/constitution-rules.md),
[contracts/machinery-removal.md](./contracts/machinery-removal.md), and
[contracts/preservation-scope.md](./contracts/preservation-scope.md); the
artifact inventory is in [data-model.md](./data-model.md).

## Prerequisites

- Repository root: `/Users/do/git/trail-plan` (all commands run here).
- Spec Kit CLI available and reporting `specify 1.0.3` (verify: `specify
  --version`); the bundled `speckit` workflow is installed.
- `git` and `rg` on `PATH`.
- A clean-enough work tree to read `git diff -- <path>` for this change, plus a
  recorded pre-change baseline:

  ```bash
  BASE=$(git rev-parse HEAD)                      # commit before implementation
  git status --porcelain                         # expect only specs/002 plan-phase artifacts
  ```

- Full-suite control run needs the local Valhalla endpoint used by
  `docs/DEVELOPMENT.md` (`http://localhost:8002`). It is the planner run, not a
  governance test; see V-6 for the fallback when the endpoint is unavailable.

## V-1 · Contributor path is stock commands (SC-001, `GOV-R-08`, `GOV-X-21`)

Read **only** `AGENTS.md` and `.specify/memory/constitution.md` and answer: "what
is required to complete a new substantive change?"

```bash
rg -n 'speckit\.' AGENTS.md .specify/memory/constitution.md
ls .opencode/commands                       # stock commands actually installed
```

Expected: the constitution names the direct sequence
`/speckit.specify → /speckit.clarify (when needed) → /speckit.plan →
/speckit.checklist (when useful) → /speckit.tasks → /speckit.analyze →
/speckit.implement → /speckit.converge` plus ordinary review/commit/PR, and
`AGENTS.md` adds no extra completion step. Every named command exists in
`.opencode/commands/`. No step says "load a skill", "run the project workflow",
"produce `promotion/`", or "clear the release gate".

## V-2 · Zero live *textual* references to deleted machinery (SC-003 textual half, `GOV-X-15`, `GOV-N-*`)

This is the live **textual**-reference check over the current governance /
current-truth artifact set. It does not inspect active workflow configuration;
that is V-3's job. SC-003 is established **jointly by V-2 + V-3**: V-2 for the
textual artifacts, V-3 for the active workflow configuration.

```bash
rg -n -i \
  'invariant-promotion|test-traceability-sync|\.agents/skills|trail-plan-speckit-completion|workflow overlay|release[ -]?gate|promotion/|mandatory independent.review|traceability-verification|executable-regression-verification' \
  AGENTS.md .specify/memory/constitution.md docs/PRODUCT_CONTRACT.md \
  docs/ARCHITECTURE.md docs/CURRENT_STATE.md docs/TEST_MATRIX.md \
  docs/DEVELOPMENT.md docs/adr
```

Expected: **no matches** in the current governance / current-truth textual set.
`.specify/workflows/**` is deliberately outside this command's scope: active
workflow configuration is verified by V-3 (supported-CLI state, overlay-file
absence, and resolved layers), and V-2 + V-3 together establish SC-003.

Then confirm the historical carve-out is *untouched*, not cleaned:

```bash
rg -l -i 'invariant-promotion|promotion/|release[ -]gate' \
  specs/001-truth-review-fixes docs/migrations
```

Expected: matches still present (User Story 3). If a match appears in V-2's
first command, fix it as a mechanical pointer correction only — never by editing
a coverage classification, `PC-*` mapping, or technical statement (FR-022).

## V-3 · Skills and overlay are gone through the supported mechanism (SC-003 active-configuration half, SC-004, `GOV-X-01…X-10`)

This is the active workflow configuration check that V-2's text search does not
perform; V-2 + V-3 together establish SC-003.

```bash
ls .agents/skills                          # expected: no Trail Plan SKILL.md dirs
ls .specify/workflows/overlays 2>/dev/null # expected: no repository-owned overlay files
specify workflow overlay list speckit      # expected: no overlays
specify workflow resolve speckit           # expected: layers = [base] only
specify workflow list                      # expected: "Full SDD Cycle (speckit) v1.0.0"
git diff --name-only "$BASE" -- .specify/templates .specify/scripts \
  .opencode/commands .specify/workflows/speckit   # expected: empty (FR-014)
```

Expected: `resolve` attributes `converge` to neither layer — the contributor runs
`/speckit.converge` directly (D3). Disabling the overlay instead of removing it
fails `GOV-X-06`/`GOV-X-08`.

## V-4 · Update rule is findable without a skill (SC-002, `GOV-R-04…R-07`, `GOV-X-17/18`)

From the constitution alone, answer (a) which document owns a kind of truth;
(b) what must move when a normative clause changes; (c) when a new invariant
needs executable protection; (d) when a `DIRECT` claim is dishonest — without
opening any skill file.

```bash
git diff "$BASE" -- .specify/memory/constitution.md    # qualitative read of the rewrite
```

Expected: all four answers sit in the constitution. Per SC-002 the rewrite is
assessed qualitatively — materially simpler than the current version, containing
only the durable rules retained by FR-002 through FR-010, with no withdrawn
machinery left as a live obligation; no numeric line-count budget applies.
`AGENTS.md` states no rule of
its own and still routes to the constitution, the active change, the canonical
owners, and `docs/DEVELOPMENT.md`.

## V-5 · Docs semantics preserved (FR-022/FR-024, `GOV-P-09/10`, `GOV-X-19/20`)

```bash
git diff "$BASE" -- docs/PRODUCT_CONTRACT.md docs/ARCHITECTURE.md \
  docs/CURRENT_STATE.md docs/adr docs/DEVELOPMENT.md   # expected: empty
git diff "$BASE" -- docs/TEST_MATRIX.md                 # expected: pointer line only
```

Expected: the only `docs/TEST_MATRIX.md` hunk replaces the
`../.agents/skills/invariant-promotion/SKILL.md` citation in §Coverage gaps with
`../.specify/memory/constitution.md` (the file's existing relative-path
convention). Any `DIRECT`/`PARTIAL`/`MISSING`, `PC-*`, or test-name
change is a scope defect.

## V-6 · Planner behavior unchanged (SC-005, `GOV-P-06/07/08`)

```bash
git diff --name-only "$BASE" -- trail-plan.scala        # expected: empty
```

Control run with the full-log discipline from `docs/DEVELOPMENT.md` — the
complete stdout/stderr is retained in a temporary log and the planner exit
status is preserved; the `SELF-TESTS` summary is only a post-run display, and
the saved full log is printed if the run fails or the summary is missing:

```bash
log=/tmp/trail-plan.$$.log; chmod +x ./trail-plan.scala && ./trail-plan.scala --input ./trails --output day --valhalla-url http://localhost:8002 >"$log" 2>&1; rc=$?; grep -E '^SELF-TESTS' "$log" || true; if [ "$rc" -ne 0 ] || ! grep -q '^SELF-TESTS' "$log"; then cat "$log"; fi; exit "$rc"
```

Expected: `rc` is 0 and the `SELF-TESTS: <n> passed, <m> failed` line in the
full log matches the pre-change baseline recorded before implementation, with 0
failures. If Valhalla is unavailable, fall back to the diff being empty plus the
documented pre-change run output; do **not** substitute a filtered or partial
log for the full run log, and do not rerun solely to recover output hidden by
filtering.

## V-7 · History intact (SC-006, `GOV-P-01…P-05`)

```bash
git diff --name-status "$BASE" -- specs/001-truth-review-fixes docs/migrations
ls specs/001-truth-review-fixes/promotion \
  specs/001-truth-review-fixes/checklists/release-gate.md
```

Expected: empty diff, and both historical artifacts still present.

## V-8 · No replacement machinery (SC-007, `GOV-P-12`, `GOV-N-06`)

```bash
git diff --name-status "$BASE" | grep -v '^.\s*specs/002-governance-simplification/'
```

Expected additions outside the change record: none. Expected deletions:
`.agents/skills/invariant-promotion/SKILL.md`,
`.agents/skills/test-traceability-sync/SKILL.md`,
`.specify/workflows/overlays/speckit.yml`,
`.specify/workflows/overlays/speckit/trail-plan-speckit-completion.yml`.
Expected modifications (allowlist): `.specify/memory/constitution.md`,
`AGENTS.md`, `docs/TEST_MATRIX.md`. The CLI-managed
`.specify/workflows/workflow-registry.json` may also appear as a modification
only if the supported `specify workflow overlay remove` command actually
produced a registry edit (registry state is conditional: a CLI-produced
registry diff is permitted, and absence of a registry diff is equally valid —
no diff is required, and this change's implementation produced none). No
other `.specify/workflows/**` path may be modified, and
`.specify/workflows/speckit/workflow.yml` must stay untouched (FR-014). Anything
else is out of scope.

## V-9 · Convergence reports no gap (SC-008, `GOV-V-01/02`)

```bash
/speckit.analyze   # spec ↔ plan ↔ tasks consistency
/speckit.converge  # completion against the active change artifacts
```

Expected: `/speckit.converge` finds every FR-001…FR-027 obligation satisfied by
the retained governance (no clause depends on a deleted artifact), the
constitution footer reads `Version: 2.0.0` with the MAJOR amendment rationale in
its Sync Impact Report, and no remaining gap is reported.

## Result summary template

| Check | Contract | Success criterion | Result |
|---|---|---|---|
| V-1 stock path only | `GOV-R-08`, `GOV-X-21` | SC-001 | ☐ |
| V-2 zero live textual refs | `GOV-X-15`, `GOV-N-01…04` | SC-003 (textual half; with V-3) | ☐ |
| V-3 skills + overlay gone | `GOV-X-01…X-10` | SC-003 (active-configuration half; with V-2), SC-004 | ☐ |
| V-4 update rule locatable | `GOV-R-04…R-07`, `GOV-X-17/18` | SC-002 | ☐ |
| V-5 docs semantics intact | `GOV-X-19/20`, `GOV-P-09/10` | SC-003 | ☐ |
| V-6 planner unchanged | `GOV-P-06/07/08` | SC-005 | ☐ |
| V-7 history intact | `GOV-P-01…P-05` | SC-006 | ☐ |
| V-8 no new machinery | `GOV-P-11/12`, `GOV-N-06` | SC-007 | ☐ |
| V-9 convergence clean | `GOV-V-01/02`, `GOV-R-09` | SC-008 | ☐ |
