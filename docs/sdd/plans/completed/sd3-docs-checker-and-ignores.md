# Plan — SD3 Documentation checker and search ignores

- State: `Concluído`
- Date: 2026-10-02
- Owner: `sdd` — shared agent workflow maintenance.
- Code affected: `tools/check-docs.mjs`, `tools/check-docs.test.mjs`, the wire export tools, `tools/package.json` description, `.gitignore`, `.ignore`. No app, server or infra code.
- Prerequisites: [SD2](../completed/sd2-documentation-authority.md) implemented, either in `completed/` or in `pending_manual_validation/` with its automated validation passed. The checker must pass on the repository SD2 leaves behind.

## Authorization gate

Implementation starts only after explicit approval identifying this file:

> Aprovo o plano `docs/sdd/plans/sd3-docs-checker-and-ignores.md`. Implemente o plano aprovado.

One agent executes it. If a check cannot be made reliable without a product or policy decision, drop that check, record why, and continue.

## Objective

Make the SD2 rules hold without relying on agent memory:

- a read-only checker fails when a current-state document can contradict its owner;
- the ignore files stop hiding tracked skill trees from git while hiding mirror copies from search.

## Discovery evidence

Verified at `5e41fcf`.

- **Silent sync failure.** `git check-ignore -v .agents/skills/new-skill/SKILL.md` → `.gitignore:28:.agents/`; `.hermes/` is on line 29. Both trees are tracked (136 files each), but a *new* file synced into them is skipped by `git add`. `check-skills.mjs` still passes, because it reads the working tree.
- **Worktrees.** `.claude/worktrees/` (two full checkouts) is excluded only in this clone's `.git/info/exclude`.
- **Search exposure.** A Claude Code Grep for a skill sentence returned the `.grok/` and `.claude/` copies. `.agents/` and `.hermes/` were hidden by `.gitignore`, and the worktrees by `.git/info/exclude`. Each search pays for duplicates, and the result depends on accidental ignore rules.
- **Gold list copy.** `tools/export-stitch.mjs` exports `DARK_SCREENS` and `LIGHT_SCREENS`, and its CLI runs only behind an `isMain` guard (line 324), so importing it has no side effects. After SD2, `docs/qa/README.md` owns the human-readable gold inventory. The script map is an unavoidable executable copy.
- **No CI.** There is no `.github/workflows/`. Checks run locally, like `check-skills.mjs`.
- **Orphaned wire tools.** SD2 deletes `wires/` (owner, 2026-10-02). `tools/export-wires.mjs`, `tools/check-wires.mjs` (imports `export-wires.mjs`) and `tools/export-painel-wires.mjs` only serve it. No `tools/package.json` script references them. Its description says "Stitch and wireframes".

## Implementation scope

### 1. `tools/check-docs.mjs`

- Dependency-free Node and read-only, in the style of `tools/check-skills.mjs`.
- Exits 0 on success and non-zero with one line per finding (`file:line check message`).
- Takes `--root <path>` for fixtures.

**Live set** (documented in the script header):

- `AGENTS.md` and `README.md`;
- every `.md` under `docs/`, except:
  - `docs/*/plans/completed/**` and `docs/*/plans/cancelled/**`;
  - `docs/decisions/**` and `docs/*/adrs/**` (C5 only);
  - `docs/*/validation/**` (C1 only);
  - `docs/qa/_legacy/**`.

Skill trees are covered by `check-skills.mjs`.

**Checks:**

- **C1 — Links.** Every relative Markdown link in the live set and in `validation/` resolves to an existing file or directory.
  - Skipped: external URLs, pure anchors, code spans and fenced blocks.
  - Anchors inside existing files are not validated.
- **C2 — "No specification" claims.** A README whose text claims there is no specification (`Sem especifica`, `Sem specifications`, `no specification`, case- and accent-insensitive) fails when the sibling `specifications/` directory has a `.md` file.
- **C3 — Plan links in specs.** In `docs/*/specifications/*.md`, a link into any `plans/` path is allowed only under a `## Provenance` or `## Proveniência` heading.
- **C4 — Status copies.**
  - Provenance entries carry no status marker: a parenthetical or backticked status word (`Concluído`, `proposto`, `proposed`, `pendente`, `pending`, `aceito`, `accepted`), matched case-insensitively as a whole word. Entry titles are free text (an S15 title once read "Pending meal is not a skip").
  - In the live set, a line linking an ADR and containing `proposto`/`proposed`/`proposta` fails when the ADR's own status line says Accepted/Aceito.
  - A line linking a plan in `plans/completed/` and containing `aguardando aprovação`, `pending approval`, `pendente de aprovação`, `em implementação` or `not current behavior` fails.
  - Generic words in rules (e.g. "refeição pendente") are not matched.
