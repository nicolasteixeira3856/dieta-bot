# Plan — D19 Skips next to other actions: two receipts and the delete proposal

- Status: Concluído (07/10/2026, owner OK, golds exported)
- Date: 07/10/2026
- Owning context: `design`
- Affected code: none in `apps/` or `server/`. Figma `Design` → `Release 1` → section "Chat · D6" (records and memory); components in `Componentes` only if a variant is missing. Repository: `docs/qa/figma/{dark,light}/{chatSK,chatSD}.png` and their node ids in `tools/export-figma.mjs`.
- Prerequisites: D6 and D9 `Concluído` ([history](./)); independent of S29 (the copy is fixed by ADR-047).
- Figma MCP budget: ≤ 30 calls (at most 120 a day, ADR-031 § 6).

Approving this plan accepts [ADR-047](../../../produto/adrs/ADR-047-skips-alongside-other-actions.md). Authorization, lifecycle, cancellation and deferral: `docs/sdd/README.md`. Approval: `Aprovo o plano docs/design/plans/d19-skips-with-other-actions.md. Implemente o plano aprovado.`

## Objective

Draw the two Chat states ADR-047 adds, in Aero, both themes: an answer that recorded a meal and skipped another one (two receipts), and the proposal to delete a record when the user skips a meal that has one.

## Sources (feature parity, ADR-031 § 4)

| Gold | Reference (current gold or frame, layout only) | Behavior source |
|---|---|---|
| `chatSK` | `chatG` (answer with the receipt and its action stack) | ADR-047 decision 2: the log receipt with its actions, then `Pulado {slot}` with **Desfazer** only |
| `chatSD` | `chatU` (the in-conversation confirmation card below an answer) | ADR-047 decision 3: `Pular {slot}?`, `{slot} tem {kcal} kcal registrados. O registro sai e o {slot} fica pulado.`, **Excluir e pular** (danger) · **Manter registro** (outline) |

Code: `apps/android/app/src/main/java/app/fibrai/android/feature/chat`.

## Scope

1. **Discovery (read only):** per gold, every visible element classified `app`, `gold-only` or `copy`; the table goes into Results before any write. Sample texts are synthetic (no tester text): `chatSK` uses a message like `Pulei o pré-treino. No café comi 2 ovos mexidos e 1 pão francês.`, with a profile that has a `Pré-treino` slot.
2. **Components:** reuse the receipt row, `Chat/ReceiptAction` and the `chatU` card. The danger primary takes the `status/bad` treatment already used by Excluir and `Dialog/Confirm` Tone=Danger; add a card variant only if the `chatU` card has no danger tone. The `Registro mantido` mark is the existing receipt mark style (icon + `muted` label), not drawn as its own gold.
3. **Screens** (section "Chat · D6", after `chatRL`): Light row `chatSK`, `chatSD`, 390 px; Dark row as clones with the `Dark` mode; frame names `<id> · <title> · Light|Dark`; spacing per ADR-031 § 3. Long label check: a 40-character slot name in `Pular {slot}?` and two-line wrapping of **Excluir e pular** at large text.
4. **Owner review** (Figma review gate): one screenshot per frame; `pending_manual_validation/` until the owner's OK; fixes with one screenshot per changed frame.
5. **Export** after the OK: ids in `tools/export-figma.mjs`; `node tools/export-figma.mjs --only chatSK,chatSD`; both ids added to the inventory of `docs/qa/README.md`; `node tools/check-figma.mjs`. Existing golds keep their bytes.

## Out of scope

- Compose (A59). Server (S29). Home. Other Chat states; `chatG` and `chatU` themselves.

## Validation

1. The discovery table exists in Results before the first write.
2. Read-back: instances for every DS component, no raw hex outside the variables, no overlapping nodes.
3. Visual: one screenshot per frame; manual owner OK.
4. `node tools/check-figma.mjs` and `node tools/check-docs.mjs` pass.

## Results

### Discovery (before the first write)

Figma budget: whoami, 2 skill reads and 2 read-only `use_figma` inspections before this table (5 of 30).

`chatSK` (reference `chatG` 72:2635, light):

| Element | Class | Source |
|---|---|---|
| `Chat/Header`, `Chip/Date`, `Chat/Composer`, background bubbles | app | `ChatScreen.kt` (as in `chatG`) |
| User bubble `Pulei o pré-treino. No café comi 2 ovos mexidos e 1 pão francês.` | copy | synthetic (plan § Scope 1) |
| Bot bubble: one clause naming the skip, then the estimate (`Chat/Estimate` Kind=Meal) and the time | app + copy | ADR-047 decision 4; S29 rule 3f, reply shape of the dev smoke (`Pré-treino de hoje fora.`) |
| Log receipt `Registrado em Café da manhã · 07:30`, chip `+{kcal} kcal`, Excluir · Trocar refeição · Editar | app | `ChatRecordComponents.kt` `Receipt`, `RecordUndo.actions` |
| Skip receipt `Pulado Pré-treino · {time}`: Minus icon, well and icon in `text/dim`, no chip, **Desfazer** only | app | `receiptIcon`, `receiptChip` (none for SKIPPED), `tone = textDim`, `RecordUndo.actions(SKIPPED)` = Desfazer |
| Receipt order: log first, then each skip | app (A59) | ADR-047 decision 2 |

`chatSD` (reference `chatU` 72:2727, light):

