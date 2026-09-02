# AGENTS.md

# Trail Plan

This repository uses GitHub Spec Kit for substantive change execution.

`.specify/memory/constitution.md` is the mandatory governance entry point. It
owns source precedence, canonical ownership, working rules, change lifecycle,
and completion criteria. This file is a repository router only and owns no
governance rules.

## Repository navigation

Before substantive work:

1. read `.specify/memory/constitution.md`;
2. work through the active `specs/<change>/` artifacts when a Spec Kit change
   exists;
3. read the canonical owners the constitution requires:
   `docs/PRODUCT_CONTRACT.md`, `docs/ARCHITECTURE.md`, `docs/CURRENT_STATE.md`,
   `docs/TEST_MATRIX.md`, `docs/adr/`, and `trail-plan.scala`.

Historical Scala snapshots, archived change artifacts, source-history comments,
and prior chat context are not current authority.

## Skill routing

`.agents/skills/invariant-promotion/SKILL.md` and
`.agents/skills/test-traceability-sync/SKILL.md` remain the project mechanisms
for newly established durable knowledge and traceability synchronization; the
constitution defines when each applies.
