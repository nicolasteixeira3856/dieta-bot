# Plan — A49 Fibrai and Tali in the app

- Status: Aguardando aprovação
- Date: 05/10/2026
- Owning context: `android`
- Executable boundary:
  - `apps/android/` only: string resources (`app_name` in main and dev), the splash wordmark constant, the onboarding header and label, the Chat header and bot label, a new avatar drawable and composable, and their tests;
  - also the tag message in `tools/distribute-dev.ps1` ("Dieta Bot" → "Fibrai").
- Related documentation:
  - product [Chat](../../produto/specifications/chat.md) and [profile/onboarding](../../produto/specifications/perfil-onboarding.md) specifications;
  - `AGENTS.md` "Product name" line;
  - [android README](../README.md); captures in `docs/qa/android/current/`.
- Prerequisites:
  - [D10](../../design/plans/completed/d10-fibrai-tali-rename.md) `Concluído` with exported golds, which accepts [ADR-035](../../produto/adrs/ADR-035-tali-in-app-identity.md);
  - [S20](../../server/plans/s20-tali-prompt-identity.md) on the dev server is recommended but not required;
  - [A48](completed/a48-fibrai-app-id-firebase.md) `Concluído`, so this plan edits the moved `app/fibrai/android` sources.

Authorization and delivery follow [SDD](../../sdd/README.md). Approval: `Aprovo o plano docs/android/plans/a49-fibrai-tali-visible-rename.md. Implemente o plano aprovado.`

## Objective

The user sees "Fibrai" on the launcher, splash and onboarding, and "Tali" with her avatar bubble in the Chat, matching the D10 golds pixel for pixel per the AGENTS visual QA.

## Scope

1. **Copy:**
   - `app_name` → "Fibrai" (main) and "Fibrai Dev" (dev).
   - `WORDMARK` → "Fibrai".
   - The onboarding header → "Fibrai" and "Fibrai Intake".
   - The Chat header → avatar + "Tali".
   - The bot label → avatar + "Tali". The lightning well in `AeroChatComponents` is removed.
2. **Avatar:**
   - Add `TaliAvatar(size: Header | Label)` to `core/designsystem/aero`, matching the D10 component: circle, glass ring, sizes 32 and 20 dp.
   - The temporary image is the D10 PNG imported as `R.drawable.tali_avatar`.
   - Decorative: `contentDescription = null`, because the adjacent "Tali" text carries the name.
   - The final art later replaces only the drawable.
3. Class, package and storage names are already Fibrai from [A48](completed/a48-fibrai-app-id-firebase.md). This plan changes only visible copy and the avatar.
4. **Specifications at Completion** (written in the present tense):
   - Chat: the AI label is the avatar plus "Tali"; the rule "Sem foto de perfil" is removed; the header title is "Tali".
   - Profile/onboarding: the splash wordmark and onboarding header say "Fibrai".
   - Add A49 to each spec's Provenance.
5. **`AGENTS.md`:** "Product name: Fibrai. Assistant: Tali (ADR-034, ADR-035)." This replaces the "Dieta Bot" line. Other docs and skills prose follow in [SD5](../../sdd/plans/sd5-fibrai-docs-prose.md).

## Out of scope

- applicationId and Firebase ([A48](completed/a48-fibrai-app-id-firebase.md)).
- Server prompt ([S20](../../server/plans/s20-tali-prompt-identity.md)).
- Launcher icon or logo, the final avatar art, the landing site.

## Validation

1. `./gradlew testDevDebugUnitTest verifyRoborazziDevDebug assembleDevRelease` passes; Roborazzi baselines are re-recorded only for the renamed screens.
2. Emulator captures of the affected ids, per the D10 Results list, in both themes, saved to `docs/qa/android/current/{dark,light}/`. Compare them with the D10 golds and write the diff list (layout, tokens, type size, radius, CTA, avatar size and alignment). Iterate until they match. Partial validation applies: only the touched flows (splash, onboarding, Chat).
3. `grep -rn "Dieta Bot" apps/android/app/src` returns nothing.
4. `node tools/check-docs.mjs` passes.

## Results

Planning only.
