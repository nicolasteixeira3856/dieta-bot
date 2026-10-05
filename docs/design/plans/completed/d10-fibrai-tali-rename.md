# Plan — D10 Fibrai and Tali in the golds

- Status: Concluído
- Date: 05/10/2026
- Owning context: `design`
- Executable boundary: Figma `Design` (components `Componentes`, the existing flow sections on `Release 1`, the `Branding` page for the wordmark only, and the phone screens of the section `Landing · D11` on the `Landing page` page). Repository outputs: re-exported PNGs in `docs/qa/figma/{dark,light}/` for the ids that show the old name, `tools/export-figma.mjs` if node ids change, and the temporary avatar image in `docs/design/brand/`. No `apps/` or `server/` code.
- Prerequisites: approval of this plan accepts [ADR-035](../../../produto/adrs/ADR-035-tali-in-app-identity.md). [ADR-034](../../../produto/adrs/ADR-034-fibrai-brand-tali-assistant.md) accepted.
- Figma MCP budget: at most 110 calls in one day ([ADR-031](../../adrs/ADR-031-figma-source-of-truth.md) § 6). The name lives in a few components, so most frames update through their instances.

Authorization and delivery follow [SDD](../../../sdd/README.md). Approval: `Aprovo o plano docs/design/plans/d10-fibrai-tali-rename.md. Implemente o plano aprovado.`

## Objective

Replace "Dieta Bot" with "Fibrai" on brand surfaces, and with "Tali" plus the avatar bubble on assistant surfaces, in every gold, in both themes. No other layout change.

## Sources

