# Contract: Simplified Constitution Rule Set

**Change**: `002-governance-simplification` · **Plan**: [../plan.md](../plan.md) ·
**Research basis**: D2, D4, D5, D7, D10

Scope: the clauses the rewritten
[`.specify/memory/constitution.md`](../../.specify/memory/constitution.md)
(2.0.0) MUST contain, and the negative obligations it MUST NOT violate. This is
a contract on document content and satisfiability, not a draft of the document
(wording is produced during implementation under FR-001…FR-010).

## Retained-rule clauses

| ID | Clause (normative requirement on the text) | Spec source | Verification |
|---|---|---|---|
| `GOV-R-01` | States canonical ownership and source precedence: one owner per kind of knowledge, references must not silently redefine, and historical Scala snapshots / archived change artifacts / prior chat context are not current authority. | FR-002 | text presence; router does not restate it |
| `GOV-R-02` | States a no-silent-weakening rule covering established product, safety, exactness, representation, and fail-closed behavior, with intentional change requiring an explicit active-change statement plus approval. | FR-003 | text presence; no product semantics restated |
| `GOV-R-03` | States the separation: active `specs/<change>/` describe changes, canonical docs describe current truth, a completed change record never becomes a competing current source. | FR-004 | text presence |
| `GOV-R-04` | Requires the affected canonical owner to be updated **in the same change** when durable product/architecture truth changes. | FR-005, US-2 | text presence; no separate procedural document required to discover it |
| `GOV-R-05` | Requires `docs/TEST_MATRIX.md` to be updated **in the same change** when normative contract/architecture clauses or their regression coverage change. | FR-006, US-2 | text presence; no skill load step |
| `GOV-R-06` | Requires newly established normative invariants to carry deterministic executable regression protection **when practically testable**, with honest in-change reporting of a testability gap. | FR-007, edge case | text presence; no separate verification stage |
| `GOV-R-07` | Prohibits claiming `DIRECT` coverage when the default-running suite would stay green after that property regressed; defers coverage-class definitions to `docs/TEST_MATRIX.md`. | FR-008 | text presence; class definitions not duplicated |
| `GOV-R-08` | Defines completion as the contributor's direct use of the stock `/speckit.*` commands through `/speckit.converge`, with implementation, tests, canonical owners, and traceability mutually consistent and convergence reporting no remaining gaps. | FR-009 | text presence; command list matches installed stock commands |
| `GOV-R-09` | Retains a short amendment section: explicit rationale, human approval, version record (SemVer classes for governance). | FR-010 | text presence; header carries Sync Impact Report |
| `GOV-R-10` | Is materially simpler than 1.1.0 and states only durable, project-specific governance. | FR-001, SC-002 | qualitative review: contains only the durable rules retained by FR-002…FR-010 and no withdrawn machinery as a live obligation; no numeric line-count budget applies |

## Negative clauses

| ID | Clause | Spec source | Verification |
|---|---|---|---|
| `GOV-N-01` | No obligation in the text can be satisfied only by the deleted skills, overlay, gates, or `promotion/` evidence packages. | FR-019 | `GOV-X-*` checks applied to the constitution path |
| `GOV-N-02` | No mandatory independent-review lifecycle stage and no separate mandatory executable-regression/traceability verification stages. | FR-015, FR-016 | text absence; only permissive case-by-case wording allowed for independent review |
| `GOV-N-03` | No mandatory `promotion/` package and no mandatory custom release-gate checklist; Spec Kit checklists may be named as optional. | FR-017, FR-018 | text absence |
| `GOV-N-04` | No Trail Plan-owned workflow overlay is referenced or assumed; bundled/base Spec Kit material is described as untouched. | FR-009, FR-013, FR-014 | text absence + `machinery-removal.md` |
| `GOV-N-05` | No product requirement, architecture claim, decision status, or ADR rationale is moved into the constitution. | FR-024 | diff scope check in `preservation-scope.md` |
| `GOV-N-06` | Introduces no new governance artifact, skill, gate, or workflow abstraction. | FR-026 | governance entry-point set is a subset of before (SC-007) |

## Answer-locatability requirements (from User Story 2's independent test)

From the constitution alone, without opening any skill file, a maintainer must be
able to answer: (a) which document owns a given kind of truth → `GOV-R-01`;
(b) what must be updated when a normative clause changes → `GOV-R-04` +
`GOV-R-05`; (c) when a new invariant needs executable protection → `GOV-R-06`;
(d) when a "direct" coverage claim is dishonest → `GOV-R-07`.

## Version and effectiveness

| ID | Clause | Spec source |
|---|---|---|
| `GOV-V-01` | The rewrite is recorded as a MAJOR governance amendment `1.1.0 → 2.0.0` with rationale in the leading Sync Impact Report and an updated footer line. | D2, Assumptions |
| `GOV-V-02` | The change proceeds under 1.1.0; the simplified rules govern future work only upon completion of this change. | FR-027 |
