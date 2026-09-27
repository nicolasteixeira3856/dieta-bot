---
name: dieta-bot-android-ui
description: Build Nutri UI in Compose. Use when creating or changing screens, timeline, chat, composer, remaining, ButtonGroup.
---

# dieta-bot-android-ui

Follow the Stitch gold design (`docs/qa/stitch/{dark,light}/`). Natural copy, numbers first. No coach.
Implementation is based strictly on the Stitch PNGs. Layout creation may use wireframes as initial reference (prefer Stitch via MCP, fallback to wireframes).

Tokens: `docs/tokens.md` (Protein mint `#4ec994`, Carbs amber `#e58e42`, Fat gold `#e8b86d`).
Theme: `MaterialExpressiveTheme` + `MotionScheme.expressive()`.

Remaining 34pt. CTA `#f3f5f7`/`#111`. Sheet radius 22 top. Card 16.
Timeline: continuous vertical guide, node markers, single consolidated meal log (e.g. 520 kcal · 28P · 52C · 22G).

Onboarding O1..O4 = ButtonGroup. Loading = LoadingIndicator.
Chat is opened via FAB. Home is the daily timeline panel.

Screenshot with `cmd /c "adb exec-out screencap -p > docs\qa\android\current\dark\<id>.png"`.
Compare with matching Stitch gold in `docs/qa/stitch/{dark,light}/<id>.png`.
