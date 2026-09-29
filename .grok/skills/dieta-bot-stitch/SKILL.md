---
name: dieta-bot-stitch
description: Verify Dieta Bot Stitch gates (ST<n>), export gold PNGs and write Stitch prompts. Use when the owner says a Stitch prompt ran, before exporting golds, or before calling the Stitch MCP. Lists the known limits of the agent and of the Stitch MCP.
---

# dieta-bot-stitch

Project: Stitch `Nutri` (`6282733070135794645`). Rules: `docs/sdd/README.md` § Gate Stitch. Titles ↔ gold ids: `docs/stitch/README.md` § Nomes das telas.

## Flow

1. Read the gate plan (`docs/stitch/plans/st<n>-*.md`) and its checklist.
2. `mcp__stitch__list_screens`: old IDs and titles unchanged; new screens exist with the exact title.
3. Add new gold ids to `tools/export-stitch.mjs` / `tools/check-stitch.mjs`. Run `node tools/export-stitch.mjs` and `node tools/check-stitch.mjs`.
4. Keep only the gate's PNGs; revert every other re-exported PNG.
5. Check the list. Anything wrong: stop, send the owner the exact fix prompt (Stitch titles, one block per theme), commit nothing.
6. Green: gold lists, title table, plan to `completed/`, git delivery.

## Known limits (read before trusting a result)

Observed on 29/09/2026 by **Claude Opus 5.5 (`claude-opus-5-5`)** running in Claude Code, during ST1 and ST2. These are findings of this model in this setup, not facts about Stitch. A different model, or the same model with better tooling, may get further: re-test before assuming a limit still holds.

### Agent side (this model and its tooling)

- **My render is not the Stitch canvas.** A short viewport put the fixed "Chat" FAB over the Home disclaimer; I blamed Stitch and was wrong. The exporter now grows the viewport to the frame height. Before reporting a Stitch defect, render the HTML and measure it.
- **Stitch screenshots can be stale.** The full-size screenshot of light `homeW` predated its HTML. Trust the HTML; screens with a known stale screenshot go in `RENDER_FROM_HTML` in the exporter.
- **Reading PNGs is slow, costly and error-prone.** Prefer DOM checks on the screen HTML: text present or absent, element overflow at 390 px, row height, image URLs answering 200. Use images for the final look only.
- **Every export rewrites all PNGs** with byte-level noise. Compare pixels, not bytes, and revert what the gate does not own.
- **`/goal` + a manual owner step loops.** The stop hook kept blocking while waiting for the owner. Do not run gate verification under `/goal`, or phrase the goal as "verification executed and reported".
- **Parallel sessions share the working tree.** Another session switched branches mid-review. Use a worktree.

### Stitch MCP side (as observed)

- **`edit_screens` reports success but persists late or not at all.** Edits came back as DOM operations; the served HTML stayed the same for minutes, and one edit never showed. Retrying does not help. Do not use it for gate fixes unless the owner explicitly authorizes; after any call, poll the HTML until the change shows.
- **`list_screens` returns screens the owner cannot see**, with the same title as a visible one (old or hidden versions). Resolve screens by ID; report duplicate titles, but they may not exist in the canvas.
- **Screenshots vary:** 1x JPEG, partial captures, desktop-sized frames (`chatG` dark: 2560 px wide).
- **Image URLs inside the HTML can expire** (403). Check them.
- **Prompts leak into unrelated elements** (header title changed, macro chip labels dropped). Prompts need a "keep unchanged" list with exact texts and measurable constraints ("one line at 390 px", font sizes, bottom space for the FAB). One screen per prompt when the change is delicate.

## Re-evaluate the limits

Stitch is a product in active development; its MCP in particular may improve. Every few weeks, and whenever a different model runs this skill:

1. Re-test each limit above with a minimal case (one `edit_screens` call then poll the HTML; compare screenshot vs HTML; list duplicate titles).
2. Update this section: what now works, what still fails, date and model.
3. Keep `.agents/skills`, `.claude/skills`, `.grok/skills` and `.hermes/skills` identical.
