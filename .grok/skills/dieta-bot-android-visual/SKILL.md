---
name: dieta-bot-android-visual
description: Compare the emulator screen with the Dieta Bot Stitch gold design. Use after any visual change or when the owner says it looks crude.
---

# dieta-bot-android-visual

Tool: Android CLI (`android`). If `android` is not on PATH (session opened before the install), call `$env:USERPROFILE\AppData\AndroidCLI\android.exe` (PowerShell) or `"$USERPROFILE/AppData/AndroidCLI/android.exe"` (Bash).

1. `android info` (or `adb devices`) lists the emulator. Gold geometry: `adb shell wm size 780x1688` and `adb shell wm density 320` (390 dp @ 2x, 1 dp = 2 px).
2. Flows with interaction: the `tools/capture-*.sh` scripts listed in `docs/qa/README.md`. They stay the official path for those screens.
3. Ad hoc capture: `android screen capture -o docs/qa/android/current/<theme>/<id>.png`. Fallback: `cmd /c "adb exec-out screencap -p > docs\qa\android\current\<theme>\<id>.png"`.
4. Measure: `android layout --flat -o <scratchpad>/<id>.json`. Read it as UTF-8 (in PowerShell 5.1: `Get-Content -Encoding utf8`), or accents break. Check against the gold, same pixel grid:
   - position and size (`bounds`) of every `resource-id` on the screen (e.g. `home-consumed`, `home-fab`, `home-summary-*`);
   - rendered text: pt-BR copy, consolidated meal entry (`520 kcal · 28P · 52C · 22G`), zero chips on day 1;
   - expected element missing or `off-screen`.
5. Color, font size (remaining 34pt), radius (sheet 22, card/chip 14), CTA and semantic macro colors: read the capture against `docs/qa/stitch/{dark,light}/<id>.png` and `docs/tokens.md`. `layout` does not carry them.
6. Element without a testTag, or missing from `layout`: `android screen capture --annotate -o <scratchpad>/<id>-annot.png` and look at the PNG before concluding. Annotated PNGs never go in `docs/qa/`.
7. Write the diff list (layout, tokens, type size, radius, ButtonGroup, CTA, timeline, semantic macros), citing the measured bounds.
8. Gate: `node tools/diff-gold.mjs <theme>/<id>` at 2% or less. `layout` feeds the diff list; it never approves a screen alone.
9. Fail if default Material purple/blue, Appbar, FAB menu, coach copy, small remaining, chip on day 1, or missing timeline guide.
10. Fix and recapture. Do not mark DONE in the dark.