- **C5 — ADR status.** Every file in `docs/decisions/` and `docs/*/adrs/` has a `- Status:` or `- Estado:` line within its first 10 lines.
- **C6 — README routing.** No `README.md` in the live set links an individual file inside `plans/completed/` or `plans/cancelled/`. A link to the directory is allowed.
- **C7 — Gold inventory.** The gold ids listed in `docs/qa/README.md` (the section SD2 defines) equal the keys of `DARK_SCREENS` and `LIGHT_SCREENS`, imported from `tools/export-stitch.mjs`. Missing or extra ids are reported per theme.

The script never writes, normalizes, downloads or fixes. Matching must not depend on line endings.

### 2. `tools/check-docs.test.mjs`

- `node:test` with disposable fixture roots: one passing and one failing case per check (C1–C7).
- The C4 negative cases include rule text containing "pendente" and "proposed memory facts".
- CLI exit codes and `--root`.
- A test that fixture bytes are unchanged after both passing and failing runs.

### 3. `.gitignore`

- Remove `.agents/` and `.hermes/`.
- Add `.claude/worktrees/`.
- Change nothing else.
- After the edit, `git status --short` must show no new untracked files under `.agents/` or `.hermes/`. If any appear, stop and report them; they may be unsynced local skill files.

### 4. `.ignore` (ripgrep-format, does not affect git)

```text
.grok/skills/
.hermes/skills/
.claude/skills/
.claude/worktrees/
```

`.agents/skills/` stays searchable as the single editing source used by SD1. Agents load skills from their own directories by path, not through search.

### 5. Wire the checker into the workflow

- **`AGENTS.md` Skills/How to work:** after a documentation change, run `node tools/check-docs.mjs` alongside `node tools/check-skills.mjs`.
- **`docs/sdd/README.md` Completion:** the same command as a step.
- **`docs/sdd/templates/plan.md` Validation:** list it for documentation deliveries.

### 6. Remove wire tooling

- Delete `tools/export-wires.mjs`, `tools/check-wires.mjs` and `tools/export-painel-wires.mjs`.
- Set the `tools/package.json` description to the Stitch visual QA tools. Dependencies stay, because `export-stitch.mjs` uses Playwright and pngjs.
- `git grep` must find no live reference to the three scripts outside completed plans and ADR bodies.

### 7. Run on the repository

`node tools/check-docs.mjs` must pass on `master` after SD2.

A finding caused by documentation that SD2 missed is fixed here only if it is a broken link, a status copy or a README routing link: no rule text and no spec rewrite. Each fix is recorded in Results. Anything else stops the delivery and goes back to the owner.

## Out of scope

- Specification rewrites and rule changes, which belong to SD2.
- Plan, ADR body or history edits.
- CI workflows, git hooks or pre-commit tooling.
- Anchor validation, semantic freshness checks and spell checks.
- Changing which skill trees exist or how they are synchronized.
- Agent configuration outside the repository.
- RAG or indexing.

## Validation

**Automated (agent):**

1. `node tools/check-docs.mjs` passes on the repository.
2. `node --test tools/check-docs.test.mjs` passes, every check having a passing and a failing fixture.
3. `node tools/check-skills.mjs`, `node --test tools/check-skills.test.mjs` and `node --test tools/verify-stitch.test.mjs` pass, and `node tools/check-stitch.mjs` still runs.
4. `git status --short` is clean apart from the plan's files after the `.gitignore` change.
5. `git check-ignore .agents/skills/x/SKILL.md` returns nothing.
6. `git check-ignore .claude/worktrees/x` matches `.gitignore`.
7. `rg -l "<a sentence from dieta-bot-android-decision/SKILL.md>"` returns only the `.agents/skills/` copy.

**Manual (owner):**

1. In a new Claude Code session, a Grep for the same sentence returns only the `.agents/skills/` copy, and the project skills (`dieta-bot-*`) are still listed.
2. Optionally, the same check in grok-cli and Hermes. A tool that ignores `.ignore` is recorded in Results and does not block completion.

## Acceptance criteria

