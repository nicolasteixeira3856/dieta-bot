# Plan — D11 Landing page (fibrai.app)

- Status: Aguardando aprovação
- Date: 05/10/2026
- Owning context: `design`
- Executable boundary: Figma `Design`, a new page named "Landing page", plus new web components in `Componentes` under a `Web/` prefix. Repository outputs: landing golds in `docs/qa/figma/{dark,light}/`, their entries in `tools/export-figma.mjs` and the [gold inventory](../../qa/README.md#golds). No `site/`, `apps/` or `server/` code.
- Prerequisites: approval of this plan accepts [ADR-037](../../site/adrs/ADR-037-landing-site.md). App screenshots used in the mockup are the current golds; after [D10](d10-fibrai-tali-rename.md) exports, they are replaced by the renamed ones.
- Figma MCP budget: at most 120 calls a day ([ADR-031](../adrs/ADR-031-figma-source-of-truth.md) § 6). The brainstorm may span more than one day; each day's call count goes into Results.

Authorization and delivery follow [SDD](../../sdd/README.md). Approval: `Aprovo o plano docs/design/plans/d11-landing-page.md. Implemente o plano aprovado.`

## Objective

Design the Fibrai landing page with the owner in Figma: Aero look, the app's palette and typography, light by default with a dark/light switch, desktop and mobile. Then freeze it as golds for [W1](../../site/plans/w1-landing-site.md).

## Fixed requirements (ADR-037)

| Area | Requirement |
|---|---|
| Content | What the app does: fit the next meal into today's remaining budget, dinner first. Disclaimer "Estimativa, não orientação". App screens shown in phone frames. Tali shown with her avatar bubble (temporary image, [ADR-035](../../produto/adrs/ADR-035-tali-in-app-identity.md)). |
| Footer | "Política de privacidade" link to `/privacidade`. The page exists and is empty: title only, plus "Em breve" or no text, decided at review. |
| Contact | A "Contato" section with no address, form or link. |
| Theme | Light by default. A visible switch (sun/moon, Phosphor). Both themes from the `Light`/`Dark` variable modes. |
| Forbidden | Download buttons, store badges, invite or waitlist forms, prices, email capture, testimonials or numbers that do not exist. Claims of medical or nutritional advice. |
| Copy | pt-BR, numbers first, dry, no coach, no slogan in the AGENTS sense. Headline and section copy are proposed by the agent and decided by the owner. |

## Scope

1. **Setup:**
   - Create the page "Landing page".
   - Read the Aero variables, text styles and existing components; reuse them. Web-only components go under `Web/` in `Componentes`: nav bar, theme switch, section, phone frame, footer.
   - Width frames: desktop 1440 px and mobile 390 px.
2. **Brainstorm (owner in the loop):**
   - Draw 2–3 hero and structure variations as low-effort frames in an "Exploração" section.
   - Screenshot them for the owner and iterate on the owner's comments.
   - Record the chosen direction and rejected variations in Results.
3. **Final composition:**
   - Section "Landing", Light row first: `land` (desktop home), `landM` (mobile home), `priv` (desktop `/privacidade` placeholder).
   - Then the Dark row as mode clones.
   - Frame names follow `<id> · <title> · Light|Dark`.
   - Spacing and readability follow ADR-031 § 3. Glass is never behind body text without the opaque fallback tone.
4. **Owner gate:** one screenshot per final frame. The plan moves to `pending_manual_validation/` until the owner's OK in Figma. Exploration frames stay in the page as history or are deleted at the owner's choice.
5. **Export after OK:**
   - Add `land`, `landM` and `priv` to `tools/export-figma.mjs` and to the inventory, source `figma`, under a "site" line.
   - `node tools/export-figma.mjs --only land,landM,priv`, then `node tools/check-figma.mjs` and `node tools/check-docs.mjs`.
   - Also export the phone-frame app screenshots W1 needs as web assets. W1 copies them into `site/`.

## Out of scope

- HTML/CSS: [W1](../../site/plans/w1-landing-site.md).
- Privacy policy text ([CP8](../../content-policy/plans/out_of_scope/cp8-public-legal-pack.md)), contact data, logo, final avatar art.
- App frames.

## Validation

1. Results list the exploration rounds, the owner's choice and the calls per day.
2. A read-back shows variables only (no raw hex), no detached DS instances and no overlap.
3. Final screenshots and the owner's OK in Figma.
4. `check-figma` and `check-docs` pass; no app gold changes.

## Results

Planning only. No Figma calls yet.
