---
name: dieta-bot-android-qa
description: Validate a Dieta Bot delivery against its approved plan, dev checks, visual evidence and manual-validation requirements.
---

# Dieta Bot delivery QA

Use [AGENTS](../../../AGENTS.md), the approved plan and [Android validation guidance](../../../docs/android/README.md). Run relevant checks from apps/android using assembleDevRelease, testDevDebugUnitTest and verifyRoborazziDevDebug. Do not substitute aggregate test or unflavored compileDebugKotlin tasks.

For changed UI:
- Confirm the dark/light gold PNGs from the id's source in the [gold inventory](../../../docs/qa/README.md) exist and any required design plan or gate is completed.
- Follow the [visual workflow](../dieta-bot-android-visual/SKILL.md): Android CLI clean captures, layout bounds/text read as UTF-8, and the existing scripted capture flows.
- Save fresh emulator captures in docs/qa/android/current/{dark,light}/. JVM renders do not belong there.
- Compare against the matching golds using the current QA gate and write diffs covering layout, tokens, type size, radius, ButtonGroup, CTA, timeline and semantic macros.
- Preserve system theme following, the Home Chat FAB, consolidated meal entries, current day-one state and splash cold-start behavior. A visible splash alone is not a crash.

Check scope, applicable ADRs and dev-only telemetry; never print secrets when checking credential boundaries. Passing automation does not complete a plan with pending owner validation. Record executed commands, real results and remaining manual checks, then apply the [SDD lifecycle](../../../docs/sdd/README.md).

When the owner asks for a phone test build, use tools/distribute-dev.ps1 -Notes <scratchpad-notes.md> per [Android distribution](../../../docs/android/README.md#distribuição). Notes are a human pt-BR changelog, never raw git log. The script requires a clean tree and controls version/commit/tag; do not hand-edit version.properties. -DryRun builds/checks without distribution.
