# Plan — W3 Landing phone screens after the conversational onboarding

- Status: Aguardando aprovação
- Date: 09/10/2026
- Owning context: `site`
- Executable boundary:
  - `web/tools/build-screens.mjs` (the `SCREENS` list and its header comment);
  - `web/public/index.html` (the left phone: image paths and alt text);
  - `web/public/img/screens/{light,dark}/` (generated: `ob3.webp` added, `home1.webp` re-cut, `o1.webp` removed);
  - captures in `docs/qa/site/current/{light,dark}/{land,landM,priv}.png`;
  - at Completion, the [site README](../README.md) line on the generated screens and the [design plans index](../../design/plans/README.md).
  - No change to `web/public/css/`, the page copy, `apps/`, `server/`, `docs/qa/figma/` or hosting ([W2](w2-landing-hosting.md)).
- Related documentation: [site README](../README.md), [ADR-037](../adrs/ADR-037-landing-site.md), [W1](completed/w1-landing-site.md) (§ 6, the screen cut), [D11](../../design/plans/completed/d11-landing-page.md), [ADR-057](../../produto/adrs/ADR-057-conversational-onboarding.md), [ADR-058](../../produto/adrs/ADR-058-extras-and-history.md), the [run report of D27 and D28](../../design/validation/2026-10-09-d27-d28.md) (follow-up 2), the [gold inventory](../../qa/README.md#golds), [production gate](../../content-policy/production-gate.md).
- Prerequisites:
  - [D27](../../design/plans/completed/d27-conversational-onboarding.md) and [D28](../../design/plans/completed/d28-home-extras-and-history.md) `Concluído` (met);
  - [D29](../../design/plans/d29-landing-phone-screens.md) `Concluído`: the golds `land` and `landM` show `ob3` and the D28 `home1` in their phones.

Authorization and delivery follow [SDD](../../sdd/README.md). Approval: `Aprovo o plano docs/site/plans/w3-landing-screens-after-onboarding.md. Implemente o plano aprovado.`

## Problem

`web/tools/build-screens.mjs` cuts the landing phones from the app golds `o1`, `home1` and `chatE`. On 09/10/2026:

| Screen | Gold | Cut WebP in `web/public/img/screens/` | `screens:check` |
|---|---|---|---|
| `o1` | retired by D27 (PNGs in `docs/qa/_legacy/figma-d4-onboarding/`) | still present, shows the old onboarding | crashes: `docs/qa/figma/light/o1.png` is missing |
| `home1` | redrawn by D28 (day strip, an extra; 780 × 3280) | stale, both themes | stale |
| `chatE` | unchanged | byte-identical to a fresh cut, both themes | fresh |

So `npm --prefix web run check` fails, and the live page would show an onboarding the app no longer has.

## Decision: `ob3` replaces `o1`

The left phone shows `ob3`, the summary of the profile. The comparison of the candidates (`ob0`, `ob2`, `ob3`, the transitional states) is owned by [D29](../../design/plans/d29-landing-phone-screens.md#decision-which-onboarding-screen-replaces-o1); in short, `ob3` is the only onboarding screen whose top 844 pt carry the ceiling, the macros, the meals and the eat-back the landing text talks about, it does not repeat the Chat look of `chatE`, and it has no CTA that could read as a way to get the app. `home1` and `chatE` stay in their places.

## Copy and layout check

Checked against the landing text (`web/public/index.html`, verbatim from the D11 golds) and the new screens. Result: **no copy or layout change, no D11 follow-up**. The only Figma change the new screens need is the clone refresh of D29.

| Landing element | Text | Still true after ADR-057 and ADR-058? |
|---|---|---|
| Hero lead | `Você define um teto de calorias para o dia…` | Yes. The app proposes the ceiling from the TMB and the user edits or sends it (ADR-057 decision 3); `ob3` shows it with a pencil. |
| Chip | `Teto do dia em kcal` | Yes (`ob3`, Teto e macros). |
| Chip | `Treino pode entrar no saldo` | Yes (`ob3`, Treino; the eat-back is still asked). |
| Chip | `Texto ou foto` | Yes for the Chat; the onboarding is text only, and the chip is about meals. |
| Passo 1 | `Um número em kcal, igual todos os dias ou diferente por dia da semana. Você escolhe quanto do treino volta…` | Yes. The ceiling mode is derived as `same` and stays editable per weekday in Config (ADR-057 decision 3). |
| Passo 2, Passo 3 | Chat questions before the estimate; the day timeline in kcal and macros | Yes. The D28 strip and extras add to the Home; nothing in the text is contradicted. |
| Tali band | `…Não monta dieta nem dá orientação de saúde.` | Unchanged by D27/D28. Tali now also builds the profile, which the text does not mention; adding it is a copy choice, not a correction, and is out of scope. |
| Layout | three phones (`land`), one phone `home1` (`landM`), 300 px screens, 844 pt cut | Unchanged. `ob3` and `home1` are 780 px wide and taller than 1688 px, so the cut is the same top 1688 px. |

## Scope

1. **Cut list (`build-screens.mjs`):** `SCREENS = ["ob3", "home1", "chatE"]`; the header comment names the three ids. Nothing else in the tool changes (width check, 1688 px cut, 600 px WebP, 300 KB budget).
2. **Re-cut:** `npm --prefix web run screens` writes the six WebP files. A fresh cut measured on 09/10/2026: `ob3` 39 KB light, 38 KB dark; `home1` 37 KB light, 34 KB dark; all under the budget.
3. **Remove the retired cut:** delete `web/public/img/screens/{light,dark}/o1.webp`. The tool does not delete files it no longer writes.
4. **Page (`index.html`, left phone only):**
   - `src` `/img/screens/{light,dark}/ob3.webp`, same `width="600" height="1298"` and `loading="lazy"`;
   - alt text, both images: `Resumo do perfil no Fibrai: teto de calorias, macros, refeições e treino` (pt-BR, describes the visible screen, no visible copy changes);
   - the `home1` and `chatE` alt texts stay; `home1` still matches the redrawn screen.
5. **Production guard (ADR-037 § 6), unchanged:** no analytics, cookies, forms, sign-up, invite, download link, store badge or APK; no third-party request; no inline script or style. The new screen has no CTA visible in the cut.
6. **Documentation at Completion:**
   - [site README](../README.md): the generated `img/screens/` line names `ob3`, `home1` and `chatE`;
   - [design plans index](../../design/plans/README.md): the landing row of this follow-up points to the history folder.

## Out of scope

- Landing copy or layout, including a sentence about the guided onboarding (a D11 copy follow-up in Figma first).
- The Figma clones and the `land`/`landM` export: [D29](../../design/plans/d29-landing-phone-screens.md).
- Hosting and deploy ([W2](w2-landing-hosting.md)).
- Any way to get the app, analytics or data collection: a [production gate](../../content-policy/production-gate.md) trigger.

## Validation

Cost: no model call, no emulator, no app build (Delivery pace).

1. **Generated files:** `npm --prefix web run check` passes: `tokens:check`, `screens:check` (six images fresh against `ob3`, `home1`, `chatE`, each under 300 KB), `html-validate` on both pages, and the forbidden-content grep.
2. **No dangling reference:** `rg -nw o1 web/public web/tools` finds no reference to the retired screen.
3. **Captures:** with `npm --prefix web run preview` running, `npm --prefix web run capture` writes `land`, `landM` and `priv` in light and dark to `docs/qa/site/current/{light,dark}/` and passes the [docs/qa gate](../../qa/README.md#gate) against the D29 golds (blurred diff ≤ 2 %, content ink 0.8×–1.25×; the up to 4 px drift of the rescaled Figma clone is accepted). Its responsive and theme checks pass. `priv` is a regression check (no phone).
4. **Written diff list** in Results for the changed phones (left and middle of `land`, the `landM` phone), both themes.
5. `node tools/check-docs.mjs` passes after the documentation updates.

## Results

Planning only.
