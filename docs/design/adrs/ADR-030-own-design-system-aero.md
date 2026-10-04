# ADR-030 — Own design system "Aero" replaces Material 3 Expressive

- Status: Accepted (2026-10-03, with the owner's named approval of [D1](../plans/completed/d1-figma-file-foundation.md))
- Date: 2026-10-03
- Context: `design`
- Replaces: [ADR-004](../../decisions/004-m3-expressive.md) completely once accepted (theme, motion, shapes, Expressive controls). The no-dynamic-color rule and the system-driven dark/light theme survive.

## Context

The Android client is themed by `MaterialExpressiveTheme`, `MotionScheme.expressive()` and Material 3 controls (`ButtonGroup`, `LoadingIndicator`, `ModalBottomSheet`, `AlertDialog`). The owner raised two problems on 2026-10-03:

1. **Cross-platform.** A future iOS client has no Material. Two native design languages mean two layouts, two sets of golds and twice the maintenance.
2. **Generic look.** The platform vendor's own system gives the app no identity of its own.

The Material footprint is small: about 20 `androidx.compose.material3` imports, most of them `Text` and `Icon`. Screens already go through `core/designsystem` (`DietaBot*` wrappers).

A Frutiger Aero pilot was run on 2026-10-03, first in a separate Stitch project and then in Figma. The owner approved the direction, the palette, Nunito Sans and Phosphor icons. The Figma pilot proved that every theme difference fits in variables: page gradient stops, glass sheen and shadow color are all bound to `Light`/`Dark` modes. The dark screens are the light screens with the variable mode switched, with no per-screen edits.

## Decision

1. **Dieta Bot owns its design system, themed "Aero"** (restrained Frutiger Aero): sky (light) and ocean (dark) page gradients, frosted glass surfaces with a top sheen, glossy pill CTAs, a few edge bubbles never behind text, and flat high-contrast numbers on top. Tone stays numbers-first and dry; the gloss lives in the frame, never on the numbers.
2. **The source of the design system is the Figma file `Design`** ([ADR-031](ADR-031-figma-source-of-truth.md)). Its variables own the token values: color per mode, shape, spacing, sheen, gradient stops and motion. The repository holds a generated mirror; nobody edits it by hand.
3. **Typography:** Nunito Sans (OFL), bundled in the app. Every number uses tabular figures. If the bundled font lacks a true `tnum` feature, the client plan records the fallback before any screen ships.
4. **Icons:** Phosphor (MIT), weights `regular` (UI), `duotone` (emphasis), `fill` (selected) and `bold` (CTA). The same SVG set feeds Figma components and the Android `ImageVector`s. The icon color is a token, and the gloss comes from the container, never from the icon.
5. **Palette (approved 2026-10-03):** light "sky and sand" and dark "ocean at night". Semantic macro colors (protein green, carbs orange, fat yellow) stay semantic. Aero blue (light) and cyan (dark) replace gold as the accent. Every text and macro color clears 4.5:1 on its surfaces. The exact values live in the Figma variables, not in this ADR.
6. **Android:** feature code never imports `androidx.compose.material3`. `core/designsystem` exposes `DietaBotTheme` through its own CompositionLocals: colors, type, shapes, glass, gloss and motion. Material 3 may stay only as a hidden behavior provider inside `core/designsystem` (for example the sheet drag or dialog window), never as the look. Glass uses a real backdrop blur on API 31+. Below API 31 (minSdk 26) it falls back to a more opaque glass without blur. The backdrop technique, and any new dependency it needs, is named in the client foundation plan and approved with it.
7. **Motion:** own motion tokens (durations and easing) replace `MotionScheme.expressive()`.
8. **Unchanged:** the theme follows the system (`isSystemInDarkTheme()`), there is no dynamic color, no wallpaper and no settings toggle.
9. **iOS** stays out of scope ([ADR-005](../../decisions/005-android-only.md), AGENTS "Do not"). This ADR only keeps the design system platform-neutral. A future iOS decision may map glass to Liquid Glass.

## Motivation

- One identity on every platform, with variables and components as the shared source and native code consuming generated tokens.
- The approved pilot showed the theme fits in variables, so every component exists once for both themes.
- A small Material footprint makes the swap a contained client refactor, not a rewrite.

## Consequences

### Positive

- Distinct visual identity; light and dark come from one component tree.
- Tokens become data (Figma → generated mirror → Kotlin), which a future Swift client can reuse.
- Feature code loses its direct Material dependency.

### Negative

- Every gold is redone (31 per theme), flow by flow ([ADR-031](ADR-031-figma-source-of-truth.md)).
- Backdrop blur costs GPU on scrolling lists; the fallback below API 31 looks flatter.
- Translucent surfaces put text contrast at risk; contrast is checked on real devices and outdoors before each flow closes.
- Behavior that Material gave for free (sheet physics, ripple, dialog focus) has to be kept or rebuilt inside `core/designsystem`.

## Alternatives considered

### Keep Material 3 Expressive

It fails both owner goals: no iOS story and a generic look.

### Compose Multiplatform to share UI code with iOS

It solves code duplication, not identity, and native Liquid Glass would need SwiftUI interop. It is a stack decision outside this ADR.

### Source Sans 3 or Hind

Both were offered in the pilot. The owner chose Nunito Sans.

### Fluent Color icons

Colored gradient icons were offered. The owner chose Phosphor with gloss on the container.

## Relations

- Specifications affected (at the Completion of each client flow plan): [home-timeline](../../produto/specifications/home-timeline.md), [perfil-onboarding](../../produto/specifications/perfil-onboarding.md), [chat](../../produto/specifications/chat.md), [memoria-push](../../produto/specifications/memoria-push.md) (visual references only, not behavior); [`docs/tokens.md`](../../tokens.md) becomes a generated mirror.
- Related ADRs: [ADR-004](../../decisions/004-m3-expressive.md) (replaced), [ADR-005](../../decisions/005-android-only.md), [ADR-012](../../produto/adrs/ADR-012-chat-home-perfil.md) (screens unchanged), [ADR-027](../../android/adrs/ADR-027-golds-divergentes.md), [ADR-031](ADR-031-figma-source-of-truth.md).
- Consumer contexts: [android](../../android/README.md).

## Rollout

- The Aero layer is built next to the current theme; each flow's client plan moves its screens to it.
- During the migration the dev build mixes looks across flows. Tester distribution during that window is the owner's call per build.
- The last client plan removes `MaterialExpressiveTheme` and every feature-level Material import. In that same delivery, the status line of ADR-004 changes to `Superseded by ADR-030`.
