# Plan — D11 Landing page (fibrai.app)

- Status: Pendente aprovação manual
- Date: 05/10/2026
- Owning context: `design`
- Executable boundary: Figma `Design`, a new page named "Landing page", plus new web components in `Componentes` under a `Web/` prefix. Repository outputs: landing golds in `docs/qa/figma/{dark,light}/`, their entries in `tools/export-figma.mjs` and the [gold inventory](../../../qa/README.md#golds). No `site/`, `apps/` or `server/` code.
- Prerequisites: approval of this plan accepts [ADR-037](../../../site/adrs/ADR-037-landing-site.md). App screenshots used in the mockup are the current golds; after [D10](../d10-fibrai-tali-rename.md) exports, they are replaced by the renamed ones.
- Figma MCP budget: at most 120 calls a day ([ADR-031](../../adrs/ADR-031-figma-source-of-truth.md) § 6). The brainstorm may span more than one day; each day's call count goes into Results.

Authorization and delivery follow [SDD](../../../sdd/README.md). Approval: `Aprovo o plano docs/design/plans/d11-landing-page.md. Implemente o plano aprovado.`

## Objective

Design the Fibrai landing page with the owner in Figma: Aero look, the app's palette and typography, light by default with a dark/light switch, desktop and mobile. Then freeze it as golds for [W1](../../../site/plans/w1-landing-site.md).

## Fixed requirements (ADR-037)

| Area | Requirement |
|---|---|
| Content | What the app does: fit the next meal into today's remaining budget, dinner first. Disclaimer "Estimativa, não orientação". App screens shown in phone frames. Tali shown with her avatar bubble (temporary image, [ADR-035](../../../produto/adrs/ADR-035-tali-in-app-identity.md)). |
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

- HTML/CSS: [W1](../../../site/plans/w1-landing-site.md).
- Privacy policy text ([CP8](../../../content-policy/plans/out_of_scope/cp8-public-legal-pack.md)), contact data, logo, final avatar art.
- App frames.

## Validation

1. Results list the exploration rounds, the owner's choice and the calls per day.
2. A read-back shows variables only (no raw hex), no detached DS instances and no overlap.
3. Final screenshots and the owner's OK in Figma.
4. `check-figma` and `check-docs` pass; no app gold changes.

## Results

Approved by the owner on 2026-10-05 ("Aprovo o plano docs/design/plans/d11-landing-page.md. Implemente o plano aprovado."). The approval accepts [ADR-037](../../../site/adrs/ADR-037-landing-site.md).

### Discovery (before the first write)

Sources read: ADR-034, ADR-035, ADR-037, [W1](../../../site/plans/w1-landing-site.md), the Home disclaimer in code (`Estimativa nutricional, não substitui consulta médica ou nutricional.`), the AGENTS formulas and product rules, and the Figma file (variables, text, effect and paint styles, every component, the `Release 1` sections). Classes: `app` (a feature that exists in code or a live specification, so the page may state it), `copy` (text proposed for the page, decided by the owner), `out` (forbidden by ADR-037 or not in the product).

| Element | Class | Figma |
|---|---|---|
| Job: the next meal fits today's remaining budget, dinner first | app | hero headline and lead (copy proposals below) |
| Daily ceiling in kcal (O1), workout credit 0 % / part / 100 % (O2) | app | facts chips (`Chip/Log` Neutral) and "Como funciona" step 1 |
| Meal by text or photo in the Chat; questions before the estimate, all at once | app | "Como funciona" step 2, Tali block |
| Home: eaten, remaining, macros, consolidated meal log | app | phone frame with the `home1` gold |
| Tali as the assistant name, avatar bubble with a temporary "T" monogram | app (ADR-034/035) | new `Web/Avatar` |
| Disclaimer "Estimativa, não orientação." | app (ADR-037) | hero caption and `Web/Footer` |
| Footer link "Política de privacidade" → `/privacidade` | app (ADR-037) | `Web/Footer` |
| "Contato" with no content | app (ADR-037 § 7) | section title only |
| Dark/light switch, light by default | app (ADR-037 § 5) | new `Web/ThemeSwitch` + `Icon/sun (regular)` |
| Download buttons, store badges, invite, waitlist, prices, e-mail capture, testimonials, user counts, medical or nutritional advice | out | not drawn |
| Logo | out (ADR-034 open item) | text wordmark "Fibrai" in `Title` |

The phone frames hold clones of the `Release 1` gold frames, scaled to 300 px, so the Dark row follows the Color mode with no extra work. Until [D10](../d10-fibrai-tali-rename.md) runs, those clones still show "Chat Dieta Bot" and "Dieta Bot AI"; W1 takes the screens after D10.

### Delivered in Figma (2026-10-05)

**`Componentes`:**

- `Icon/sun (regular)` (`101:173`) in the `Ícones` grid, from the official Phosphor SVG (`@phosphor-icons/core` 2.1.1), fill `icon/primary`.
- New section `Web · D11` (`101:179`, 160 px below `Config e push`), each component with a description:
  - `Web/ThemeSwitch` (`101:196`), Theme = Light | Dark: a glass pill with sun and moon; the current theme's segment is `accent/default`.
  - `Web/Avatar` (`101:235`): 48 px "T" monogram on the accent gradient, glass edge.
  - `Web/PhoneFrame` (`101:232`): a transparent bezel (`surface/2` 8 px, `border/glass` 1 px) drawn over a clipped screen.
  - `Web/NavBar` (`101:220`), Size = Desktop | Mobile: wordmark, anchor links (desktop) and the switch.
  - `Web/Footer` (`101:231`), Size = Desktop | Mobile: wordmark, disclaimer, privacy link, `border/line` top rule.

**Page `Landing page`** (`102:2`, after `Release 1`): page header (`102:3`) and section `Exploração · D11` (`102:6`).

### Exploration round 1 (2026-10-05)

| Variation | Frame | Hero | Structure |
|---|---|---|---|
| A · Saldo primeiro, uma tela | `102:9` | "Quanto ainda cabe no jantar." left, one phone (`home1`) right | glass bands: Como funciona, Tali, aviso, Contato |
| B · Centralizado, três telas | `104:111` | "A próxima refeição, dentro do saldo do dia." centered, three phones (`o1`, `home1`, `chatE`) | three step cards, Tali band, Contato |
| C · Chat primeiro, Tali em destaque | `104:275` | "Descreva o prato. A Tali estima e desconta do saldo." left, two phones (`chatE`, `home1`) | Tali card with avatar, zigzag steps with a phone each, Contato |

Exploration titles used raw sizes (56 / 36 / 20 px), because the Aero text styles stop at 34 px.

**Owner's choice (2026-10-05):** variation B. A and C stay in `Exploração · D11` as history unless the owner asks to delete them. The owner left the copy to the agent ("um texto explicando o próprio aplicativo"), approved web text styles (option a) and the `/privacidade` placeholder with its title plus "Em breve.".

### Web text styles (owner decision, 2026-10-05)

- New Figma text styles, each with a description and a specimen row in `Cores e tipografia` → `Tipografia` (that section grew by 274 px and the sections below moved down by the same amount):
  - `Web/Display`: Bold 56/64;
  - `Web/Heading`: Bold 36/44;
  - `Web/Subheading`: Bold 28/36;
  - `Web/Lead`: Regular 20/30.
- `docs/design/tokens.json` gained the four styles (read on 2026-10-05) and `node tools/gen-tokens.mjs` re-rendered `docs/tokens.md`. This goes beyond the repository outputs listed in this plan, by owner decision.
- Side effect: the Android build generates `AeroTextTokens` from the same JSON, so it will also emit four text tokens the app does not use. No app source changes.

### Final composition (2026-10-05)

Section `Landing · D11` (`106:527`), 160 px below `Exploração · D11`. The frames use auto layout, page width with content at 1200 px on desktop and 20 px side margins on mobile.

| Gold | Light | Dark | Size |
|---|---|---|---|
| `land` · Home | `106:530` | `107:853` | 1440 × 2568 |
| `landM` · Home mobile | `106:716` | `107:1003` | 390 × 2940 |
| `priv` · Política de privacidade | `106:833` | `107:1086` | 1440 × 699 |

- **Structure (`land`):**
  - `Web/NavBar`;
  - hero: eyebrow, H1 in `Web/Display`, lead in `Web/Lead`, three `Chip/Log` facts and the disclaimer;
  - three phones: `o1` and `chatE` 48 px lower, `home1` in the middle;
  - "Como funciona" with three glass step cards;
  - "Tali": a glass card with `Web/Avatar`;
  - `Card/Note` with the estimate disclaimer;
  - "Contato" as a title only;
  - `Web/Footer`.
- **`landM`:** the same order with `Web/Heading` for the H1, `Body` for the lead, wrapping chips, one phone (`home1`), stacked steps and `Web/Subheading` titles.
- **`priv`:** nav, H1 `Política de privacidade`, `Em breve.` in `Web/Lead`, footer.
- **Dark frames:** clones with the Color mode set to Dark. The phone screens are gold clones with no explicit mode, so they follow the frame. One deliberate exception: the nested `Web/ThemeSwitch` is set to Theme=Dark, so the dark page shows the moon as current.
- **Decorative bubbles:** clones of the app's edge bubble, absolute, placed only in empty margins.
- **Read-back:**
  - outside the gold clones, no unbound solid paint and no text without a style;
  - every DS element is an instance;
  - the frames do not overlap.

**Copy (agent-written, open to owner edits):**

- Eyebrow: "DIÁRIO DE CALORIAS COM IA".
- H1: "A próxima refeição, dentro do saldo do dia."
- Lead: "Você define um teto de calorias para o dia. Cada refeição, descrita em texto ou foto, recebe uma estimativa e sai do saldo. O Fibrai mostra quanto ainda cabe na próxima refeição sem tirar o espaço do jantar."
- Facts: "Teto do dia em kcal", "Treino pode entrar no saldo", "Texto ou foto".
- Steps:
  - Passo 1 "Teto do dia": "Um número em kcal, igual todos os dias ou diferente por dia da semana. Você escolhe quanto do treino volta para o saldo: nada, uma parte ou tudo. Sem treino informado, nada volta."
  - Passo 2 "Refeição no Chat": "Escreva o que comeu ou mande a foto do prato. Se faltar alguma informação, a Tali faz todas as perguntas de uma vez antes de estimar."
  - Passo 3 "Saldo na Home": "A linha do tempo do dia mostra cada refeição em kcal, proteína, carboidrato e gordura, e quanto ainda cabe na próxima."
- Tali: "Tali é a assistente do Fibrai no Chat. Lê o texto ou a foto da refeição, pergunta o que falta e responde com números: calorias, proteína, carboidrato e gordura. Não monta dieta nem dá orientação de saúde."
- Note: "Os valores são estimativas feitas por IA a partir do que você escreve ou fotografa. O Fibrai não substitui nutricionista nem médico."

### Owner gate

One review image per final frame was sent to the owner on 2026-10-05 (REST dry-run export). The plan waits in `pending_manual_validation/` for the owner's OK in Figma.

**Still open after the OK (step 5):**

- Map `land`, `landM` and `priv` in `tools/export-figma.mjs` and in the inventory under a `figma` "site" line.
- Teach `tools/check-figma.mjs` the site widths (2880 px desktop, 780 px mobile at 2×). Today it accepts only 780 px.
- Export, then run `check-figma` and `check-docs`.
- Export the phone screens as web assets for W1.

### Owner fix 1: WCAG contrast (2026-10-05)

The owner found the text hard to read in Light. The ratios were measured against the real background: the `Background/Page` gradient at each text's position, or the glass composite of `surface/glass` + sheen over it. Targets are WCAG 2.2 AA, 4.5:1 for body text and 3:1 for large text and UI icons.

**What failed (Light only):** text in `text/muted`, `text/dim` or `accent/default` placed straight on the saturated top of the gradient (`bg/top` → `bg/mid`):

| Text | Before | After (`text/primary`) |
|---|---|---|
| Nav links (`Web/NavBar` Desktop) | 2.66 | ≥ 6.46 |
| Eyebrow | 2.67 | ≥ 6.46 |
| Lead (`land`, `landM`) | 2.94 / 2.85 | ≥ 7.01 |
| Disclaimer caption | 2.89 | ≥ 6.46 |
| `priv` "Em breve." | 3.58 | 8.81 |

- **Fix:** those 17 text nodes (nav component, hero texts and `priv`, both rows) are bound to `text/primary`. No variable changed, so the app is untouched.
- **Already passing (Light):**
  - text on glass cards: `text/muted` ≥ 4.99;
  - chips: `text/muted` on `surface/2` 4.87;
  - footer: `text/dim` / `accent/default` ≥ 4.62 / 4.74;
  - the switch icons on the glass pill: > 3:1.
- **Dark:** every text was already ≥ 5.34.
- **Not changed:** the app screens inside the phone frames, which are gold clones of the app.
- **Exploration:** frames A, B and C keep the old colors as history.

### Figma MCP budget

Day 1 (2026-10-05): 22 calls of 120 (the WCAG fix added one write):

- 1 `whoami` and 3 skill reads;
- 18 `use_figma`: 7 read-only inspections and 11 writes. Of the inspections, one was a syntax error. Of the writes, one hit an instance lookup error and one matched nothing.

Review images come from the REST export (`tools/export-figma.mjs --dry-run`) and cost no MCP call.
