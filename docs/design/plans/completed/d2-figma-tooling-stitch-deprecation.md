# Plan — D2 Figma tooling, Figma review gate and Stitch deprecation

- Status: Concluído
- Date: 03/10/2026
- Owning context: `design`
- Affected code:
  - tooling: `tools/export-figma.mjs` (new), `tools/check-figma.mjs` (new), `tools/gen-tokens.mjs` (new), `tools/diff-gold.mjs`, `tools/check-docs.mjs` + `tools/check-docs.test.mjs` (C7);
  - generated mirrors: `docs/design/tokens.json` (new), `docs/tokens.md`;
  - docs and rules: `docs/qa/README.md`, `AGENTS.md`, `docs/sdd/README.md` + `docs/sdd/templates/figma-flow.md` (new), `docs/stitch/README.md`, `docs/README.md`;
  - skills: the new `dieta-bot-figma` skill in the four skill trees (`.agents`, `.grok`, `.hermes`, `.claude`).

  No `apps/` or `server/` code.
- Prerequisites: [D1](d1-figma-file-foundation.md) `Concluído`.
- Figma MCP budget: ≤ 10 calls (token read-back and one export dry run).

Authorization, lifecycle, cancellation and deferral: `docs/sdd/README.md`. Approval: `Aprovo o plano docs/design/plans/d2-figma-tooling-stitch-deprecation.md. Implemente o plano aprovado.`

## Objective

Give the repository what it needs to treat Figma as the UI source ([ADR-031](../../adrs/ADR-031-figma-source-of-truth.md)): gold export, token mirror, a gold inventory with two sources during the migration, the Figma review gate in the rules, a Figma skill and the formal freeze of Stitch.

## ⛔ Owner step — Figma access token (once)

The REST image export needs a personal access token. The agent never sees it typed in chat and never prints it.

1. In Figma desktop: avatar (top left) → **Settings** → **Security** → **Personal access tokens** → **Generate new token**.
2. Name: `dieta-bot-export`. Expiration: the longest offered. Scopes: **File content: Read-only** only.
3. Copy the token, then in PowerShell (user scope, not the repository):

   ```powershell
   [Environment]::SetEnvironmentVariable("FIGMA_TOKEN", "<paste here>", "User")
   ```

4. Close and reopen the terminal and Claude, then tell the agent "token configurado".

## Scope

1. **Gold export** — `tools/export-figma.mjs`:
   - `FIGMA_FILE_KEY` plus maps `DARK_FRAMES` / `LIGHT_FRAMES` (`gold id → node id`), empty until a flow plan fills them;
   - it calls `GET /v1/images/{key}?ids=…&format=png&scale=2` with the `FIGMA_TOKEN` header and writes `docs/qa/figma/{dark,light}/<id>.png`;
   - `--only <ids>`, and the same noise filter as `export-stitch.mjs` (a PNG with < 0.05 % changed pixels goes back to its git version);
   - it fails without printing the token when the variable is missing or the response is 403.
2. **Gold check** — `tools/check-figma.mjs`: every id mapped in `export-figma.mjs` has both PNGs, each 780 px wide.
3. **Inventory with source** — `docs/qa/README.md` § Golds:
   - each id gets a source (`stitch` or `figma`); all start as `stitch`;
   - a new folder line for `figma/{dark,light}/`;
   - the visual loop resolves the gold folder from the id's source.

   `tools/check-docs.mjs` C7 becomes: the inventory equals `stitch map ∪ figma map`, and each id is present in the map of its declared source. Tests added to `check-docs.test.mjs`. `tools/diff-gold.mjs` reads the gold from the declared source.
4. **Token mirror:**
   - the agent reads the Figma variables (one MCP call) and writes `docs/design/tokens.json` (collections, modes, values, code syntax);
   - `tools/gen-tokens.mjs` renders from it the "Aero" section of `docs/tokens.md`, with a header saying it is generated from Figma `Design` and never edited by hand;
   - the current values move under a "Material 3 Expressive — flows not migrated" section, which [A45](../../../android/plans/completed/a45-remove-material3.md) deletes;
   - `docs/tokens.md` stays the single repository owner of token values, as a mirror of Figma.
