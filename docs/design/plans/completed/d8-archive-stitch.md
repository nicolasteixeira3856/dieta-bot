# Plan — D8 Archive Stitch

- Status: Concluído
- Date: 03/10/2026
- Owning context: `design`
- Affected code:
  - Stitch tooling: `tools/export-stitch.mjs`, `tools/check-stitch.mjs`, `tools/verify-stitch.mjs`, `tools/verify-stitch.test.mjs` and `tools/fixtures/verify-stitch/` removed (git history keeps them);
  - `tools/check-docs.mjs` C7 (figma only) + tests;
  - Stitch golds: `docs/qa/stitch/` moved to `docs/qa/_legacy/stitch/`; `.stitch/` moved to `docs/qa/_legacy/stitch-design-system/`;
  - rules and docs: `AGENTS.md`, `docs/README.md`, `docs/sdd/README.md` (Gate Stitch section and template removed), `docs/stitch/README.md`, `docs/qa/README.md`;
  - skills: `dieta-bot-stitch` removed from the four skill trees and added to "Retired".

  No `apps/` or `server/` code.
- Prerequisites: [A40](../../../android/plans/completed/a40-home-aero.md) through [A44](../../../android/plans/completed/a44-config-push-aero.md) `Concluído`, which means every gold in the inventory has source `figma`.
- Figma MCP budget: 0.

Authorization, lifecycle, cancellation and deferral: `docs/sdd/README.md`. Approval: `Aprovo o plano docs/design/plans/d8-archive-stitch.md. Implemente o plano aprovado.`

## Objective

Close the Stitch deprecation ([ADR-031](../../adrs/ADR-031-figma-source-of-truth.md) § 9): Figma `Design` becomes the only UI source, and Stitch leaves the live rules and tooling while staying in history.

## Scope

1. Confirm that every inventory id has source `figma` and that `node tools/check-figma.mjs` passes. If any id is still `stitch`, stop.
2. Move the Stitch golds and the Stitch design system file to `_legacy` ("never compare against `_legacy`" already applies).
3. Remove the Stitch tooling and its tests; C7 checks the inventory against the figma map only.
4. `docs/stitch/README.md`:
   - rewritten in the present as a historical context ("Stitch project `Nutri` was the layout source until ADR-031; its gates live in `plans/completed/`");
   - the matrix row moves from contexts to "Outros docs" as history.
5. `AGENTS.md`: remove every Stitch instruction, and point "Visual QA" to Figma only. Skills list: remove `dieta-bot-stitch` and add it to "Retired".
6. `docs/sdd/README.md`: remove the Gate Stitch section and the `stitch-gate` template; the Gate Figma section stays.
7. The inventory in `docs/qa/README.md` drops the source column (everything is `figma`).

## Out of scope

- Deleting the Stitch project `Nutri` in Google Stitch. That is the owner's choice, outside the repository.
- App code ([A45](../../../android/plans/completed/a45-remove-material3.md) renames the client gold test).

## Validation

1. `node tools/check-figma.mjs`, `node tools/check-docs.mjs`, `node tools/check-skills.mjs` and `node --test tools/check-docs.test.mjs` pass.
2. `grep -ri stitch AGENTS.md .agents .claude .grok .hermes` returns only the "Retired" line.
3. `docs/qa/` root has no PNG/JPG.

## Results

Approved by the owner on 05/10/2026 and implemented the same day. Figma MCP calls: 0.

**Owner decision during implementation (05/10/2026).** Scope 7 conflicted with "No `apps/` code": the client `GoldInventory` (used by `GoldTest`) required every inventory line to start with `stitch:` or `figma:`, so dropping the source column without touching `apps/` would break `testDevDebugUnitTest`. Asked with three options (keep the `figma:` prefix, drop it and edit the app test, stop the plan); the owner chose "Tirar e editar o app". This delivery therefore also changes three JVM test files, no production code:
- `GoldInventory.kt` parses the ids only (`contains(id)`), no source;
- `GoldTest.kt` asserts the id is in the inventory instead of `source == "figma"`;
- `GoldInventoryTest.kt` follows the new format.

