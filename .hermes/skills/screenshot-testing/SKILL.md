---
name: screenshot-testing
description: Verify Dieta Bot JVM screenshot regression with Roborazzi and distinguish app baselines from read-only Stitch design golds after visual changes.
---

# Screenshot regression

Read [AGENTS](../../../AGENTS.md), [QA guidance](../../../docs/qa/README.md), the approved plan and the current [Roborazzi tests](../../../apps/android/app/src/test/java/com/nutri/android/ui/RoborazziSmokeTest.kt) / [Stitch tests](../../../apps/android/app/src/test/java/com/nutri/android/ui/StitchGoldTest.kt).

From apps/android:
- verifyRoborazziDevDebug checks existing app baselines first.
- compareRoborazziDevDebug produces comparison output for review.
- recordRoborazziDevDebug intentionally changes app baselines. Use it only when baseline updates are in scope, after inspecting the diff; do not start every verification by recording.

Three separate artifact classes:
- App regression baselines: apps/android/app/src/test/snapshots/{dark,light}/, as configured by Roborazzi.
- Imported design inputs: docs/qa/stitch/{dark,light}/. These are read-only during app verification; never point captureRoboImage or a baseline recorder at them.
- Fresh device captures: docs/qa/android/current/{dark,light}/. JVM renders/diffs belong in build output, not in device capture folders.

The Roborazzi tests currently use CompareOptions(changeThreshold = 0.01f). StitchGoldTest has its own 2% blurred-image/region comparisons and explicit report-only gold conflicts; the emulator tool has its corresponding gate. These metrics are not interchangeable. Inspect the actual test/tool before interpreting its report, and do not relax thresholds or describe report-only checks as assertions.

JVM rendering is useful for regression, but it does not prove device layout, IME behavior or a Stitch match. Changed UI still requires the [visual workflow](../dieta-bot-android-visual/SKILL.md): fresh dark/light emulator PNGs and written gold comparisons. Report known gaps honestly. Do not promise a fixed run time.
