---
name: dieta-bot-stitch
description: Write Dieta Bot Stitch gate prompts and verify owner-run ST gates through the existing scripts/API, exported golds and visual checklist.
---

# Dieta Bot Stitch gates

Project: Nutri, 6282733070135794645. Read [SDD gate rules](../../../docs/sdd/README.md#gate-stitch), [exact screen titles and check types](../../../docs/stitch/README.md), the gate plan and its checks.json.

- The owner executes a new/changed layout prompt. A gate does not modify app layout or behavior; dependent implementation waits until the whole gate is completed.
- Every owner instruction and prompt uses the exact Stitch title. Gold IDs only appear in parentheses for agent reference. Provide one ready-to-paste prompt per theme and final titles for new screens.
- Verify with node tools/verify-stitch.mjs st<n> --report --out <scratchpad>/stitch-report.html. It resolves IDs/titles, renders HTML, checks copy/geometry/coherence/image URLs and produces fix prompts.
- Failure: stop gate/dependent implementation, report the missing items and share the HTML through an available absolute file link or preview. Do not require a host-specific file-sending tool or commit failed golds.
- Passing automated checks still require visual inspection of the report crops for checklist items marked for report review.
- After the whole checklist passes, update permitted gold mappings and the gold inventory in docs/qa/README.md, then export only affected golds with node tools/export-stitch.mjs --only <ids>. Run node tools/check-stitch.mjs. Apply the gate lifecycle/indices and required git delivery.
- A gate's checks.json covers exact copy, fits/gaps, content above fixed controls, and a keep list. A green script does not substitute for checklist items it does not cover.

## Capability and historical limits

Direct Stitch MCP may be unavailable. Existing scripts use the Stitch API; inspect tool availability and configuration without printing STITCH_API_KEY. Missing access is a concrete blocker, not permission to implement from any other source.

Observations from the 2026-09-29 ST1/ST2 work and SV1:
- A too-short viewport positioned the fixed Chat FAB incorrectly. The verifier and exporter share loadFrame and grow the viewport; measure served HTML before blaming Stitch.
- Screenshots can be stale, partial or differently sized. The exporter renders HTML where necessary; light homeW has a known stale screenshot entry.
- list_screens may expose hidden/duplicate titles. Resolve existing screens by ID and review duplicate-title warnings.
- Embedded image URLs can expire. Dark/light copy can drift or unrelated elements can change; verify coherence, exact keep texts and URL checks.
- Historical edit_screens calls reported success before persistence, or did not persist. Routine verification must not run live edit probes. Only an explicitly authorized edit may be followed by bounded HTML persistence checks.
- Another chat can change a shared checkout. Use an isolated worktree and preserve its files/branch.

These are historical observations, not universal facts about every host/model. Re-evaluate a specific limit when relevant with read-only evidence first; do not create schedules or repeat live mutation probes just because a different model loaded this skill. Offline verifier tests are node --test tools/verify-stitch.test.mjs. Update all four repository skill copies together after an authorized instruction change.
