# Plan — A49 Fibrai and Tali in the app

- Status: Concluído
- Date: 05/10/2026
- Owning context: `android`
- Executable boundary:
  - `apps/android/` only: string resources (`app_name` in main and dev), the splash wordmark constant, the onboarding header and label, the Chat header and bot label, a new avatar drawable and composable, and their tests;
  - also the tag message in `tools/distribute-dev.ps1` ("Dieta Bot" → "Fibrai").
- Related documentation:
  - product [Chat](../../../produto/specifications/chat.md) and [profile/onboarding](../../../produto/specifications/perfil-onboarding.md) specifications;
  - `AGENTS.md` "Product name" line;
  - [android README](../../README.md); captures in `docs/qa/android/current/`.
- Prerequisites:
  - [D10](../../../design/plans/completed/d10-fibrai-tali-rename.md) `Concluído` with exported golds, which accepts [ADR-035](../../../produto/adrs/ADR-035-tali-in-app-identity.md);
  - [S20](../../../server/plans/completed/s20-tali-prompt-identity.md) on the dev server is recommended but not required;
  - [A48](a48-fibrai-app-id-firebase.md) `Concluído`, so this plan edits the moved `app/fibrai/android` sources.

Authorization and delivery follow [SDD](../../../sdd/README.md). Approval: `Aprovo o plano docs/android/plans/a49-fibrai-tali-visible-rename.md. Implemente o plano aprovado.`

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
3. Class, package and storage names are already Fibrai from [A48](a48-fibrai-app-id-firebase.md). This plan changes only visible copy and the avatar.
4. **Specifications at Completion** (written in the present tense):
   - Chat: the AI label is the avatar plus "Tali"; the rule "Sem foto de perfil" is removed; the header title is "Tali".
   - Profile/onboarding: the splash wordmark and onboarding header say "Fibrai".
   - Add A49 to each spec's Provenance.
5. **`AGENTS.md`:** "Product name: Fibrai. Assistant: Tali (ADR-034, ADR-035)." This replaces the "Dieta Bot" line. Other docs and skills prose follow in [SD5](../../../sdd/plans/sd5-fibrai-docs-prose.md).

## Out of scope

- applicationId and Firebase ([A48](a48-fibrai-app-id-firebase.md)).
- Server prompt ([S20](../../../server/plans/completed/s20-tali-prompt-identity.md)).
- Launcher icon or logo, the final avatar art, the landing site.

## Validation

1. `./gradlew testDevDebugUnitTest verifyRoborazziDevDebug assembleDevRelease` passes; Roborazzi baselines are re-recorded only for the renamed screens.
2. Emulator captures of the affected ids, per the D10 Results list, in both themes, saved to `docs/qa/android/current/{dark,light}/`. Compare them with the D10 golds and write the diff list (layout, tokens, type size, radius, CTA, avatar size and alignment). Iterate until they match. Partial validation applies: only the touched flows (splash, onboarding, Chat).
3. `grep -rn "Dieta Bot" apps/android/app/src` returns nothing.
4. `node tools/check-docs.mjs` passes.

## Results

Started on 2026-10-05 at the owner's request ("já comece o A49"), right after the owner closed [A48](a48-fibrai-app-id-firebase.md). Implemented on `feat/a49-fibrai-tali` from master `38cf238`.

### Delivered

- **Copy:**
  - `app_name` "Fibrai" (main) and "Fibrai Dev" (dev); `WORDMARK` "Fibrai";
  - onboarding header "Fibrai" (O4) and "Fibrai Intake" (O2, O3);
  - Chat header and bot label "Tali", and the loading caption "Tali".
- **Avatar:** `TaliAvatar(TaliAvatarSize.Header | Label)` in `core/designsystem/aero` (32 / 20 dp, circle, 1 dp `border/glass` ring, decorative). The image `R.drawable.tali_avatar` is the D10 PNG `docs/design/brand/tali-avatar-placeholder.png`, copied byte for byte into `drawable-nodpi`.
- **Layout:**
  - The header shows the avatar next to a column with "Tali" + the accent dot over "ASSISTENTE DE REFEIÇÕES", 8 dp apart, with the group centred.
  - The bot label shows the avatar + "Tali", 6 dp apart, 20 dp high. The lightning well is gone.
