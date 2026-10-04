# Plan — D7 Release 1: Config and push

- Status: Concluído
- Date: 03/10/2026
- Owning context: `design`
- Affected code: none in `apps/` or `server/`. Figma `Design` → `Release 1` → section "Config e push". Repository: `docs/qa/figma/{dark,light}/{cfg,cfgS,wipe,push}.png` and their node ids in `tools/export-figma.mjs`.
- Prerequisites: [D6](../completed/d6-release1-chat-records-memory.md) `Concluído`.
- Figma MCP budget: ≤ 80 calls.

Authorization, lifecycle, cancellation and deferral: `docs/sdd/README.md`. Approval: `Aprovo o plano docs/design/plans/d7-release1-config-push.md. Implemente o plano aprovado.`

## Objective

Draw the profile/day settings, the per-day meals settings, the wipe dialog and the meal reminder notification in Aero, both themes. This closes the `Release 1` page.

## Sources (feature parity, ADR-031 § 4)

| Gold | Stitch title (layout reference only) |
|---|---|
| `cfg` | Configurações do perfil e dia |
| `cfgS` | Configurações com refeições por dia |
| `wipe` | Reiniciar registros de hoje - Diálogo Wipe |
| `push` | Notificação do sistema - Lembrete de refeição |

Behavior: [memoria-push](../../../produto/specifications/memoria-push.md) (Config, push), [ADR-021](../../../produto/adrs/ADR-021-refeicoes-por-dia.md). Code: the Config and push packages under `feature/`.

## Scope

