---
name: dieta-bot-android-feature
description: Implement a feature in the Nutri Android client. Use when the request is a screen, a rule, an API client, or day persistence. Folder apps/android.
---

# dieta-bot-android-feature

Folder: `apps/android/`. Do not edit `server/` in this flow.

Order:
1. Domain rule test (TDD).
2. Repository / DataStore.
3. ViewModel + UiState.
4. Composable.
5. `.\gradlew.bat test` and `compileDebugKotlin`.
6. Emulator screenshot if UI changed.

Parity: photo ≤1280 JPEG 70, header `X-Invite`, timeout 20s, home day 1 no chip.

Do not restore Flutter/RN. No Room in this cut (DataStore/JSON).