- The checker and its tests exist, pass, and are read-only.
- A new file under `.agents/` or `.hermes/` is no longer ignored by git.
- Mirror skill trees and worktrees are hidden from ripgrep-based search; `.agents/skills/` is not.
- `AGENTS.md`, the SDD Completion step and the plan template name the checker.
- No wire tooling remains in `tools/`.

## Results

Implemented 2026-10-02 on branch `feat/sd3-docs-checker`, from `master` @ `3300057`. Automated validation passed; the owner confirmed the manual check on 2026-10-02.

### Checker

- `tools/check-docs.mjs`: read-only, no dependencies beyond `tools/export-stitch.mjs` (C7); `--root <path>`; output `file:line Cn message`; exit 0/1, 2 if it cannot finish.
- Reuses `proseOnly` from `tools/check-skills.mjs`, now exported (no behavior change).
- `docs/sdd/templates/**` is excluded from the live set: its links are placeholders by design.
- C4 deviation from the plan text, needed to avoid false positives:
  - "proposed" is matched only in the 40 characters right after an ADR link, not anywhere on the line. A rule such as `proposed memory facts ([ADR-001](…))` is not a status copy.
  - Provenance status markers are matched on raw lines, because the prose filter removes backticks.
- On the repository: 35 live files and 38 link-checked files, 0 findings.
- On the pre-SD2 tree (`git archive 51cdded`), as evidence that it catches the original problems:
  - 3 C1, 2 C2, 132 C3, 5 C4, 10 C5, 198 C6;
  - 1 C7: `pngjs` is absent in the archive, so the gold map could not load there.
  - The 5 C4 hits are the "proposto" ADR-024/ADR-028 copies that SD2 removed.
- Known limit: C4 does not see a banner that calls behavior pending without linking a completed plan (the old `chat.md` CP2 banner linked a spec). Single writer and the spec template carry that rule.

### Tests

`node --test tools/check-docs.test.mjs`: 12/12. Each of C1–C7 has a passing and a failing fixture. The negative C4 cases are "refeição pendente", "proposed memory facts", "independente", a proposed ADR that really is proposed, and a free-text "Pending meal" title. The tests also cover CRLF, CLI exit codes with `--root`, unknown options, and unchanged fixture bytes after passing and failing runs.

### Ignores

- `.gitignore`: dropped `.agents/` and `.hermes/`, added `.claude/worktrees/`.
  - `git status` showed no new untracked files under `.agents/` or `.hermes/`.
  - `git check-ignore .agents/skills/x/SKILL.md` matches nothing.
  - `.claude/worktrees/x` matches `.gitignore:27`.
  - SD2's delivery had hit the old rule: `git add` refused the two trees.
- `.ignore` hides `.grok/skills/`, `.hermes/skills/`, `.claude/skills/` and `.claude/worktrees/`.
  - `rg --hidden -l "<dieta-bot-android-decision sentence>"`: only `.agents/skills/…` (4 copies with `--no-ignore-dot`).
  - The Claude Code Grep tool in this session returned the same single copy.

### Wire tooling

Deleted `tools/export-wires.mjs`, `tools/check-wires.mjs` and `tools/export-painel-wires.mjs`. The `tools/package.json` description now names the Stitch golds; dependencies unchanged. `git grep` finds the three names only in completed plans and ADR bodies (history).

### Workflow

`node tools/check-docs.mjs` is now required in three places: `AGENTS.md` (How to work), SDD Completion step 6 (a failure blocks delivery) and the plan template's Validation section.

### Validation

- `node tools/check-docs.mjs`: pass.
- `node --test tools/check-docs.test.mjs`: 12/12.
- `node tools/check-skills.mjs`: pass. `node --test tools/check-skills.test.mjs`: 14/14.
- `node --test tools/verify-stitch.test.mjs`: 2/2. `node tools/check-stitch.mjs`: 62 golds (31 + 31).
- `git diff --check`: clean.

Manual (owner, confirmed 2026-10-02: a new Claude Code session listed the seven `dieta-bot-*` skills and Grep returned only `.agents/skills/dieta-bot-android-decision/SKILL.md`):

1. In a new Claude Code session, confirm that the project skills (`dieta-bot-*`) are still listed and that a Grep for a skill sentence returns only the `.agents/skills/` copy.
2. Optional: the same Grep in grok-cli and Hermes. A tool that ignores `.ignore` is recorded and does not block completion.
