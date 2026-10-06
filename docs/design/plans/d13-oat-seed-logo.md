# Plan — D13 Oat seed logo and logo-only splash

- Status: Aguardando aprovação
- Date: 06/10/2026
- Owning context: `design`
- Executable boundary: the `Branding` page and the `splash` frames in Figma `Design`; no app/server code. Repository outputs: the re-exported golds `splash` (dark, light) and design documentation.
- Prerequisites: the brand sources in `design/brand/` already replaced by the owner-chosen oat seed (this plan's Discovery records them); [D10](completed/d10-fibrai-tali-rename.md) golds in place.
- Figma MCP budget: at most 25 calls, within the ceiling and rollover rules of [ADR-031](../adrs/ADR-031-figma-source-of-truth.md).

Authorization and delivery follow [SDD](../../sdd/README.md). Approval: `Aprovo o plano docs/design/plans/d13-oat-seed-logo.md. Implemente o plano aprovado.`

## Objective

Two owner decisions of 06/10/2026:

1. Replace the A14 placeholder (gold ring with a robot) by the Fibrai logo the owner chose: a single stylized oat seed with a minimal face, flat, two fills and a dark outline. The name reads as "fibra" ([ADR-034](../../produto/adrs/ADR-034-fibrai-brand-tali-assistant.md)), and the seed makes the food meaning clear in the icon, which ADR-034 lists as open.
2. The splash shows only the logo, centered and large. No wordmark, no disclaimer, no divider. The disclaimer keeps its place on the Home ([perfil-onboarding](../../produto/specifications/perfil-onboarding.md) rule 7), so the product rule "estimate, not advice" still holds; the name stays visible in the launcher label, the onboarding headers and the Home (rule 9).

## Sources

- Master vector: `design/brand/icon.svg` (1024 viewBox, three fills `#3A2A14` outline, `#E9C984` grain, `#F4E4BB` highlight, dark face). Traced by the agent from the owner's chosen raster; the raster is not kept.
- Themed-icon mask: `design/brand/icon-mono.svg` (white silhouette, face cut out).
- Rasters for the build: `design/brand/icon.png`, `icon-mono.png`, `icon-dark-bg.png` (reference only), consumed by `tools/brand-icons.ps1` ([android README § Marca](../../android/README.md#marca)).
- Site favicon: `web/public/img/favicon.svg` plus 32, 180 and 512 px PNGs ([site README](../../site/README.md)).

The launcher and the favicon have no gold. Only the Compose splash shows the logo inside a gold, so only `splash` changes.

Proposed `splash` layout (to be confirmed by the owner in the Figma review): the Aero background of the current frame (gradient and glass circles unchanged); the logo `Logo / Mark` alone, centered on both axes, 160 dp tall (the tight crop, so the tilted seed is about 92 dp wide); nothing else on the frame.

## Scope

1. **Discovery before any write.** Read the live `splash` frames (ids in `tools/export-figma.mjs`) and the `Branding` page. Record the current logo node and its box (120 dp, 24 dp above the wordmark per A14).
2. **Branding page.** Place the master SVG as a component `Logo / Mark` with the three fills bound to local brand variables (`brand/outline`, `brand/grain`, `brand/highlight`; same value in Light and Dark). Add the monochrome variant. No wordmark change.
3. **Splash.** In `splash · Light` and `splash · Dark`: remove the wordmark, the divider and the disclaimer text; place one `Logo / Mark` instance centered, 160 dp tall. Background untouched.
4. **Owner gate.** Screenshot both frames through the MCP, link the section, move this plan to `pending_manual_validation/` and record the call count. No export before the owner's OK.
5. **Export after OK.** `node tools/export-figma.mjs --only splash`, then `node tools/check-figma.mjs` and `node tools/check-docs.mjs`. Other golds keep their bytes.

## Client follow-up

[A51](../../android/plans/a51-logo-only-splash.md) changes the Compose splash to the new frame and re-records the baselines; it starts only after this plan is `Concluído`. The regenerated `logo_mark` raster is already in place. If the icon background `#0B0D10` is revisited (the dark outline disappears on it), that is a separate decision, not this plan.

## Intended documentation changes

At Completion: the design index links the delivered design and the `splash` golds are re-exported. The product rules about the splash copy (rules 7 and 9 of [perfil-onboarding](../../produto/specifications/perfil-onboarding.md)) are updated by A51, not here.

## Out of scope

Wordmark design, store listing art, any other screen, token edits outside the three brand variables, the icon background color, Material You tinting rules.

## Validation

1. Discovery note with the previous node and box.
2. Two frame screenshots (Light, Dark) with the seed alone, centered, 160 dp tall, and no text on the frame.
3. Explicit owner review in Figma.
4. Re-exported `splash` golds; check-figma and check-docs pass; no unrelated gold diff; splash Roborazzi gate within the approved threshold ([qa README](../../qa/README.md)).
5. SDD git delivery.

## Results

Planning only.
