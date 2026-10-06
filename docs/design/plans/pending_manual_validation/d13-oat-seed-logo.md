# Plan — D13 Oat seed logo and logo-only splash

- Status: Pendente aprovação manual
- Date: 06/10/2026
- Owning context: `design`
- Executable boundary: the `Branding` page and the `splash` frames in Figma `Design`; no app/server code. Repository outputs: the re-exported golds `splash` (dark, light) and design documentation.
- Prerequisites: the brand sources in `design/brand/` already replaced by the owner-chosen oat seed (this plan's Discovery records them); [D10](../completed/d10-fibrai-tali-rename.md) golds in place.
- Figma MCP budget: at most 25 calls, within the ceiling and rollover rules of [ADR-031](../../adrs/ADR-031-figma-source-of-truth.md).

Authorization and delivery follow [SDD](../../../sdd/README.md). Approved on 06/10/2026 (`Aprovo o plano docs/design/plans/d13-oat-seed-logo.md. Implemente o plano aprovado.`).

## Objective

Two owner decisions of 06/10/2026:

1. Replace the A14 placeholder (gold ring with a robot) by the Fibrai logo the owner chose: a single stylized oat seed with a minimal face, flat, two fills and a dark outline. The name reads as "fibra" ([ADR-034](../../../produto/adrs/ADR-034-fibrai-brand-tali-assistant.md)), and the seed makes the food meaning clear in the icon, which ADR-034 lists as open.
2. The splash shows only the logo, centered and large. No wordmark, no disclaimer, no divider. The disclaimer keeps its place on the Home ([perfil-onboarding](../../../produto/specifications/perfil-onboarding.md) rule 7), so the product rule "estimate, not advice" still holds; the name stays visible in the launcher label, the onboarding headers and the Home (rule 9).

## Sources

- Master vector: `design/brand/icon.svg` (1024 viewBox, three fills `#3A2A14` outline, `#E9C984` grain, `#F4E4BB` highlight, dark face). Traced by the agent from the owner's chosen raster; the raster is not kept.
- Themed-icon mask: `design/brand/icon-mono.svg` (white silhouette, face cut out).
- Rasters for the build: `design/brand/icon.png`, `icon-mono.png`, `icon-dark-bg.png` (reference only), consumed by `tools/brand-icons.ps1` ([android README § Marca](../../../android/README.md#marca)).
- Site favicon: `web/public/img/favicon.svg` plus 32, 180 and 512 px PNGs ([site README](../../../site/README.md)).

The launcher and the favicon have no gold. Only the Compose splash shows the logo inside a gold, so only `splash` changes.

Proposed `splash` layout (to be confirmed by the owner in the Figma review): the Aero background of the current frame (gradient and glass circles unchanged); the logo `Logo / Mark` alone, centered on both axes, 160 dp tall (the tight crop, so the tilted seed is about 92 dp wide); nothing else on the frame.

## Scope

1. **Discovery before any write.** Read the live `splash` frames (ids in `tools/export-figma.mjs`) and the `Branding` page. Record the current logo node and its box (120 dp, 24 dp above the wordmark per A14).
2. **Branding page.** Place the master SVG as a component `Logo / Mark` with the three fills bound to local brand variables (`brand/outline`, `brand/grain`, `brand/highlight`; same value in Light and Dark). Add the monochrome variant. No wordmark change.
3. **Splash.** In `splash · Light` and `splash · Dark`: remove the wordmark, the divider and the disclaimer text; place one `Logo / Mark` instance centered, 160 dp tall. Background untouched.
4. **Owner gate.** Screenshot both frames through the MCP, link the section, move this plan to `pending_manual_validation/` and record the call count. No export before the owner's OK.
5. **Export after OK.** `node tools/export-figma.mjs --only splash`, then `node tools/check-figma.mjs` and `node tools/check-docs.mjs`. Other golds keep their bytes.

## Client follow-up

[A51](../../../android/plans/a51-logo-only-splash.md) changes the Compose splash to the new frame and re-records the baselines; it starts only after this plan is `Concluído`. The regenerated `logo_mark` raster is already in place. If the icon background `#0B0D10` is revisited (the dark outline disappears on it), that is a separate decision, not this plan.

## Intended documentation changes

At Completion: the design index links the delivered design and the `splash` golds are re-exported. The product rules about the splash copy (rules 7 and 9 of [perfil-onboarding](../../../produto/specifications/perfil-onboarding.md)) are updated by A51, not here.

## Out of scope

Wordmark design, store listing art, any other screen, token edits outside the three brand variables, the icon background color, Material You tinting rules.

## Validation

1. Discovery note with the previous node and box.
2. Two frame screenshots (Light, Dark) with the seed alone, centered, 160 dp tall, and no text on the frame.
3. Explicit owner review in Figma.
4. Re-exported `splash` golds; check-figma and check-docs pass; no unrelated gold diff; splash Roborazzi gate within the approved threshold ([qa README](../../../qa/README.md)).
5. SDD git delivery.

## Results

Implementation on 06/10/2026 (Figma Student seat Full).

### Discovery (before the first write)

Live `splash` frames (`51:732` Light, `54:1233` Dark, section `Splash e onboarding · D4`): vertical auto layout centered on both axes, gradient background bound to Aero variables, three absolute `Bubble` ellipses, then in flow: `Mark` (120 × 120 rectangle with the A14 image fill), `Gap` 16, `Wordmark`, `Gap` 10, `Accent bar`, `Gap` 28, `Disclaimer`. `Branding` page `17:2`: page header and the `Wordmark · D10` section only; no logo component, no `brand/*` variable.

| Element | Source | Decision |
|---|---|---|
| Background gradient and bubbles | app (`SplashScreen`) | kept, untouched |
| `Mark` 120 dp placeholder | gold-only (A14 image) | replaced by the `Logo / Mark` instance |
| Wordmark `Fibrai`, accent bar, gaps | app | removed (owner decision 06/10/2026, rule 9 updated by A51) |
| Disclaimer | app (`SplashBoot.COPY`) | removed from the splash; stays on the Home (rule 7 updated by A51) |
| Logo 160 dp centered | spec (this plan) | drawn |

### Written

- Color collection: `brand/outline` `VariableID:132:2` (#3A2A14), `brand/grain` `VariableID:132:3` (#E9C984), `brand/highlight` `VariableID:132:4` (#F4E4BB); same value in Light and Dark, scopes shape fill and stroke, code syntax `var(--color-brand-*)` / `DietaBotColors.brand*` / `DietaBotColor.brand*`.
- `Branding` page: section `Logo · D13` (`132:22`) with the component set `Logo / Mark` (`132:21`): `Style=Color` (`132:12`, fills bound to the three brand variables, built from `design/brand/icon.svg`, tight crop, children with SCALE constraints) and `Style=Mono` (`132:20`, `icon/primary` silhouette with the face in `bg/page`). Description on the set.
- `splash · Light` `51:732` and `splash · Dark` `54:1233`: `Mark`, the three `Gap`s, `Wordmark`, `Accent bar` and `Disclaimer` removed; one `Logo` instance of `Style=Color` (`132:4293`, `132:4300`), 91.2 × 160, centered by the frame's auto layout (x 149.4, y 342). Background and bubbles untouched.

### Figma MCP calls

10 of 25: whoami 1, skill reads 2, metadata 2, use_figma 3 (discovery, Branding, splash), screenshots 2.

### Owner gate

Screenshots of both frames sent to the owner on 06/10/2026. Waiting for the review in Figma. After the OK: `node tools/export-figma.mjs --only splash`, `node tools/check-figma.mjs`, `node tools/check-docs.mjs`, move to `completed/`.