| Element | Class | Source |
|---|---|---|
| `Chat/Header`, `Chip/Date`, `Chat/Composer`, background bubbles | app | as in `chatU` |
| User bubble `Acabei não almoçando hoje.` | copy | synthetic |
| Bot bubble: `Almoço de hoje fora.` and the time; no estimate (a skip-only turn) | app + copy | ADR-047 decision 4; S29 |
| Card `Pular Almoço?` (body strong) and `Almoço tem 640 kcal registrados. O registro sai e o Almoço fica pulado.` (body, muted) | copy | ADR-047 decision 3 |
| **Excluir e pular**: status/bad pill, Trash icon, label in accent/on | app (A59) | `AeroDangerButton` (`Dialog/Confirm` Tone=Danger primary, `AeroChatComponents.kt`) |
| **Manter registro**: outlined pill | app (A59) | `OutlinePill` (`chatI`, `chatIC`) |
| Buttons stacked, full width (danger over outline) | app (A59) | `AdditionCard` (`chatI`): two long labels do not fit side by side at large text |
| `Registro mantido` mark | not drawn | plan § Scope 2 (existing receipt mark style) |

Component gap: `Chat/Receipt` has no danger card, so a variant `State=SkipDeletePending` is added (plan § Scope 2), built from `State=ReplacePending` with the `Dialog/Confirm` Tone=Danger primary.

### Build (2026-10-07)

- **Component:** `Chat/Receipt` `State=SkipDeletePending` (`154:235`, `Componentes`): `Question` and `Detail` bound to the set's properties; `Buttons` vertical, gap 10; `Danger` (`154:245`) = the `Dialog/Confirm` Tone=Danger primary (56 px status/bad pill) with the Trash icon and `Excluir e pular`; `Secondary` = the 58 px outline pill with `Manter registro`. The set description names the variant.
- **Token (owner request during the build):** the danger pill's label and icon are white in both themes. `accent/on` is `#03203a` in Dark, so a new variable `status/on-bad` (white in Light and Dark; scopes shape, text and stroke fill) carries them. Mirrored into `docs/design/tokens.json` (read 2026-10-07), `docs/tokens.md` (`node tools/gen-tokens.mjs`) and `web/public/css/tokens.css` (`npm --prefix web run tokens`); the app generates `AeroColors.statusOnBad` from the JSON at build time. `Dialog/Confirm` Tone=Danger keeps `accent/on` (its golds keep their bytes). Contrast of white on Dark `status/bad` (`#ff7a6b`) is about 2.5:1, below WCAG AA for text; recorded for the owner review.
- **Frames** (section "Chat · D6", after `chatRL`):

| Gold | Light | Dark | Size |
|---|---|---|---|
| `chatSK` · Chat com registro e pulo | `155:6711` (x 4780) | `155:6914` | 390 × 1050 |
| `chatSD` · Chat com pulo de refeição registrada | `155:6839` (x 5250) | `155:6936` | 390 × 844 |

  `chatSK` is a clone of `chatG`: log receipt (`+320 kcal`, Excluir · Trocar refeição · Editar), then the skip receipt (`Chat/Receipt` Saved: Title `Pulado`, Slot `Pré-treino`, Minus icon, well and icon bound to `text/dim`, no chip, Excluir and Trocar refeição hidden, Desfazer shown). The food names in the reply keep `chatG`'s highlight. `chatSD` is a clone of `chatU` without the estimate, with the receipt instance switched to `SkipDeletePending`. Dark rows are clones with the `Color` mode Dark.
- **Section:** "Chat · D6" widened to 5720 px; "Config e push · D7" moved 940 px right so the row keeps 160 px between sections. No gold node changed.

### Validation

1. Discovery table above, written before the first write.
2. Read-back (`use_figma`): 8 instances in each `chatSK` frame and 6 in each `chatSD` frame, none detached; zero visible solid paints without a variable in the four frames; no overlapping thread rows; Light frames in mode Light, Dark in Dark.
3. Long labels: a 40-character slot (`Lanche pós-treino da academia de sábado!`) wraps `Pular {slot}?` to two lines and the card grows to 262 px with nothing clipped. At 1.3× text, `Excluir e pular` measures 139 px of 264 px and `Manter registro` 150 of 288: both stay on one line in the stacked layout.
4. Review images via `node tools/export-figma.mjs --only <node ids> --dry-run` (no MCP calls), both themes; fixed during the build: the first pass wrote the reply into the user bubble (corrected), the swapped Trash icon lost its color, and the owner asked for a white label and icon in Dark (`status/on-bad`).
5. `node tools/check-docs.mjs` passes; `node tools/gen-tokens.mjs --check` passes; `npm --prefix web run check` passes the tokens step (its screens step, for `home1` and `chatE`, and its fonts step, for `OFL.txt`, already fail on `master` without this change).

Figma MCP budget: 15 of 30 calls (whoami, 2 skill reads, 12 `use_figma`).

### Owner OK and export (07/10/2026)

- The owner approved every frame in Figma ("Está tudo aprovado"), including the white label and icon of the danger pill in Dark and its contrast.
- `chatSK` (Dark `155:6914`, Light `155:6711`) and `chatSD` (Dark `155:6936`, Light `155:6839`) mapped in `tools/export-figma.mjs`; `node tools/export-figma.mjs --only chatSK,chatSD` wrote 780 × 2100 (`chatSK`) and 780 × 1688 (`chatSD`) PNGs per theme; both ids added to the inventory of `docs/qa/README.md`; `node tools/check-figma.mjs`: 96 golds verified (48 dark + 48 light). Existing golds keep their bytes.
