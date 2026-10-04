---
name: aero-compose
description: Build and change Dieta Bot Compose UI with the own Aero design system (tokens, glass, components, icons) instead of Material 3.
---

# Aero Compose

Read [AGENTS](../../../AGENTS.md), [ADR-030](../../../docs/design/adrs/ADR-030-own-design-system-aero.md), [tokens](../../../docs/tokens.md), the approved plan and the Figma golds of the inventory ([docs/qa](../../../docs/qa/README.md)).

- `AeroTheme` is the root theme (system-following light/dark, no dynamic color, no toggle). Read values through `Aero.colors`, `Aero.type` and `Aero.shapes`; never hard-code a colour, size or radius that a token owns.
- Tokens are generated: Figma `Design` → `docs/design/tokens.json` → Gradle task `generateAeroTokens` (`AeroTokens.kt` under `build/generated`). Change Figma and the JSON, never the generated code.
- Feature code never imports `androidx.compose.material3` or Material icons. Icons are Phosphor drawables through `AeroIcon(AeroIconName.*)`; add a new one as a `ph_<name>_<weight>.xml` VectorDrawable plus an enum entry.
- Text goes through `AeroText` (Figma line boxes, tabular numbers); controls through `dietaClick` (Aero press veil + haptic).
- Glass: `aeroGlass` on an `AeroPage` backdrop. API 31+ blurs with Haze; below 31 the fill is more opaque. On a surface that is already glass (sheet, dialog, glass card) pass the fill-only option (`backdropBlurred` / `onGlass`).
- Dialogs and sheets are drawn in the screen (`AeroConfirmDialog`, `AeroNoticeDialog`, `AeroSheet`, `AeroTimeWheelDialog`) over `AeroScrim`, with the page behind blurred (layer blur 8).
- Reuse the components in `core/designsystem/aero` before writing a new one; a new component mirrors its Figma component name in its KDoc.
- Accent is an accent only; macros keep their semantic colours. Preserve the Home FAB that opens Chat; no showcase app bars or bottom navigation.

Validate with `testDevDebugUnitTest` (`GoldTest` for the JVM gold), `verifyRoborazziDevDebug`, and fresh dark/light emulator captures through `node tools/diff-gold.mjs`.
