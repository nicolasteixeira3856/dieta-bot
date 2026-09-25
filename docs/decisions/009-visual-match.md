# 009 — Screens vs gold wire

Date: 2026-09-25

## Decision

The Android client follows `docs/qa/wire/<id>.png` for splash, O1, O2, T0, T1, T2, T3.

Splash is a cold start ≤2s (Compose boot + window splash resources). A visible splash is not a freeze. After the delay the app continues to O1 or T0. Capture seed `nutri_tela=splash` holds the boot frame so QA can shoot it.

Theme follows the system (`isSystemInDarkTheme()`): `MaterialExpressiveTheme` + `MotionScheme.expressive()`. No dynamic color. No Appbar / FAB / BottomNav.

O1, O2 and T3 modes use `ButtonGroup`. T1 loading uses `LoadingIndicator`. Sheet top radius 22. Card/chip 14. Remaining 34sp. Field 28sp. CTA is high contrast, not gold.

Captures: `docs/qa/android/goal-11/{dark,light}/` copied to `docs/qa/android/current/{dark,light}/`.

## Leftover (not obvious hierarchy/token mismatches)

- Status bar, clock, battery, gesture nav — ignored by the Visual QA law.
- Emulator font raster vs wire HTML font.
- Selected `ToggleButton` uses M3 Expressive checked shape (more pill) vs the wire’s 15px inner radius.
- Gold PNGs are the dark wire. Light shots use the light token set (`#f4f3f0` bg, CTA `#111` on `#f3f5f7`).

No id is left with an obvious hierarchy, copy, saldo-size, CTA-color, or day-1-chip mismatch.

## Rejected

- Treating splash as a crash or removing it.
- Pixel-diff of the status bar.
- Comparing against `docs/qa/_legacy/`.
- Editing `server/`.
