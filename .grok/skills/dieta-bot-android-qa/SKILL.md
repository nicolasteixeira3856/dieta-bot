---
name: dieta-bot-android-qa
description: Validate a Nutri delivery. Use before marking /goal DONE, after UI, or when the owner pastes a log.
---

# dieta-bot-android-qa

Checklist:
- `.\gradlew.bat test` green
- `.\gradlew.bat :app:compileDebugKotlin` green
- `adb devices` has an emulator
- screenshots in `docs/qa/android/current/{dark|light}/` of the screens touched
- tokens vs `docs/tokens.md` (no default purple)
- home day 1 no chip
- no OPENAI_API_KEY in source
- ADR if there was a decision

Without a screenshot of new UI, it is not DONE.