**Delivered:**
1. Inventory check: every id was `figma` before any change; `node tools/check-figma.mjs` passed (68 golds, 34 dark + 34 light).
2. Moved with `git mv`: `docs/qa/stitch/` (62 PNGs) to `docs/qa/_legacy/stitch/`, `.stitch/DESIGN.md` to `docs/qa/_legacy/stitch-design-system/`.
3. Removed: `tools/export-stitch.mjs`, `tools/check-stitch.mjs`, `tools/verify-stitch.mjs`, `tools/verify-stitch.test.mjs`, `tools/fixtures/verify-stitch/` (7 files), `docs/sdd/templates/stitch-gate.md`, the `dieta-bot-stitch` skill in the four trees. Consequences needed to keep the remaining tools working:
   - `tools/export-figma.mjs` imported `listArg`, `pngDiff` and `NOISE_MAX_PCT` from `export-stitch.mjs`: the helpers (and `NOISE_DELTA`) now live in `export-figma.mjs`, unchanged;
   - `tools/check-docs.mjs` C7: the inventory is a plain id list and must equal both theme maps of `export-figma.mjs`; tests rewritten;
   - `tools/diff-gold.mjs`: the Stitch branch (source lookup, Stitch geometry, 1768 px band) is gone, golds are read from `docs/qa/figma/`. Output of `dark/o1 light/o3t dark/chatF light/home0 dark/chatP` is byte-identical before and after;
   - `tools/capture-photo.sh` reads its sample photos (chatF plate, chatA thumbnail) from `docs/qa/_legacy/stitch/dark/`: test input, not a comparison;
   - `tools/package.json`: Stitch scripts and the `playwright` dependency (used only by the Stitch exporter) removed, `export:figma` / `check:figma` added; lockfile updated by `npm uninstall`.
4. `docs/stitch/README.md` rewritten as history; the matrix row moved from contexts to "Outros docs".
5. `AGENTS.md`: Visual QA points to Figma only; Stitch folder law, export commands, Stitch gate line, Stitch project id in the technical-ID sentence and the Stitch fallback in "Do not" removed; `dieta-bot-stitch` added to "Retired".
6. `docs/sdd/README.md`: "Gate de design (congelado)" type, "Gate Stitch" section and the template link removed; the Gate Figma section stays (its `stitch` → `figma` source sentence removed). `docs/sdd/templates/figma-flow.md` no longer refers to Stitch titles or the source column.
7. `docs/qa/README.md`: inventory lines without source prefix; Stitch folder, export and gate wording removed.
- Other live text that named Stitch as current: `README.md`, `docs/produto/README.md`, `docs/android/README.md`, `docs/design/README.md`, `docs/design/plans/README.md`; skills `android-architecture`, `android-cli` (interact reference), `dieta-bot-android-decision`, `-feature`, `-qa`, `-ui`, `-visual`, `dieta-bot-figma`, `screenshot-testing`.

**Validation:**
- `node tools/check-figma.mjs`: 68 golds verified.
- `node tools/check-docs.mjs`: passed (47 live files, 50 link-checked).
- `node tools/check-skills.mjs`: passed (26 skills, 136 files per tree).
- `node --test tools/*.test.mjs`: 28/28 (`check-docs.test.mjs` 14/14).
- `grep -rni stitch AGENTS.md .agents .claude .grok .hermes --exclude-dir=worktrees`: only the "Retired" line. Without the exclusion the grep also hits `.claude/worktrees/`, which is gitignored local checkouts of other sessions, not repository content.
- `docs/qa/` root: no PNG/JPG.
- `:app:testDevDebugUnitTest --tests GoldInventoryTest --tests GoldTest`: 60 tests, 57 pass. The 3 failures are pixel gates, not the inventory: `chatF` dark/light (region 3.64% / 2.90%) and `chatL` dark (ink 1.258). Their golds changed in D10 (Tali header and loading caption, `completed/`) and the client plan A49 that draws them is not implemented yet; this delivery only changed the inventory assertion.

**Not done (out of scope):** the Stitch project `Nutri` in Google Stitch and the `stitch` MCP server entry in `.mcp.json` are untouched (owner's choice). Comments naming Stitch in app code and app tests (`DietaBotTokens.kt`, `ChatFixtures.kt`, `HomePanelMapperTest.kt`, `RoborazziSmokeTest.kt`) stay; ADR bodies and history plans stay.
