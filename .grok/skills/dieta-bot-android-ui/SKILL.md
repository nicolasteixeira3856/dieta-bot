---
name: dieta-bot-android-ui
description: Build Dieta Bot UI in Compose. Use when creating or changing screens, timeline, chat, composer, remaining, ButtonGroup.
---

# dieta-bot-android-ui

Follow the Stitch gold design (`docs/qa/stitch/{dark,light}/`). Natural copy, numbers first. No coach.
Implementation is based strictly on the Stitch PNGs. Layout creation may use wireframes as initial reference (prefer Stitch via MCP, fallback to wireframes).

## Design Tokens & Expressive Theme

- Tokens: `docs/tokens.md`
  - Protein mint (`#4ec994` Dark / `#1b7a4b` Light)
  - Carbs amber (`#e58e42` Dark / `#c2651e` Light)
  - Fat gold (`#e8b86d` Dark / `#b8873d` Light)
  - Bad coral (`#e07a6a` Dark / `#c14d40` Light)
- Theme: `MaterialExpressiveTheme` + `MotionScheme.expressive()`.
- Typography: Remaining highlighted 34pt w590, Number field 28pt.
- Shapes: Sheet radius 22 top, card/chip 14, continuous gold bar 6px.
- Buttons: Expressive `ButtonGroup` for segmented options (O1 teto, O2 eat-back, O3 slots).

## Architecture & Code Quality

- **One file per screen**: `HomeScreen.kt`, `ChatScreen.kt`, `ConfigScreen.kt`. Never cram multiple screens into one file.
- **Screen-scoped ViewModel**: Each screen injects its own ViewModel via `hiltViewModel()`. No monolithic global state.
- **State Stability**: Annotate UI state models with `@Immutable` or `@Stable`. Use `ImmutableList` or read-only collections to enable recomposition skipping.
- **Timeline**: Continuous vertical guide line, circular node markers, single consolidated meal log (e.g. "520 kcal · 28P · 52C · 22G").
- **Chat**: Opened strictly via Home FAB. Uses inverted `LazyColumn(reverseLayout = true)`.

## Visual Verification

1. Automated headless screenshot test:
   - `./gradlew.bat verifyRoborazziDevDebug`
2. Emulator fallback:
   - `cmd /c "adb exec-out screencap -p > docs\qa\android\current\dark\<id>.png"`
3. Compare against Stitch gold in `docs/qa/stitch/{dark,light}/<id>.png`. A screen is NOT done until verified against the matching Stitch PNG.