- **Tooling:** `tools/distribute-dev.ps1` uses "Fibrai 0.0.N" as the release-notes header and "Fibrai …" as the tag message.
- **Docs:**
  - AGENTS product line;
  - product [Chat](../../../produto/specifications/chat.md) rules 2 and 14 (the "sem foto de perfil" rule is gone);
  - [profile/onboarding](../../../produto/specifications/perfil-onboarding.md) new rule 9 for the visible name. It is appended, so the rule numbers that history cites stay valid.
  - android README (flavor labels, notes header) and Provenance in both specifications.

### Validation

1. `testDevDebugUnitTest` and `verifyRoborazziDevDebug`: **548/548 pass**. The 3 GoldTests that failed since D10 (`chatF` ×2, `chatL` dark) pass now.
   - Roborazzi re-recorded only `chatQ` (dark, light). The compare image showed differences only in the header and the two bot labels; the other 54 captures are unchanged.
   - `assembleDevRelease`: `aapt2` shows `application-label:'Fibrai Dev'`, package `app.fibrai.android.dev`.
2. JVM GoldTest against the D10 golds (blurred gate 2 %):
   - splash 0.00 / 0.00 %; o2 0.07 / 0.04 %; o4 0.10 / 0.07 %;
   - chat0 0.13 / 0.08 %; chatL 0.00 / 0.00 %; chatQ 0.02 %; chatE 0.02–0.03 %; chatU 0.01–0.03 %;
   - chatF passes its regions; chatA 0.67 / 0.66 %; chatX 0.10 / 0.08 %; the rest of the Chat ids are at most 0.76 %.

   Emulator (`Medium_Phone` at gold geometry, fake server, `tools/capture-chat.sh` in both themes; fresh captures in `docs/qa/android/current/{dark,light}/`):
   - Captured ids: `splash`, `o2`, `o3`, `o3t`, `o3s`, `o4`, `chat0`, `chatL`, `chatQ`, `chatE`, `chatT`, `chatG`, `chatU`, `chatD`, `chatR`, `chatM`, `chatS`.
   - `tools/diff-gold.mjs`:
     - every full-frame and region gate passes in both themes: splash 0.00 %, onboarding at most 1.26 %, chat0 at most 0.33 %, chatE at most 0.27 %;
     - every header/label region 0.00 %. Before this plan, `light/chatU` region was 7.37 %;
     - full frames of conversations that fill the screen stay report-only, as before.
   - Script checks: 125/130 (dark) and 126/130 (light).
     - The 4 failures in both themes are the `chatX` scene: `adb input` truncated the pasted text (1563 / 1698 characters), so the over-2000 state was never reached. Those `chatX` captures were discarded. `chatX` is covered by its JVM GoldTest.
     - The fifth failure (dark only, "composer not filled" after Editar) passed in light; it is a flaky dump in code this plan does not touch.
   - Not captured on the emulator: `chatA` and `chatF` (the photo flow needs the virtual-scene camera AVD), `chatX` (above) and `push` (outside the plan's partial-validation flows). Their golds pass the JVM GoldTest. `o1`/`o1e` have no name and were reverted.

   Diff list against D10:

   | Check | Result |
   |---|---|
   | Layout | unchanged except the header and label rows |
   | Avatar | 32 dp in the header and 20 dp in the label, centred on the text line |
   | Header | 44 dp high, the group centred |
   | Tokens | `border/glass` ring; `text/primary` "Tali"; accent dot unchanged |
   | Type | Body/Strong title; Caption/Strong label; Caption for the loading text |
   | CTA and radius | not touched |
3. `grep -rn "Dieta Bot" apps/android/app/src`: no match.
4. `node tools/check-docs.mjs`: passed.
