# Plan — D10 Fibrai and Tali in the golds

- Status: Aguardando aprovação
- Date: 05/10/2026
- Owning context: `design`
- Executable boundary: Figma `Design` (components `Componentes`, the existing flow sections on `Release 1`, the `Branding` page for the wordmark only, and the phone screens of the section `Landing · D11` on the `Landing page` page). Repository outputs: re-exported PNGs in `docs/qa/figma/{dark,light}/` for the ids that show the old name, `tools/export-figma.mjs` if node ids change, and the temporary avatar image in `docs/design/brand/`. No `apps/` or `server/` code.
- Prerequisites: approval of this plan accepts [ADR-035](../../produto/adrs/ADR-035-tali-in-app-identity.md). [ADR-034](../../produto/adrs/ADR-034-fibrai-brand-tali-assistant.md) accepted.
- Figma MCP budget: at most 110 calls in one day ([ADR-031](../adrs/ADR-031-figma-source-of-truth.md) § 6). The name lives in a few components, so most frames update through their instances.

Authorization and delivery follow [SDD](../../sdd/README.md). Approval: `Aprovo o plano docs/design/plans/d10-fibrai-tali-rename.md. Implemente o plano aprovado.`

## Objective

Replace "Dieta Bot" with "Fibrai" on brand surfaces, and with "Tali" plus the avatar bubble on assistant surfaces, in every gold, in both themes. No other layout change.

## Sources

- [ADR-035](../../produto/adrs/ADR-035-tali-in-app-identity.md): copy and the avatar rules.
- [Chat specification](../../produto/specifications/chat.md) and [profile/onboarding specification](../../produto/specifications/perfil-onboarding.md): the live behavior.
- [Gold inventory](../../qa/README.md#golds): ids. Code: `apps/android/.../feature/{splash,onboarding,chat}/` and `core/designsystem/aero/AeroChatComponents.kt`.

## Scope

1. **Discovery (read only, before any write):**
   - Read the Figma file and list every node whose text contains "Dieta Bot", with page, component or instance, and gold id.
   - Expected surfaces:
     - the splash wordmark (`splash`);
     - the onboarding header and "Dieta Bot Intake" label (`o1`, `o1e`, `o2`, `o3`, `o3t`, `o3s`, `o4`);
     - the Chat header "Chat Dieta Bot" and the bot label "Dieta Bot AI" (the Chat ids that show them).
   - The table of affected ids goes into Results and decides the export list. Ids without the name are not re-exported.
2. **Avatar component** in `Componentes`:
   - `Chat/TaliAvatar`, with size variants `header` (32 px) and `label` (20 px);
   - circular image fill inside an Aero glass ring, variables only;
   - the temporary image is a "T" monogram in Nunito Sans on the accent gradient.

   Store the temporary image as `docs/design/brand/tali-avatar-placeholder.png` (at least 256 px) so the client can ship the same pixels.
3. **Copy and slots** (component-level edits, so instances follow):
   - Splash wordmark → "Fibrai".
   - Onboarding header → "Fibrai"; section label → "Fibrai Intake".
   - Chat header title → `TaliAvatar/header` + "Tali".
   - Bot label → `TaliAvatar/label` + "Tali". The lightning icon well is removed.
   - Put the "Fibrai" wordmark text style on `Branding` as reference. That page holds no logo work.
4. **Review of every affected frame, Light then Dark (mode switch only):**
   - no overlap or wrap regression;
   - header height unchanged;
   - label baseline aligned with the bubble;
   - semantic macro colors untouched.
5. **Owner gate:** one screenshot per affected frame. The plan moves to `pending_manual_validation/` until the owner's OK in Figma, and fixes are re-screenshotted.
6. **Export after OK:**
   - `node tools/export-figma.mjs --only <affected ids>`, then `node tools/check-figma.mjs`;
   - inventory sources do not change, because every id is already `figma`;
   - A49 compares against these PNGs.
7. **Landing phone screens** ([D11](completed/d11-landing-page.md)):
   - the phones of `land` and `landM` (both rows) hold static clones of the `o1`, `home1` and `chatE` golds, taken before this rename;
   - replace each clone with a fresh clone of the renamed gold, scaled to 300 px wide, and clear its explicit Color mode so it follows the frame;
   - add `land` and `landM` to the export list. [W1](../../site/plans/w1-landing-site.md) compares against them.

## Intended documentation changes

None at D10 Completion besides Results and this plan's lifecycle. The product specifications change when [A49](../../android/plans/a49-fibrai-tali-visible-rename.md) delivers.

## Out of scope

- Final avatar art: when the owner delivers it, a follow-up design plan swaps the image fill and re-exports.
- Logo and app icon.
- Home, Config and push frames, unless discovery finds the name there; then they enter the same edit.
- Compose, server and the landing layout ([D11](completed/d11-landing-page.md)); only the phone screens of scope item 7 change.

## Validation

1. The discovery table exists in Results before the first write.
2. A read-back shows no text node containing "Dieta Bot" on `Release 1` or `Componentes`, and avatar instances are not detached.
3. Screenshots of every affected frame; the owner's OK in Figma.
4. `node tools/check-figma.mjs` and `node tools/check-docs.mjs` pass. Unaffected golds have no byte diff.

## Results

Planning only. No Figma calls yet.
