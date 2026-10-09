---
name: screenshot-testing
description: Verify Fibrai JVM screenshot regression with Roborazzi and distinguish app baselines from read-only Figma golds after visual changes.
---

# Screenshot regression

Read [AGENTS](../../../AGENTS.md), [QA guidance](../../../docs/qa/README.md), the approved plan and the current [Roborazzi tests](../../../apps/android/app/src/test/java/app/fibrai/android/ui/RoborazziSmokeTest.kt) / [gold tests](../../../apps/android/app/src/test/java/app/fibrai/android/ui/GoldTest.kt).

From apps/android:
- verifyRoborazziDevDebug checks existing app baselines first.
- compareRoborazziDevDebug produces comparison output for review.
- recordRoborazziDevDebug intentionally changes app baselines. Use it only when baseline updates are in scope, after inspecting the diff; do not start every verification by recording.

Three separate artifact classes:
- App regression baselines: apps/android/app/src/test/snapshots/{dark,light}/, as configured by Roborazzi.
- Imported design inputs: docs/qa/figma/{dark,light}/, one gold per id of the inventory. These are read-only during app verification; never point captureRoboImage or a baseline recorder at them.
- Fresh device captures: docs/qa/android/current/{dark,light}/. JVM renders/diffs belong in build output, not in device capture folders.

The Roborazzi tests currently use CompareOptions(changeThreshold = 0.01f). GoldTest has its own 2% blurred-image/region comparisons against the Figma frames and explicit report-only regions; the emulator tool has its corresponding gate. These metrics are not interchangeable. Inspect the actual test/tool before interpreting its report, and do not relax thresholds or describe report-only checks as assertions.

JVM rendering is useful for regression, but it does not prove device layout, IME behavior or a gold match. Changed UI still requires the [visual workflow](../dieta-bot-android-visual/SKILL.md): fresh dark/light emulator PNGs and written gold comparisons. Report known gaps honestly. Do not promise a fixed run time.
