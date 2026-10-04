# Plan — D2 Figma tooling, Figma review gate and Stitch deprecation

- Status: Aguardando aprovação
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

Give the repository what it needs to treat Figma as the UI source ([ADR-031](../adrs/ADR-031-figma-source-of-truth.md)): gold export, token mirror, a gold inventory with two sources during the migration, the Figma review gate in the rules, a Figma skill and the formal freeze of Stitch.

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
   - the current values move under a "Material 3 Expressive — flows not migrated" section, which [A45](../../android/plans/a45-remove-material3.md) deletes;
   - `docs/tokens.md` stays the single repository owner of token values, as a mirror of Figma.
5. **Rules:**
   - `AGENTS.md`:
     - Visual QA: two gold sources during the migration, the `docs/qa/figma/` folder law, export commands;
     - "Stitch gate": Stitch is frozen and the Figma review gate applies (link to [plans/README.md § Figma review gate](README.md#figma-review-gate));
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

<Filled at Completion.>
