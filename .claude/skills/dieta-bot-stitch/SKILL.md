---
name: dieta-bot-stitch
description: Verify Dieta Bot Stitch gates (ST<n>), export gold PNGs and write Stitch prompts. Use when the owner says a Stitch prompt ran, before exporting golds, or before calling the Stitch MCP. Lists the known limits of the agent and of the Stitch MCP.
---

# dieta-bot-stitch

Project: Stitch `Nutri` (`6282733070135794645`). Rules: `docs/sdd/README.md` § Gate Stitch. Titles ↔ gold ids: `docs/stitch/README.md` § Nomes das telas. Check types: `docs/stitch/README.md` § Verificação automática (SV1).

## Flow

1. Read the gate plan (`docs/stitch/plans/st<n>-*.md`), its checklist and its `st<n>-*.checks.json`.
2. `node tools/verify-stitch.mjs st<n> --report --out <scratchpad>/stitch-report.html`. It resolves the screens (ID for old ones, exact title for new ones), renders the HTML with the exporter's render, runs the checks, dark × light coherence and image URLs, and writes the report with before/after crops and one fix prompt per failing screen and theme.
3. `NÃO PASSOU`: stop. Send the owner the report in one message (`SendUserFile`), commit nothing.
4. `PASSOU`: look at the report crops for the checklist items marked "relatório" (look only; no script measures them).
5. Add the new gold ids to `tools/export-stitch.mjs` / `tools/check-stitch.mjs`. Run `node tools/export-stitch.mjs --only <gate golds>` and `node tools/check-stitch.mjs`. The noise filter restores every PNG without a visual change.
6. Green: gold lists, title table, plan to `completed/` (with its `checks.json`), git delivery.

A new gate plan ships with its `checks.json`: one `text` check per exact copy of the prompt, `fits`/`gap` for every "one line at 390 px" or spacing constraint, `visibleAbove` for content near a fixed element, and a `keep` list for the fix prompt.

## Known limits (read before trusting a result)

Observed on 29/09/2026 by **Claude Opus 5.5 (`claude-opus-5-5`)** running in Claude Code, during ST1 and ST2. These are findings of this model in this setup, not facts about Stitch. A different model, or the same model with better tooling, may get further: re-test before assuming a limit still holds.

### Agent side (this model and its tooling)

- **My render is not the Stitch canvas.** A short viewport put the fixed "Chat" FAB over the Home disclaimer; I blamed Stitch and was wrong. The exporter now grows the viewport to the frame height, and the verifier uses the same render (`loadFrame`). Before reporting a Stitch defect that the checks do not cover, render the HTML and measure it.
- **Stitch screenshots can be stale.** The full-size screenshot of light `homeW` predated its HTML. Trust the HTML; screens with a known stale screenshot go in `RENDER_FROM_HTML` in the exporter.
- **`/goal` + a manual owner step loops.** The stop hook kept blocking while waiting for the owner. Do not run gate verification under `/goal`, or phrase the goal as "verification executed and reported".
- **Parallel sessions share the working tree.** Another session switched branches mid-review. Use a worktree.

Resolved on 29/09/2026 by SV1 (Claude Opus 5.5, `claude-opus-5-5`): reading PNGs to check a gate (now DOM checks in `tools/verify-stitch.mjs`; images only for the final look), export noise on every PNG (now `--only` and the pixel noise filter), expired image URLs (now checked on every screen).

### Stitch MCP side (as observed)

- **`edit_screens` reports success but persists late or not at all.** Edits came back as DOM operations; the served HTML stayed the same for minutes, and one edit never showed. Retrying does not help. Do not use it for gate fixes unless the owner explicitly authorizes; after any call, poll the HTML until the change shows.
- **`list_screens` returns screens the owner cannot see**, with the same title as a visible one (old or hidden versions). Resolve screens by ID; the verifier reports duplicate titles as `aviso`, but they may not exist in the canvas.
- **Screenshots vary:** 1x JPEG, partial captures, desktop-sized frames (`chatG` dark: 2560 px wide).
- **Image URLs inside the HTML can expire** (403). The verifier checks them.
- **Prompts leak into unrelated elements** (header title changed, macro chip labels dropped). Prompts need a "keep unchanged" list with exact texts and measurable constraints ("one line at 390 px", font sizes, bottom space for the FAB). One screen per prompt when the change is delicate. The coherence check catches a leak that hits only one theme.
- **Dark and light drift apart.** SV1's first run found 5 golds whose visible texts differ between themes (`homeX`, `chatF`, `chatT`, `chatP`, `chatG`); they are listed as `coherence.known` in the ST1/ST2 `checks.json`.

## Re-evaluate the limits

Stitch is a product in active development; its MCP in particular may improve. Every few weeks, and whenever a different model runs this skill:

1. Re-test each limit above with a minimal case (one `edit_screens` call then poll the HTML; compare screenshot vs HTML; list duplicate titles).
2. Run `node --test tools/verify-stitch.test.mjs` and `node tools/verify-stitch.mjs st1` / `st2`: they must stay green.
3. Update this section: what now works, what still fails, date and model.
4. Keep `.agents/skills`, `.claude/skills`, `.grok/skills` and `.hermes/skills` identical.
