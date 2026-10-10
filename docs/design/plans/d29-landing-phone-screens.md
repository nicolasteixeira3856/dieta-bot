# Plan — D29 Landing page: phone screens after D27 and D28

- Status: Aguardando aprovação
- Date: 09/10/2026
- Owning context: `design`
- Affected code: none in `apps/`, `server/` or `web/`. Figma `Design` → page `Landing page` → section `Landing · D11`: the phone-screen clones of `land` (Light `106:530`, Dark `107:853`) and `landM` (Light `106:716`, Dark `107:1003`). Repository: `docs/qa/figma/{dark,light}/{land,landM}.png`. Node ids in `tools/export-figma.mjs` only if a frame id changes.
- Prerequisites: [D27](completed/d27-conversational-onboarding.md) and [D28](completed/d28-home-extras-and-history.md) `Concluído` (met).
- Figma MCP budget: ≤ 15 calls (at most 120 a day, ADR-031 § 6).

Authorization, lifecycle, cancellation and deferral: `docs/sdd/README.md`. Approval: `Aprovo o plano docs/design/plans/d29-landing-phone-screens.md. Implemente o plano aprovado.`

## Objective

The phones of the landing golds hold static clones of app golds ([D11](completed/d11-landing-page.md), refreshed by [D10](completed/d10-fibrai-tali-rename.md) scope 7). Two of them are now out of date: the left phone of `land` shows the retired `o1`, and the middle phone of `land` and the only phone of `landM` show the `home1` from before D28. Replace those clones with the current golds so that [W3](../../site/plans/w3-landing-screens-after-onboarding.md) can cut the same screens for `web/` and compare against `land` and `landM`.

## Decision: which onboarding screen replaces `o1`

`ob3` (summary of the profile). Reasons, against the landing as it stands:

| Candidate | What the 844 pt the phone shows contain | Fit with the landing |
|---|---|---|
| `ob3` (chosen) | `Resumo do seu perfil`, the blocks Corpo, Teto e macros, Refeições, Treino, Tom, each with its pencil | Numbers first. It shows exactly the hero chips (`Teto do dia em kcal`, `Treino pode entrar no saldo`) and Passo 1 (`Teto do dia`, eat-back), with the ceiling editable as the lead says (`Você define um teto…`). Its card layout differs from `chatE` next to it. |
| `ob2` | Tali proposing TMB, IMC and the ceiling, the prefilled composer | Shows the proposal well, but it is a second Chat screen beside `chatE`; the three phones would read as two chats and a Home. |
| `ob0` | Logo, `Bem-vindo ao Fibrai`, CTA `Vamos começar` | No number. A CTA inside a marketing phone also reads like a way to get the app (production gate, ADR-037 § 6). |
| `ob1`, `ob1e`, `ob4`–`ob6` | a single closed question, a retry, loading, success, error | Transitional states; none says what the app does. |

The `chatE` phone stays: its gold did not change (its cut WebP in `web/` is still byte-identical).

## Sources (feature parity, ADR-031 § 4)

| Phone | New clone source | Behavior source |
|---|---|---|
| `land` left phone (was `o1`) | `ob3` Light frame `209:1164` (`Release 2`, Onboarding v2) | [ADR-057](../../produto/adrs/ADR-057-conversational-onboarding.md) decision 4 |
| `land` middle phone, `landM` phone (`home1`) | `home1` Light frame `214:1384` (`Release 2`, D28) | [ADR-058](../../produto/adrs/ADR-058-extras-and-history.md) |
| `land` right phone (`chatE`) | unchanged | — |

## Scope

1. **Discovery (read only):** read the four landing frames and list each phone clone (node, size, position, Color mode). Copy check of the landing text against the new screens; result recorded in Results. Expected: no text change (see the copy table in [W3](../../site/plans/w3-landing-screens-after-onboarding.md#copy-and-layout-check)). A text change found here stops the plan and goes back to Planning as a D11 copy follow-up.
2. **Clones** (as in D10 scope 7):
   - replace the left-phone clone of `land` (both rows) by a fresh clone of the `ob3` Light frame, renamed `Phone · ob3`;
   - replace the `home1` clones of `land` and `landM` (both rows) by fresh clones of the `home1` Light frame;
   - each clone scaled to 300 px wide, at the old position, clipped by `Web/PhoneFrame`, with no explicit Color mode so the Dark row follows its frame;
   - check the Dark clones for bound paints that do not follow the mode (known issue of clones on this file); fix by removing the explicit mode, never by repainting.
3. **No other change:** frame size, copy, chips, steps, Tali band, footer and the `chatE` phone stay as they are.
4. **Owner review** (Figma review gate): one screenshot per changed frame (`land` and `landM`, Light and Dark) from the REST dry run (`node tools/export-figma.mjs --only land,landM --dry-run`, no MCP call); the plan goes to `pending_manual_validation/` until the owner's OK.
5. **Export** after the OK: `node tools/export-figma.mjs --only land,landM`, then `node tools/check-figma.mjs`. The inventory of `docs/qa/README.md` does not change (same ids).

## Out of scope

- Landing copy, layout, phone order or count (D11). A copy change is its own plan.
- `priv` (no phone).
- `web/`: [W3](../../site/plans/w3-landing-screens-after-onboarding.md) cuts the screens and compares.
- App golds: `ob3` and `home1` are read, not changed.

## Validation

1. A read-back of the four frames: every clone is a clone of the named Light frame, no explicit mode, no overlap, no detached DS instance outside the clones.
2. Owner OK in Figma on `land` and `landM`, both themes.
3. `node tools/check-figma.mjs` passes after the export; only `land` and `landM` PNGs have a byte diff.
4. Figma MCP calls counted in Results (cap 15).
5. `node tools/check-docs.mjs` passes.

## Results

Planning only.
