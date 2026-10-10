# Plan — D29 Landing page: phone screens after D27 and D28

- Status: Concluído
- Date: 09/10/2026
- Owning context: `design`
- Affected code: none in `apps/`, `server/` or `web/`. Figma `Design` → page `Landing page` → section `Landing · D11`: the phone-screen clones of `land` (Light `106:530`, Dark `107:853`) and `landM` (Light `106:716`, Dark `107:1003`). Repository: `docs/qa/figma/{dark,light}/{land,landM}.png`. Node ids in `tools/export-figma.mjs` only if a frame id changes.
- Prerequisites: [D27](../completed/d27-conversational-onboarding.md) and [D28](../completed/d28-home-extras-and-history.md) `Concluído` (met).
- Figma MCP budget: ≤ 15 calls (at most 120 a day, ADR-031 § 6).

Authorization, lifecycle, cancellation and deferral: `docs/sdd/README.md`. Approval: `Aprovo o plano docs/design/plans/d29-landing-phone-screens.md. Implemente o plano aprovado.`

## Objective

The phones of the landing golds hold static clones of app golds ([D11](../completed/d11-landing-page.md), refreshed by [D10](../completed/d10-fibrai-tali-rename.md) scope 7). Two of them are now out of date: the left phone of `land` shows the retired `o1`, and the middle phone of `land` and the only phone of `landM` show the `home1` from before D28. Replace those clones with the current golds so that [W3](../../../site/plans/completed/w3-landing-screens-after-onboarding.md) can cut the same screens for `web/` and compare against `land` and `landM`.

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
| `land` left phone (was `o1`) | `ob3` Light frame `209:1164` (`Release 2`, Onboarding v2) | [ADR-057](../../../produto/adrs/ADR-057-conversational-onboarding.md) decision 4 |
| `land` middle phone, `landM` phone (`home1`) | `home1` Light frame `214:1384` (`Release 2`, D28) | [ADR-058](../../../produto/adrs/ADR-058-extras-and-history.md) |
| `land` right phone (`chatE`) | unchanged | — |

## Scope

1. **Discovery (read only):** read the four landing frames and list each phone clone (node, size, position, Color mode). Copy check of the landing text against the new screens; result recorded in Results. Expected: no text change (see the copy table in [W3](../../../site/plans/completed/w3-landing-screens-after-onboarding.md#copy-and-layout-check)). A text change found here stops the plan and goes back to Planning as a D11 copy follow-up.
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
- `web/`: [W3](../../../site/plans/completed/w3-landing-screens-after-onboarding.md) cuts the screens and compares.
- App golds: `ob3` and `home1` are read, not changed.

## Validation

1. A read-back of the four frames: every clone is a clone of the named Light frame, no explicit mode, no overlap, no detached DS instance outside the clones.
2. Owner OK in Figma on `land` and `landM`, both themes.
3. `node tools/check-figma.mjs` passes after the export; only `land` and `landM` PNGs have a byte diff.
4. Figma MCP calls counted in Results (cap 15).
5. `node tools/check-docs.mjs` passes.

## Results

Approved by the owner on 09/10/2026 ("Aprovo o plano docs/design/plans/d29-landing-phone-screens.md. Implemente o plano aprovado."). Prerequisites checked: D27 and D28 in `completed/`. Seat: Figma Student, Full.

### Discovery (read only, before the write)

Each phone is `Phone · <id>` (316 × 665, clipped) holding a `Screen` frame (300 × 649 at 8, 8, clipped) with one gold clone at 0, 0, under a `Web/PhoneFrame` instance. No clone carries an explicit Color mode; the landing frames do (`land`/`landM` Light `2:0`, Dark `2:1`).

| Frame | Phone | Screen | Old clone | Action |
|---|---|---|---|---|
| `land` Light `106:530` | `Phone · o1` `106:558` (slot `106:557`, 48 px lower) | `106:559` | `o1 (clone do gold)` `114:4954`, 300 × 760 | replace by `ob3` |
| `land` Dark `107:853` | `Phone · o1` `107:871` (slot `107:870`) | `107:872` | `114:5050` | replace by `ob3` |
| `land` Light | `Phone · home1` `106:596` | `106:597` | `home1 (clone do gold)` `114:4986`, 300 × 1108 | replace by D28 `home1` |
| `land` Dark | `Phone · home1` `107:907` | `107:908` | `114:5082` | replace by D28 `home1` |
| `landM` Light `106:716` | `Phone · home1` `106:745` | `106:746` | `114:5146` | replace by D28 `home1` |
| `landM` Dark `107:1003` | `Phone · home1` `107:1016` | `107:1017` | `114:5188` | replace by D28 `home1` |
| `land` both | `Phone · chatE` `106:644`, `107:953` | — | — | unchanged |

Copy check: the landing text was compared with `ob3` and the D28 `home1` (table in [W3](../../../site/plans/completed/w3-landing-screens-after-onboarding.md#copy-and-layout-check)). No text change; the plan continued.

### Write (one `use_figma` call)

- New clones of the Light frames `209:1164` (`ob3`) and `214:1384` (`home1`), each `rescale(300/390)`, Color mode cleared, at 0, 0 of the same `Screen`, named `<id> (clone do gold)`: `ob3` `219:1171` (Light), `219:1192` (Dark); `home1` `219:1213`, `219:1260` (`land`), `219:1307`, `219:1354` (`landM`). The six old clones were removed.
- Renamed: `Phone · ob3` (`106:558`, `107:871`) and `Slot · ob3` (`106:557`, `107:870`).
- No frame id changed: `tools/export-figma.mjs` keeps `land` `106:530`/`107:853` and `landM` `106:716`/`107:1003`.

### Read-back (REST dry run, no MCP call)

`node tools/export-figma.mjs --only land,landM --dry-run`, compared with the current golds:

- Pixels changed outside the phone boxes: 0 in all four PNGs. Frame sizes unchanged (`land` 2880 × 5136, `landM` 780 × 5880).
- Dark rows follow the mode (dark surfaces, light text). No Light placeholder paint showed up in a Dark clone.
- Each phone screen at 2x is 600 × 1298, the size of the `web/` cut. Blurred diff against a fresh cut of the gold (share of pixels > 40): `ob3` 12.0 % Light, 4.9 % Dark; `home1` 7.0 % / 4.2 % (`land` and `landM` alike); the untouched `chatE` phone 8.0 % / 5.1 %. The new clones sit within the drift W1 already accepted for `chatE` (rounded screen corners, decorative bubbles over the phone edge, the rescaled clone). The page gate is W3's capture against these golds.

### Review

Review PNGs (phone crops of `land` and `landM`, both themes, from the dry run) sent to the owner on 09/10/2026. Owner OK on 09/10/2026, first round, no fixes (given in chat with the approval of W3: "Yes, OK D29").

### Export

- `node tools/export-figma.mjs --only land,landM`: `land` 3.839 % px changed Light, 1.156 % Dark; `landM` 5.951 % Light, 1.911 % Dark; all kept by the noise filter. Only these four PNGs have a byte diff.
- `node tools/check-figma.mjs`: all 114 gold PNGs verified (57 dark + 57 light). Inventory unchanged.
- Next: [W3](../../../site/plans/completed/w3-landing-screens-after-onboarding.md) cuts the same screens for `web/` and compares against these golds.

### Figma MCP calls

5 of 15: `whoami`, 2 skill reads (`figma-use`, `figma-generate-design`), 1 read-only `use_figma`, 1 write `use_figma`. No screenshot call (review images from the REST dry run).