- [ADR-035](../../../produto/adrs/ADR-035-tali-in-app-identity.md): copy and the avatar rules.
- [Chat specification](../../../produto/specifications/chat.md) and [profile/onboarding specification](../../../produto/specifications/perfil-onboarding.md): the live behavior.
- [Gold inventory](../../../qa/README.md#golds): ids. Code: `apps/android/.../feature/{splash,onboarding,chat}/` and `core/designsystem/aero/AeroChatComponents.kt`.

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
7. **Landing phone screens** ([D11](d11-landing-page.md)):
   - the phones of `land` and `landM` (both rows) hold static clones of the `o1`, `home1` and `chatE` golds, taken before this rename;
   - replace each clone with a fresh clone of the renamed gold, scaled to 300 px wide, and clear its explicit Color mode so it follows the frame;
   - add `land` and `landM` to the export list. [W1](../../../site/plans/w1-landing-site.md) compares against them.

## Intended documentation changes

None at D10 Completion besides Results and this plan's lifecycle. The product specifications change when [A49](../../../android/plans/a49-fibrai-tali-visible-rename.md) delivers.

## Out of scope

- Final avatar art: when the owner delivers it, a follow-up design plan swaps the image fill and re-exports.
- Logo and app icon.
- Home, Config and push frames, unless discovery finds the name there; then they enter the same edit.
- Compose, server and the landing layout ([D11](d11-landing-page.md)); only the phone screens of scope item 7 change.

## Validation

1. The discovery table exists in Results before the first write.
2. A read-back shows no text node containing "Dieta Bot" on `Release 1` or `Componentes`, and avatar instances are not detached.
3. Screenshots of every affected frame; the owner's OK in Figma.
4. `node tools/check-figma.mjs` and `node tools/check-docs.mjs` pass. Unaffected golds have no byte diff.

## Results

Approved by the owner on 2026-10-05 ("Aprovo o plano docs/design/plans/d10-fibrai-tali-rename.md. Implemente o plano aprovado."). The approval accepts [ADR-035](../../../produto/adrs/ADR-035-tali-in-app-identity.md).

### Discovery (before the first write)

Read-only search of every page of the Figma file for text containing "Dieta" (2026-10-05), cross-checked with the code (`OnboardingAeroScreens.kt`, `ChatScreen.kt`, `AeroChatComponents.kt`, `DietaBotTokens.kt`, `strings.xml`). Classes: `app` (the code draws it, A49 renames it), `copy` (only the text changes).

| Surface | Figma node | Kind | Gold ids | Class | Change |
|---|---|---|---|---|---|
| Splash wordmark "Dieta Bot" (`Hero/Number`) | `Wordmark` text in each frame | frame text | `splash` | app (`WORDMARK`) | "Fibrai" |
| Onboarding label "DIETA BOT INTAKE" (`Label/Section`) | `Top bar/Bar/Label` in each frame | frame text | `o2`, `o3`, `o3t`, `o3s` | app | "FIBRAI INTAKE" |
| Onboarding header "Dieta Bot" (`Title`) | `Top bar/Bar/Wordmark` | frame text | `o4` | app | "Fibrai" |
| Chat header "Chat Dieta Bot" + accent dot | `Chat/Header` (`60:136`) | component, 28 instances on `Release 1` | `chat0`, `chatL`, `chatQ`, `chatE`, `chatX`, `chatT`, `chatF`, `chatA`, `chatG`, `chatU`, `chatD`, `chatR`, `chatM`, `chatS` | app | `Chat/TaliAvatar` header + "Tali" |
| Bot label: lightning well + "Dieta Bot AI" | `Chat/BotLabel` (`60:110`), also nested in `Chat/Question` (`60:155`) | component, 20 instances on `Release 1` | `chatQ`, `chatE`, `chatT`, `chatF`, `chatG`, `chatU`, `chatD`, `chatR`, `chatM` | app | `Chat/TaliAvatar` label + "Tali", well removed |
| Loading caption "Dieta Bot AI" under the loader bubble (`Caption`, `text/muted`, no icon) | `Loading/Label` text | frame text | `chatL` | app (`ChatScreen.kt` loading row) | "Tali" (text only: this caption has no icon today) |
| System notification app name "Dieta Bot" | `Notification/Push` (`76:188`) | component, 2 instances | `push` | app (`app_name`) | "Fibrai" |
| Landing phone screens (gold clones, old name) | `Phone · o1/home1/chatE` screens in `land` (`106:530`, `107:853`) and `landM` (`106:716`, `107:1003`) | clone | `land`, `landM` | — | fresh clones of the renamed golds |

- **No name:** `o1` and `o1e` (their header is the step eyebrow and "Teto do dia"), `chatP`, every Home id, `cfg`, `cfgS`, `wipe`, `priv`. They are not re-exported.
- **Not touched:**
  - `Cores e tipografia` lists the Kotlin token names `DietaBotColors.*` and `DietaBotMotion.*`, which are technical identifiers (ADR-016, [ADR-036](../../../android/adrs/ADR-036-fibrai-technical-identity.md)), not visible copy.
  - The `Exploração · D11` frames A, B and C keep the old clones as history.
  - `Web/Avatar` keeps its 48 px monogram, because swapping it would change the landing layout.
- **Export list (23 ids):** `splash`, `o2`, `o3`, `o3t`, `o3s`, `o4`, `chat0`, `chatL`, `chatQ`, `chatE`, `chatX`, `chatT`, `chatF`, `chatA`, `chatG`, `chatU`, `chatD`, `chatR`, `chatM`, `chatS`, `push`, `land`, `landM`.

### Delivered in Figma (2026-10-05)

**Temporary avatar image:** a 128 px frame (accent/default + the `Gloss/Button` sheen, "T" in Nunito Sans Bold 64, `accent/on`, Light mode) was exported at 2x through the REST API and then deleted. The result is `docs/design/brand/tali-avatar-placeholder.png` (256 × 256), uploaded to the file as an image fill.

**`Componentes`, section `Chat`:**

- New `Chat/TaliAvatar` (`114:192`), in a wrapper frame at the end of `Components`. Variants:
  - `Size=Header` (`114:190`), 32 px;
  - `Size=Label` (`114:191`), 20 px.
- Each variant has the image fill, a 1 px `border/glass` ring inside and corners bound to `radius/pill`. The description names the PNG and says the final art replaces only the image fill.
- `Chat/BotLabel` (`60:110`): the lightning well is deleted; `Chat/TaliAvatar` Label + "Tali" (`Caption/Strong`, `text/primary`), 6 px gap, 20 px high. `Chat/Question` follows through its nested instance.
- `Chat/Header` (`60:136`):
  - The `Title` slot is now horizontal, centered, 8 px gap: `Chat/TaliAvatar` Header next to a new `Text` column. That column keeps "Tali" + accent dot over "ASSISTENTE DE REFEIÇÕES", left-aligned to the avatar.
  - Height stays 44.
- `Notification/Push` (`76:188`): app name "Fibrai".
- The descriptions of `Chat/Header` and `Chat/BotLabel` were rewritten for the new content.

**`Release 1`:** 14 frame texts changed (splash and `o4` "Fibrai", `o2`/`o3`/`o3t`/`o3s` "FIBRAI INTAKE", `chatL` "Tali"), Light and Dark. The 28 header and 20 label instances follow their components: 48 avatar instances, none detached, every `Chat/Header` instance 44 px high.

**`Landing page`:** the eight phone-screen clones (`land` o1, home1 and chatE; `landM` home1; both rows) were replaced by fresh clones of the Light golds. Each is scaled to 300 px with no explicit Color mode, at the old position and size (o1 300 × 760, home1 300 × 1094, chatE 300 × 649).

**`Branding`:**

- The page description says "Nome: Fibrai. Assistente: Tali (ADR-034). Logo e marca: a definir."
- New section `Wordmark · D10` (`115:2`) with "Fibrai" in `Hero/Number`, `text/primary` on `Background/Page`, Light above Dark.

**Read-back:**

- No text node and no component description contains "Dieta" on `Release 1` or `Componentes`.
- The three avatar instances on `Componentes` (`Chat/BotLabel`, `Chat/Header`, nested in `Chat/Question`) point to `Chat/TaliAvatar`.

### Owner gate

- Review images were exported through the REST dry run and sent to the owner on 2026-10-05. They cover every affected frame in Light and Dark, `land` and `landM`, the `Chat/TaliAvatar`, `Chat/BotLabel` and `Chat/Header` components, and the `Branding` section.
- Open points for the review:
  - the header text column is left-aligned next to the avatar (the old title was centered);
  - the `chatL` loading caption shows only "Tali", with no avatar;
  - the avatar ring is 1 px `border/glass` at both sizes.
- The plan waited in `pending_manual_validation/` for the owner's OK in Figma.

**Owner's OK in Figma:** 2026-10-05 ("Aprovado no Figma, pode exportar os golds"). The three open points stay as drawn.

### Gold export (2026-10-05)

- **Export:** `node tools/export-figma.mjs --only <ids>` in six batches. One request for all 23 ids timed out (`fetch failed`). No node id changed, so `tools/export-figma.mjs` is unchanged.
- **Noise filter override:** the filter (< 0.05 % changed pixels) restored five PNGs from git, although the name had changed in them:
  - `o3t` Light and Dark: "FIBRAI INTAKE" under the time-picker scrim;
  - `chatT` Light: the header under the sheet scrim;
  - `land` Light and Dark: the `chatE` phone header.

  They were exported again with `--dry-run` and copied over the golds. A crop of each one shows the new name.
- **Unchanged on purpose:** `landM` rendered identical to git (0.000 %), because its only phone shows `home1`, which has no name.
- **Changed golds:** 44 PNGs, 22 ids × 2 themes: `splash`, `o2`, `o3`, `o3t`, `o3s`, `o4`, `chat0`, `chatL`, `chatQ`, `chatE`, `chatX`, `chatT`, `chatF`, `chatA`, `chatG`, `chatU`, `chatD`, `chatR`, `chatM`, `chatS`, `push`, `land`. No other gold has a byte diff.
- **Inventory:** sources do not change; every id is already `figma`.
- **Checks:** `node tools/check-figma.mjs` (68 PNGs) and `node tools/check-docs.mjs` pass.
- **Next:** [A49](../../../android/plans/a49-fibrai-tali-visible-rename.md) compares against these PNGs and ships `docs/design/brand/tali-avatar-placeholder.png`. [W1](../../../site/plans/w1-landing-site.md) takes the phone screens from `o1`, `home1` and `chatE`.

### Figma MCP budget

Total: 22 calls of 110, all on day 1 (2026-10-05). The export used none:

- 1 `whoami` and 3 skill reads;
- 17 `use_figma`: 12 read-only (discovery, inspection, read-backs) and 5 writes (temporary avatar source, `Componentes`, `Release 1`, `Landing page`, `Branding`);
- 1 `upload_assets`, which put the placeholder PNG into the file.

Review images come from the REST export and cost no MCP call.
