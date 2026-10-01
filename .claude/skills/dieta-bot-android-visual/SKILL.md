---
name: dieta-bot-android-visual
description: Compare fresh Dieta Bot emulator captures and measured layout bounds with matching dark/light Stitch golds after a visual change.
---

# Dieta Bot visual QA

Read [AGENTS](../../../AGENTS.md), the [QA workflow](../../../docs/qa/README.md), [tokens](../../../docs/tokens.md) and the matching gold. Use [Android CLI interaction guidance](../android-cli/references/interact.md); discover the installed executable before treating stale PATH as an absent installation.

1. Confirm the intended device with android info or adb devices. Coordinate use with other active chats before navigating, installing instrumentation/APKs, changing theme, geometry or data. The QA tool expects device captures at 780x1688 and density 320; exported golds have different/full-page heights and need its documented offsets. Set device geometry only on a coordinated test device.
2. For interaction flows, retain the tools/capture-*.sh scripts listed in QA. Ad hoc clean capture: android screen capture --device=<serial> -o docs/qa/android/current/<theme>/<id>.png. An adb binary screencap is a fallback when CLI capture is unavailable.
3. Inspect each PNG visually. Capture both themes as required for the changed screen; annotated diagnostic captures stay in scratch output, never in QA gold/current folders.
4. Measure with android layout --device=<serial> --flat -o <scratchpad>/<id>.json; add --full when non-interactive content is needed. Read UTF-8, including with Get-Content -Encoding utf8 on PowerShell 5.1. Do not rely on the deprecated no-op --diff flag.
5. Compare resource-id/testTag, bounds, visible text and off-screen status against the gold's pixel grid. Record content offsets and clipping rather than treating different full-page heights as interchangeable.
6. Layout JSON does not prove colors, font sizes, radius or CTA appearance. Check these visually against the gold/tokens; use screen capture --annotate only as a diagnostic fallback for visually located elements.
7. Write the required diff list: layout, tokens, type size, radius, ButtonGroup, CTA, timeline, semantic macros, and measured bounds for changed elements.
8. Run node tools/diff-gold.mjs <theme>/<id>; preserve the existing 2% gate and its current region/report-only behavior. Report known gold conflicts explicitly; do not lower thresholds or treat a report-only region as a passing assertion.
   When golds disagree with each other or with the plan, apply [ADR-027](../../../docs/android/adrs/ADR-027-golds-divergentes.md) instead of stopping: a state gold keeps its own geometry and the base gold is untouched; dark/light drift takes the average when both pass; plan/spec/token values win over a gold's rendering and go into the diff list; disabled dimming uses a save layer. Only a future approved plan changes these rules.
9. Check the approved day-one Home and Chat states, including Chat suggestion chips, the Home Chat FAB and consolidated entries. Fail on default Material colors, coaching, missing timeline or incorrect tokens/layout.
10. Iterate and recapture until the applicable comparisons pass. Ignore only the system/font-raster differences allowed by AGENTS. A cold-start splash is expected, not evidence of a freeze.

Layout measurements support the diff; they do not approve the screen alone. Imported Stitch PNGs remain unchanged during app verification.
