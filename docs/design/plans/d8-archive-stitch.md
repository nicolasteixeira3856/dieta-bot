# Plan — D8 Archive Stitch

- Status: Aguardando aprovação
- Date: 03/10/2026
- Owning context: `design`
- Affected code:
  - Stitch tooling: `tools/export-stitch.mjs`, `tools/check-stitch.mjs`, `tools/verify-stitch.mjs`, `tools/verify-stitch.test.mjs` and `tools/fixtures/verify-stitch/` removed (git history keeps them);
  - `tools/check-docs.mjs` C7 (figma only) + tests;
  - Stitch golds: `docs/qa/stitch/` moved to `docs/qa/_legacy/stitch/`; `.stitch/` moved to `docs/qa/_legacy/stitch-design-system/`;
  - rules and docs: `AGENTS.md`, `docs/README.md`, `docs/sdd/README.md` (Gate Stitch section and template removed), `docs/stitch/README.md`, `docs/qa/README.md`;
  - skills: `dieta-bot-stitch` removed from the four skill trees and added to "Retired".

  No `apps/` or `server/` code.
- Prerequisites: [A40](../../android/plans/completed/a40-home-aero.md) through [A44](../../android/plans/completed/a44-config-push-aero.md) `Concluído`, which means every gold in the inventory has source `figma`.
- Figma MCP budget: 0.

Authorization, lifecycle, cancellation and deferral: `docs/sdd/README.md`. Approval: `Aprovo o plano docs/design/plans/d8-archive-stitch.md. Implemente o plano aprovado.`

## Objective

Close the Stitch deprecation ([ADR-031](../adrs/ADR-031-figma-source-of-truth.md) § 9): Figma `Design` becomes the only UI source, and Stitch leaves the live rules and tooling while staying in history.

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
- App code ([A45](../../android/plans/completed/a45-remove-material3.md) renames the client gold test).

## Validation

1. `node tools/check-figma.mjs`, `node tools/check-docs.mjs`, `node tools/check-skills.mjs` and `node --test tools/check-docs.test.mjs` pass.
2. `grep -ri stitch AGENTS.md .agents .claude .grok .hermes` returns only the "Retired" line.
3. `docs/qa/` root has no PNG/JPG.

## Results

<Filled at Completion.>
