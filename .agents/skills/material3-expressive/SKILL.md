---
name: material3-expressive
description: Maintain Dieta Bot's approved Material 3 Expressive theme, motion, shapes and controls when changing Compose UI or design-system code.
---

# Material 3 Expressive

Read [AGENTS](../../../AGENTS.md), [tokens](../../../docs/tokens.md), the approved plan and matching Stitch golds.

- Keep the stable Compose BOM and the explicit approved Material 3 1.5 alpha override from the current version catalog. Do not switch the whole project to an alpha BOM or update dependencies merely because newer versions exist.
- Root theme uses MaterialExpressiveTheme and MotionScheme.expressive(), with system-following dark/light palettes. No dynamic wallpaper colors or theme toggle in this cut.
- Inspect dependencies with gradlew.bat :app:dependencies --configuration devReleaseCompileClasspath from apps/android; the flavor-specific configuration matters.
- Use the expressive controls specified by current screens/golds, including ButtonGroup where required and LoadingIndicator for applicable loading states. Obsolete T1/T3 screen instructions do not apply.
- Preserve the Home FAB that opens Chat. Do not introduce showcase app bars/bottom navigation/rails.
- CTA colors are theme-specific in tokens: dark uses the light foreground on the dark background; light uses the dark foreground on the light background. Gold is an accent, not a replacement CTA fill.
- Apply each component's specified shape. The sheet's top radius, card/chip radius and Chat-specific bubble/composer shapes are different rules.

Use appropriate experimental opt-ins for APIs already approved in the stack. An opt-in is not approval to migrate to unrelated experimental APIs. Validate dev behavior/regression checks and fresh dark/light emulator gold comparisons for changed UI.
