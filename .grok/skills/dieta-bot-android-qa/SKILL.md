---
name: dieta-bot-android-qa
description: Validate a Nutri delivery. Use before marking /goal DONE, after UI, or when the owner pastes a log.
---

# dieta-bot-android-qa

Checklist:
- `.\gradlew.bat test` green
- `.\gradlew.bat :app:compileDebugKotlin` green
- `adb devices` has an emulator
- screenshots in `docs/qa/android/current/{dark|light}/` matching Stitch gold in `docs/qa/stitch/{dark,light}/`
- tokens vs `docs/tokens.md` (no default purple, correct semantic macros)
- timeline guide and single consolidated meal entries
- no OPENAI_API_KEY in source
- ADR if there was a decision

Without a screenshot of new UI matching the Stitch gold, it is not DONE.
