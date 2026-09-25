---
name: dieta-bot-android-visual
description: Compare the emulator screen with the Nutri wire. Use after any visual change or when the owner says it looks crude.
---

# dieta-bot-android-visual

1. `adb devices` is not empty.
2. Capture with `cmd /c "adb exec-out screencap -p > docs\qa\android\current\dark\<id>.png"`.
3. Compare with `docs/qa/wire/<id>.png` and `docs/tokens.md`.
4. Fail if default Material purple/blue, Appbar, FAB, Expo tab, coach copy, small remaining, chip on day 1.
5. Fix and recapture. Do not mark DONE in the dark.