5. **Rules:**
   - `AGENTS.md`:
     - Visual QA: two gold sources during the migration, the `docs/qa/figma/` folder law, export commands;
     - "Stitch gate": Stitch is frozen and the Figma review gate applies (link to [plans/README.md § Figma review gate](../README.md#figma-review-gate));
     - Tokens: values mirror Figma `Design`;
     - Skills: add `dieta-bot-figma`.

     Live stack and Material lines stay until A45.
   - `docs/sdd/README.md`: new context type "design source", a "Gate Figma" section, and the Stitch gate marked frozen ("no new ST gates"). New template `templates/figma-flow.md`, derived from D3.
   - `docs/stitch/README.md`: purpose rewritten in the present (frozen reference for flows not yet migrated; no new gates).
   - `docs/README.md`: matrix row for `stitch` updated the same way.
6. **Skill `dieta-bot-figma`** (four identical trees, `node tools/check-skills.mjs`):
   - read the design README, the ADRs and the flow plan;
   - `whoami` first, and report the remaining budget;
   - read before write, using the `figma-use`, `figma-generate-library` and `figma-generate-design` MCP skills;
   - keep a state ledger in the scratchpad;
   - readability rule and naming, Light build plus Dark clone through the variable mode;
   - feature parity (code and specs first, gold elements without code dropped);
   - one screenshot per composed frame, owner review, export with `tools/export-figma.mjs`;
   - never print `FIGMA_TOKEN`;
   - known limits: unpublished Community libraries are invisible to the MCP; `createAutoLayout` frames come with a white fill that must be cleared.

   `dieta-bot-stitch` stays until [D8](d8-archive-stitch.md). The skills that name Stitch golds as the comparison target (`dieta-bot-android-ui`, `dieta-bot-android-visual`, `dieta-bot-android-qa`, `screenshot-testing`) change to "the gold from the id's source in the inventory", in the four trees.

## Out of scope

- Any Figma screen (D3–D7) or app code (A39+).
- Retiring Stitch files and tools (D8).

## Validation

1. `node tools/export-figma.mjs --only <one pilot frame> --dry-run`: authenticates and resolves the frame without writing to the repository (the PNG goes to the scratchpad).
2. `node tools/check-figma.mjs`, `node tools/check-stitch.mjs` and `node --test tools/check-docs.test.mjs` pass.
3. `node tools/check-docs.mjs` and `node tools/check-skills.mjs` pass.
4. `grep` finds no `FIGMA_TOKEN` value in the repository or in logs.

## Results

Implemented and closed `Concluído` on 2026-10-04 after the owner's named approval. The plan has no manual validation item.

### Owner step

`FIGMA_TOKEN` was already set in the user environment (checked as set/unset only, never printed).

### Figma MCP budget

3 calls of the 10 budgeted: 1 `whoami` (Figma Student, seat Full), 1 skill read (`figma-use`), 1 read-only `use_figma` (all variables with modes, values, descriptions, scopes and code syntax; text styles; frame ids on `Release 1`). The export dry run uses the REST API, not the MCP.

### Delivered

1. **Gold export** — `tools/export-figma.mjs`: `FIGMA_FILE_KEY`, empty `DARK_FRAMES` / `LIGHT_FRAMES`, `GET /v1/images/{key}?ids=…&format=png&scale=2` with the `X-Figma-Token` header into `docs/qa/figma/{dark,light}/<id>.png`, `--only`, and the noise filter of `export-stitch.mjs` (shared `pngDiff` and `NOISE_MAX_PCT`, own git lookup under `docs/qa/figma/`). Missing token, 403 and 404 fail with a fixed message and exit code 1, without the token. Addition beyond the plan text, needed by validation 1: `--dry-run [--out <dir>]` writes outside `docs/qa/` and accepts a raw node id, so the token and a frame can be checked before any gold is mapped.
2. **Gold check** — `tools/check-figma.mjs`: every id mapped in either theme must be mapped in both and have both PNGs, 780 px wide. With the empty maps it passes with 0 golds.
3. **Inventory with source** — `docs/qa/README.md`: each inventory line starts with its source (`stitch:` for all 31 ids), the `figma/{dark,light}/` folder line, the Figma export commands and a visual loop that resolves the gold folder from the source. `tools/check-docs.mjs` C7: inventory = stitch map ∪ figma map, each id in the map of its declared source, every line with a known source, no id listed twice; `parseGoldInventory` is exported and `tools/diff-gold.mjs` reads the gold from `docs/qa/<source>/`. Three C7 tests added or extended in `check-docs.test.mjs` (15 tests in total).
4. **Token mirror** — `docs/design/tokens.json` (Color 23 variables Light/Dark, Shape 11, Motion 6, 9 text styles). `tools/gen-tokens.mjs` renders the "Aero" section of `docs/tokens.md` between generated markers, with the "never edit by hand" header; `--check` reports drift without writing. The previous values moved under "Material 3 Expressive — flows not migrated" (`### Dark`, `### Light`, unchanged values).
5. **Rules** — `AGENTS.md` (Visual QA with two sources and the `docs/qa/figma/` folder law, export commands, Figma review gate, Stitch gate frozen, tokens mirror Figma, `dieta-bot-figma` in Skills; the splash and "Do not" lines point at the inventory gold), `docs/sdd/README.md` (context type "Fonte de design", "Gate Stitch" marked frozen, new "Gate Figma" section, template list), new `docs/sdd/templates/figma-flow.md` (derived from D3), `docs/stitch/README.md` (purpose, type, scope and boundaries rewritten as a frozen reference), `docs/README.md` (matrix rows for `design` and `stitch`, tokens row).
6. **Skills** — new `dieta-bot-figma` in the four trees; `dieta-bot-android-ui`, `dieta-bot-android-visual`, `dieta-bot-android-qa` and `screenshot-testing` now name the gold from the id's source in the inventory. `dieta-bot-stitch` unchanged. `StitchGoldTest` is app code and stays as is (no `apps/` change).

### Validation

1. `node tools/export-figma.mjs --only 9:2 --dry-run --out <scratchpad>` (the pilot `rascunho · Home · Light`): authenticated, resolved and wrote `node/9-2.png`, 780×2780, to the scratchpad; nothing under `docs/qa/`. Also: without `FIGMA_TOKEN` → "FIGMA_TOKEN is not set…", exit 1; with an invalid token → "Figma refused the token (403)…", exit 1.
2. `node tools/check-figma.mjs`: passed (0 golds mapped). `node tools/check-stitch.mjs`: 62 PNGs verified. `node --test tools/check-docs.test.mjs`: 15/15 pass. Also `node --test tools/check-skills.test.mjs tools/verify-stitch.test.mjs`: 16/16 pass, `node tools/diff-gold.mjs` (default ids, existing captures): all pass, `node tools/gen-tokens.mjs --check`: in sync.
3. `node tools/check-docs.mjs`: passed. `node tools/check-skills.mjs`: passed, 27 skills and 137 files in each tree; YAML front matter of every skill parses with `name` = folder.
4. `grep -rlF "$FIGMA_TOKEN"` over the repository (without `node_modules`, `build`, `.gradle`): 0 files. The token never appeared in command output.