Procedure as [D3 § Scope](../completed/d3-release1-home.md#scope) steps 1, 3, 4 and 5, applied to the four golds above. Flow-specific work:

1. **Config:** reuse the onboarding components (`Field/Number`, `Choice/Segmented`, `Row/MealSlot`, `Tabs/Weekday`) and `Dialog/Confirm` for `wipe`.
2. **Push:**
   - the notification is drawn by the system;
   - the app controls only the small icon, the title, the text, the actions and the accent color;
   - the frame shows a neutral lock screen with the notification, and only those fields follow Aero.
3. **Dev tools:** `Memória da IA (dev)` stays without a gold ([ADR-019](../../../produto/adrs/ADR-019-ferramentas-dev.md)).
4. **Review:** after the owner's OK, a final readability pass on `Release 1` as a whole (section order Splash e onboarding → Home → Chat → Config e push, spacing) with one page screenshot.

## Out of scope

- Compose ([A44](../../../android/plans/a44-config-push-aero.md)). Behavior or copy changes.

## Validation

As [D3 § Validation](../completed/d3-release1-home.md#validation), with 8 frames and the page screenshot.

## Results

Approved by the owner on 2026-10-04 ("Aprovo o plano docs/design/plans/d7-release1-config-push.md. Implemente o plano aprovado."). The D6 prerequisite is `Concluído`.

### Discovery (before the first write)

Sources read: `feature/config/ConfigScreen.kt` (list, sheets, `WipeDialog`), `ConfigUiState.kt`, `ConfigViewModel.kt` (`ConfigMapper`: row values), `domain/SlotsOfDay.kt` (`SlotModes`), `core/push/PushHandler.kt` and `domain/PushPlan.kt` (notification), `res/drawable/ic_notification.xml`, the gold fixtures in `StitchGoldTest.kt` (`CFG_DAY`, `CfgS`), the [memoria-push](../../../produto/specifications/memoria-push.md) specification (Config rules 1–9, push rules 1–6), ADR-019, ADR-021 and the four Stitch light golds. Classes as in D3: `app` (in code or spec, drawn), `gold-only` (dropped), `copy` (exact pt-BR text from the code), `spec` (missing from the gold, drawn from the spec).

**Common to `cfg`, `cfgS` (and the Config behind `wipe`)**

| Element | Class | Figma |
|---|---|---|
| Status bar, gesture bar | gold-only | dropped (system UI) |
| Back button (content description `Voltar`) + title `Configurações` | copy | new `Header/Page` (`IconButton/Glass` + `Icon/caret-left`, `Title`) |
| Warm glow behind the header | gold-only | dropped: the Aero page background and edge bubbles (ADR-030) |
| Section labels `METAS E LIMITES`, `HORÁRIOS DAS REFEIÇÕES`, `TREINO DE HOJE` | copy | screen text, `Label/Section` |
| Grouped cards with 1 px dividers | app | glass layout frames (`Surface/Glass`, `Glass`, `radius/card`) holding `Row/Setting` instances |
| Row `Meta de calorias`, detail `Mesmo valor todos os dias`, value `2000 kcal` in the accent | copy | new `Row/Setting` Value tone=Accent |
| Row `Compensação de treinos`, value `0% (desativado)` | copy | `Row/Setting` Value tone=Muted |
| Row `Macronutrientes (P · C · G)`, value `150g · 200g · 67g` | copy | `Row/Setting` |
| Row `Gasto calórico do treino`, detail `Crédito atual: 0 kcal`, value `Nenhum informado` | copy | `Row/Setting` |
| Note `Alterar a meta de calorias reinicia os registros do dia atual. O histórico da conversa será mantido.` with an info icon | copy | `Card/Note` (footer off) |
| Gold accent on the meta value (`2000 kcal`) | app | `accent/default` (Aero accent replaces the Stitch gold) |
| Edit sheets (`Meta de calorias`, `Compensação de treinos`, `Macronutrientes`, `Treino de hoje`) and the full-screen meal editor | app | no gold id, so no frame; their content is the onboarding components (`Field/Number`, `Choice/Segmented`, `Option/Card`, `Card/MacroTarget`, `Row/MealSlot`, `Tabs/Weekday`) inside `Sheet/Bottom`, already drawn in D3/D4 |
| `Memória da IA (dev)` row | — | dev only, no gold (ADR-019) |

**Per gold**

| Gold | Element | Class | Figma |
|---|---|---|---|
| `cfg` | Mode `same`: four rows `Café da manhã 07:30`, `Almoço 12:30`, `Lanche da tarde 16:00`, `Jantar 20:00` | copy | `Row/Setting` |
| `cfgS` | Mode label `Seg–Sex · Sáb–Dom` right of the section label | copy | screen text (`Caption`, `text/dim`) |
| `cfgS` | Rows `Seg a Sex` / `4 refeições · 07:30 a 20:00` and `Sáb e Dom` / `3 refeições · 09:30 a 20:30`, no value, chevron | copy | `Row/Setting` with detail, value hidden |
| `wipe` | Config behind the dialog, blurred, scrim | app | `cfg` content + 8 px layer blur + `overlay/scrim` |
| `wipe` | Badge with the restart icon in a `status/bad` tint | app | `Dialog/Confirm` Tone=Danger, `status/bad-tint` + `Icon/arrow-counter-clockwise` (Phosphor stands in for Material `RestartAlt`) |
| `wipe` | Title `Reiniciar registros de hoje?` | copy | `Dialog/Confirm` Title |
| `wipe` | Body `Ao atualizar sua meta calórica, as refeições de hoje serão reiniciadas para o novo cálculo de saldo. O histórico da conversa e os dias anteriores serão preservados.` | copy | `Dialog/Confirm` Body (new prop) |
| `wipe` | `Confirmar e reiniciar dia` (filled `status/bad`, restart icon) and `Cancelar` | copy | `Dialog/Confirm` Tone=Danger actions |
| `wipe` | Dark badge as a rounded square with a ring, light as a circle | gold-only | one circle badge in both themes (the Dark frame is a mode clone) |
| `push` | Lock screen wallpaper, clock `20:00`, date | app (system) | neutral flat `bg/page`, system type (Roboto); the date follows the fixture day 2026-09-25 (`sexta-feira`), the gold's `Quinta-feira` is a wrong weekday |
| `push` | Pill `RITMO CIRCADIANO ATIVO` | gold-only | dropped |
| `push` | Flashlight and camera shortcuts, status bar | gold-only | dropped (system UI) |
| `push` | Small icon: fork and knife on the accent | app | Aero `Icon/fork-knife` in `accent/default` (code: monochrome Material `restaurant` tinted by `setColor`) |
| `push` | App name `DIETA BOT` + `Agora` | app (system) | `Dieta Bot` · `agora` in system type; the name is the app label |
| `push` | Title `Lembrete do Jantar` + text `Você ainda não registrou sua refeição das 20:00. Deseja registrar agora ou pular?` | gold-only | replaced by the code's only line: title `Jantar. Ainda não registrou.` (`PushPlan.copy`, spec push rule 3), no content text |
| `push` | Actions `Registrar agora` (black pill) / `Pular refeição` | gold-only | the code's actions `Registrar` / `Pular` (spec push rule 4) as system text buttons in the accent |
| `push` | Accent color | app | `accent/default` (code: `setColor`) |

New component `Notification/Push` holds the system template; only the icon, title, actions and accent bind to Aero, the rest is neutral system styling (Roboto, `surface/2`, `text/*`).

### Delivered in Figma (2026-10-04)

**`Componentes`** → new section `Config e push` (160 px below `Onboarding`), each component with a description:

- `Header/Page` (prop Title): `IconButton/Glass` with `Icon/caret-left` + `Title`;
- `Row/Setting`, Value tone = Muted | Accent (props Title, Detail, Value, `Show detail`, `Show value`): `Body` title, `Caption` detail in `text/muted`, value in `Body` `text/muted` or `Body/Strong` `accent/default`, `Icon/caret-right` in `icon/muted`; no fill, the row sits in a glass group card;
- `Notification/Push` (props Title, Time, Action 1, Action 2): the Android template in Roboto on `surface/2` + `border/line`, `text/*`; Aero only on the small icon (`Icon/fork-knife` in `accent/on` on an `accent/default` circle), the title, the actions and the accent;
- `Dialog/Confirm` became a set, Tone = Default | Danger. Default is the D5 dialog unchanged (the `chatP` instances still read `Pular Lanche?` / `Pular` / `Cancelar`). Danger adds a 48 px `status/bad-tint` badge with `Icon/arrow-counter-clockwise` in `status/bad`, the Body prop (`Body`, `text/muted`), a `status/bad` pill with the restart icon and the Action prop in `accent/on`, and the same secondary pill.

**Variables.** None added.

**`Release 1` → section `Config e push · D7`** (1960 × 2202, right of the Chat sections, 160 px apart):

| Gold | Light | Dark |
|---|---|---|
| `cfg` | `78:3425` | `78:3664` |
| `cfgS` | `78:3520` | `78:3695` |
| `wipe` | `78:3596` | `78:3723` |
| `push` | `78:3646` | `78:3757` |

- Frame names `<id> · <Stitch title without the theme suffix> · Light|Dark`, 390 px wide, Light row above Dark row, 80 px apart. Dark frames are clones with the Color mode set to Dark and no other change.
- `cfg` grows to 917 px to show the whole list, `cfgS` is 845 px; `wipe` and `push` are 844 px.
- `cfg`/`cfgS`: `Header/Page`, three blocks (`Label/Section` label + glass group of `Row/Setting` with `border/line` dividers), `Card/Note`, three edge bubbles (decoration, absolute).
- `wipe`: the `cfg` content with an 8 px layer blur, an `overlay/scrim` layer and `Dialog/Confirm` Tone=Danger centered.
- `push`: a neutral flat `bg/page` lock screen, Roboto clock and date (system), `Notification/Push` below the clock.

### Figma MCP budget

14 calls of the 80 budgeted: 1 `whoami`, 3 skill reads, 10 `use_figma` (5 read-only inspections and audits, 5 writes). Review images from the REST export (`tools/export-figma.mjs --dry-run`), no MCP call.

### Validation

1. Discovery table: written above before the first Figma write.
2. Read-back of the 8 frames and the D7 components:
   - every DS element is an instance (`Header/Page`, `IconButton/Glass`, `Row/Setting`, `Card/Note`, `Dialog/Confirm`, `Notification/Push`, icons); 0 orphan instances;
   - 0 solid fills or strokes without a variable or a paint style;
   - text without a text style: only the system text of `push` (clock, date, notification template in Roboto), on purpose: the system draws it;
   - 0 overlapping in-flow siblings; the absolute layers are the edge bubbles, and in `wipe` the blurred Config, the scrim and the dialog.
3. Visual: the 8 frames exported at 2× and sent to the owner on 2026-10-04 (one fix before sending: the layout frames around the group cards clipped the card shadow into a square band). Owner review in Figma done the same day, no fixes requested.
4. Export and checks after the OK:
   - `node tools/export-figma.mjs --only cfg,cfgS,wipe,push`: 8 new files, 780 px wide (`cfg` 1834, `cfgS` 1690, `wipe` and `push` 1688 px tall), byte-identical to the images reviewed by the owner;
   - `node tools/check-figma.mjs`: 62 Figma gold PNGs verified (31 dark + 31 light);
   - `node tools/check-docs.mjs`: passed.
5. Final readability pass on `Release 1` (Scope 4), one read-back and one page screenshot:
   - section order left to right: `Splash e onboarding · D4`, `Home · D3`, `Chat · D5` with `Chat · D6` below it, `Config e push · D7`; 160 px between sections, 80 px pads inside every section, 0 overlapping sections or frames;
   - three copy fixes: the page description now names the four flows and drops the old line about pilot drafts; the D7 heading became `Config e push · D7`, like the other headings; the D6 note was rewritten in pt-BR to match the other notes.

### For the owner review (accepted)

1. `push`: the gold's title, text and pill buttons are replaced by what the app posts (`Jantar. Ainda não registrou.`, `Registrar`, `Pular`); the system template is drawn in Roboto with Aero variables for its colors, and the notification sits below the clock as Android draws it.
2. `push`: the date is `sexta-feira, 25 de setembro` (the fixture day 2026-09-25 is a Friday).
3. `wipe`: one circle badge in both themes; the danger pill uses `status/bad` with `accent/on` text.
4. The accent (meta value, push icon and actions) is `accent/default`, not the Stitch gold.
5. The edit sheets and the full-screen meal editor have no gold id and are not drawn.

### Owner review (2026-10-04)

The owner reviewed the section `Config e push · D7` in Figma and closed the plan: "Revisei no Figma, pode exportar os golds e concluir o D7." The five points above were accepted with no changes.

### Exported golds

`docs/qa/figma/dark/` and `docs/qa/figma/light/`: `cfg.png`, `cfgS.png`, `wipe.png`, `push.png`, mapped in `tools/export-figma.mjs` (`DARK_FRAMES` / `LIGHT_FRAMES`). The inventory source of the four ids stays `stitch` in `docs/qa/README.md`; [A44](../../../android/plans/a44-config-push-aero.md) switches it.

### Figma MCP budget, total

17 of 80: the 14 above, plus 3 for the readability pass (1 read-only `use_figma`, 1 copy write, 1 page `get_screenshot`). The export used no MCP call (REST API).
