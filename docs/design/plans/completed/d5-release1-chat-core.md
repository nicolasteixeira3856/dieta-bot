# Plan — D5 Release 1: Chat core

- Status: Concluído
- Date: 03/10/2026
- Owning context: `design`
- Affected code: none in `apps/` or `server/`. Figma `Design` → `Release 1` → section "Chat", new components in `Componentes`. Repository: `docs/qa/figma/{dark,light}/{chat0,chatL,chatQ,chatE,chatT,chatP,chatX}.png` and their node ids in `tools/export-figma.mjs`.
- Prerequisites: [D4](d4-release1-splash-onboarding.md) `Concluído` (it adds `Choice/Segmented` and `Dialog/TimeWheel` patterns reused here).
- Figma MCP budget: ≤ 110 calls, with the same split-day rule.

Authorization, lifecycle, cancellation and deferral: `docs/sdd/README.md`. Approval: `Aprovo o plano docs/design/plans/d5-release1-chat-core.md. Implemente o plano aprovado.`

## Objective

Draw the core Chat states in Aero, both themes: empty, loading, question before the estimate, estimate, slot picker, skip confirmation and text-too-long error.

## Sources (feature parity, ADR-031 § 4)

| Gold | Stitch title (layout reference only) |
|---|---|
| `chat0` | Chat vazio |
| `chatL` | Chat loading - Estimando |
| `chatQ` | Chat com pergunta antes da estimativa |
| `chatE` | Estimate com botões de ação |
| `chatT` | Selecionar refeição - Bottom Sheet |
| `chatP` | Diálogo de confirmação para pular refeição |
| `chatX` | Chat com texto longo demais |

Behavior: [chat](../../../produto/specifications/chat.md), [ADR-022](../../../produto/adrs/ADR-022-limite-texto-chat.md), [ADR-026](../../../produto/adrs/ADR-026-perguntas-antes-da-estimativa.md), [ADR-028](../../../produto/adrs/ADR-028-registro-autonomo.md). Code: `apps/android/app/src/main/java/com/nutri/android/feature/chat/ChatScreen.kt`.

Parity points already known (ADR-031 § 4):

- no microphone in the composer;
- no tune button in the header (the app keeps only its space);
- the composer placeholder is "Descreva sua refeição ou envie foto...";
- the bot label is "Dieta Bot AI".

## Scope

Procedure as [D3 § Scope](d3-release1-home.md#scope) steps 1, 3, 4 and 5, applied to the seven golds above. Flow-specific work:

1. **Components:**
   - `Chat/Header`: back button, title with dot, subtitle, 44 px spacer;
   - `Chat/Composer` states: `Default`, `Blocked`, `TooLong` (counter and error copy per ADR-022);
   - `Chat/Estimate`: energy, macros with semantic colors, and the actions the spec defines for `chatE`;
   - `Chat/Question` with the "Forçar estimativa" bar (ADR-026);
   - `Loader/Aero`, the replacement for the M3 `LoadingIndicator`;
   - `Dialog/Confirm` for `chatP`;
   - slot list rows in `Sheet/Bottom` for `chatT`;
   - `Chip/Date` for the day separator.
2. **The pilot Chat draft** is replaced by `chatE` and deleted.

## Out of scope

- Photo, receipts, replacement, undo, plan, memory and routine states ([D6](../d6-release1-chat-records-memory.md)). Compose ([A42](../../../android/plans/a42-chat-core-aero.md)).

## Validation

As [D3 § Validation](d3-release1-home.md#validation), with 14 frames.

## Results

Approved by the owner on 2026-10-04 ("Aprovo o plano docs/design/plans/d5-release1-chat-core.md. Implemente o plano aprovado.").

### Discovery (before the first write)

Sources read: `ChatScreen.kt`, `ChatV2Components.kt`, `ChatRecordComponents.kt` (`RegisterBar`), `ChatUiState.kt`, `ChatViewModel.kt` (greeting, date separator, `emptyDay`, `forceEstimate`), `domain/ChatText.kt`, `feature/home/HomePanelScreen.kt` (`SkipDialog`), `core/designsystem` (`SheetActions`, `formatRemaining`), the [chat](../../../produto/specifications/chat.md) specification, ADR-022, ADR-026, ADR-028 and the seven Stitch light golds. Class: `app` = exists in code or spec and is drawn; `gold-only` = dropped; `copy` = exact pt-BR text from the code; `spec` = missing from the gold, drawn from the spec.

**Common to the Chat screens (`chat0`, `chatL`, `chatQ`, `chatE`, `chatX`, and the Chat behind `chatT`)**

| Element | Class | Figma |
|---|---|---|
| Status bar, gesture bar, phone frame | gold-only | dropped (system UI) |
| Back button with a chevron (content description `Voltar`) | app | `IconButton/Glass` + `Icon/caret-left` in `Chat/Header` |
| Title `Chat Dieta Bot` + accent dot, subtitle `ASSISTENTE DE REFEIÇÕES` | copy | `Chat/Header` (`chat0` and `chatX` golds draw a different header: replaced by the code's) |
| Tune button top right | gold-only | dropped; its 44 px space stays (`Chat/Header` spacer) |
| Date separator `Hoje, 25 de setembro` | copy | `Chip/Date` |
| User bubble at the right, time + double check | copy | `Chat/Bubble` Sender=User (D1) |
| Bot label: lightning in a tinted circle + `Dieta Bot AI` | copy | `Chat/BotLabel` |
| Composer: camera button, placeholder `Descreva sua refeição ou envie foto...`, accent send | copy | `Chat/Composer` State=Default |
| Microphone in the composer | gold-only | dropped |
| Footer gradient over the thread | app | dropped as a layer: the frame shows the whole thread, the footer closes the column |
| Bubble-to-thread gaps 16, question hugging its estimate | app | thread column, gap 16 |

**Per gold**

| Gold | Element | Class | Figma |
|---|---|---|---|
| `chat0` | Greeting `Olá! Descreva o que você comeu ou envie uma foto do prato para estimarmos as calorias e macros.` + time `20:14`, no bot label | copy | `Chat/Bubble` Sender=Bot |
| `chat0` | Card `Meta calórica de hoje`, `1.450 / 2.100 kcal restantes`, `69%`, fork-and-knife well | copy | `Chat/MetaCard` |
| `chat0` | Suggestion chips `📸 Tirar foto do prato`, `☕ Café com 2 ovos mexidos`, `🥗 Salada Caesar` (scroll row, third cut at the edge) | copy | `Chip/Log` Neutral ×3 in a clipped row |
| `chatL` | User `2 pães franceses com 2 ovos mexidos no café da manhã` `20:15` | copy | `Chat/Bubble` Sender=User |
| `chatL` | Loading bubble: loader, `Analisando e calculando estimativa...`, `MICRO & MACRONUTRIENTES`, label `Dieta Bot AI` under the bubble | copy | glass bubble + `Loader/Aero` (replaces the M3 `LoadingIndicator`) |
| `chatL` | Suggestion chips under the thread | gold-only | dropped: with a message pending the day is not empty (`emptyDay` false), so no chips and no greeting |
| `chatQ` | User `Jantei macarrão com frango ao molho branco` `20:12` | copy | `Chat/Bubble` Sender=User |
| `chatQ` | Question 1 `O molho branco levou creme de leite ou requeijão? E o macarrão, foi 1 prato raso ou fundo?` `20:12`; question 2 `O frango foi grelhado ou empanado?` `20:15`; each with its own bot label, accent left bar, help icon, time below | copy | `Chat/Question` ×2 |
| `chatQ` | User answer `Creme de leite, prato fundo` `20:14` | copy | `Chat/Bubble` Sender=User |
| `chatQ` | `Forçar estimativa` with a fast-forward icon in the actions slot (second question in a row) | copy | `Chat/ActionBar` + new `Icon/fast-forward` |
| `chatQ` | 18 sp question text | app | `Body/Strong` (16): the Aero ramp has no 18; A42 maps the size |
| `chatE` | User `2 pães franceses com 2 ovos mexidos no café da manhã` `20:15` | copy | `Chat/Bubble` Sender=User |
| `chatE` | Bot text `Identifiquei 2 pães franceses e 2 ovos mexidos. A estimativa total é de:` with the item names in the accent | copy | glass bot bubble, highlight ranges `accent/default` |
| `chatE` | Estimate card `ENERGIA TOTAL` `~380 kcal`, `PROTEÍNA 22g P`, `CARBO 36g C`, `GORDURA 16g G` in the macro colors | copy | `Chat/Estimate` (kcal in `text/primary`: every number is primary in Aero) |
| `chatE` | `Deseja registrar essa refeição no Café da manhã?` with the slot emphasised, time `20:15` | copy | bubble text, slot range `Body/Strong` |
| `chatE` | One `Registrar` pill with a check-circle in the actions slot (ADR-028) | copy | `Chat/ActionBar` |
| `chatT` | Chat behind, blurred and dimmed | app | `chatE` content + layer blur + `overlay/scrim` |
| `chatT` | Sheet: grabber, `Selecione a refeição`, `Escolha o momento do dia para salvar este registro:` | copy | `Sheet/Bottom` + `Sheet/SlotList` content |
| `chatT` | Rows `Café da manhã 07:30`, `Almoço 12:30`, `Lanche 16:00`, `Jantar 20:00 (atual)` selected, band icon well, radio / check | copy | `Row/SlotPick` Selected = false/true (icons `coffee`, `fork-knife`, `cookie`, `bowl-food`, as the D4 bands) |
| `chatT` | `Confirmar refeição` with an arrow | copy | `Sheet/Bottom` primary (`Button/Primary` with `arrow-right`) |
| `chatT` | `Cancelar` as a text link | app | `Sheet/Bottom` secondary pill (the Aero sheet's cancel, same action) |
| `chatT` | Wordmark `Dieta Bot` over the scrim | gold-only | dropped |
| `chatP` | Skip confirmation from the Home timeline (spec: `chatP` left the Chat with ADR-028 and stays as the skip reference) | app | `Dialog/Confirm` over a dimmed `home0` |
| `chatP` | Title `Pular Lanche?` (`Pular {nome}?`), `Pular` / `Cancelar` | copy | `Dialog/Confirm` |
| `chatP` | Gold title `Deseja pular o Lanche da tarde?`, body `Nenhuma caloria será somada hoje…`, slot icon, `16:00` time chip, `Pular refeição` with an icon | gold-only | dropped: the code draws title and two actions only |
| `chatX` | Same top as `chat0` (greeting, meta card, chips) | copy | as `chat0` |
| `chatX` | Composer over 2000 characters: 5 visible lines of `Hoje no almoço comi arroz branco, feijão carioca, duas coxas de frango assadas sem pele, salada de alface com tomate e cebola, uma colher de farofa, …`, `status/bad` border, rounded box, camera and send disabled, buttons at the bottom | copy | `Chat/Composer` State=TooLong |
| `chatX` | `Texto muito longo` under the box in `status/bad` | copy | `Chat/Composer` State=TooLong |
| `chatX` | Character counter (named in the plan) | — | none: ADR-022 decision 3 says no counter, so `TooLong` has none |
| — | Composer `Blocked` state (named in the plan) | — | none apart from `TooLong`: the app blocks camera and send only for the too-long text, and draws nothing different while sending; `Blocked` is folded into `TooLong` |

Not drawn (no gold id in D5): the failure bubble `Não deu. Toque para tentar de novo.`, the notice `Foto grande demais.`, the photo chooser, the loading indicator of older pages. They exist in the app; photo states are D6.

### Delivered in Figma (2026-10-04)

**`Componentes`** (section `Chat`, now wrapping its components 80 px apart; sections below moved to keep 160 px)

- New icon `Icon/fast-forward (regular)` in `Ícones` (Phosphor core 2.1.1, MIT, colour bound to `icon/primary`).
- `Chat/Composer` became a set, State = Default | TooLong:
  - Default is the D1 pill (radius now bound to `radius/pill`);
  - TooLong: `radius/card` box with a 1.5 px `status/bad` border, 5 visible lines of text, camera and send disabled (`text/dim` icons, send on `surface/2`), buttons at the bottom, `Texto muito longo` (Caption, `status/bad`) aligned with the text; no counter, no separate Blocked state.
- New components, each with a description:
  - `Loader/Aero` (24 px ring, `border/line` track, `accent/default` arc), the M3 `LoadingIndicator` replacement;
  - `Chat/BotLabel` (lightning in a `surface/tint` well + `Dieta Bot AI`);
  - `Chip/Date` (prop Label);
  - `Chat/Header` (`IconButton/Glass` + `caret-left`, title + accent dot, `ASSISTENTE DE REFEIÇÕES`, 44 px spacer);
  - `Chat/ActionBar` (props Label, Icon swap): `Registrar` + check-circle, `Forçar estimativa` + fast-forward;
  - `Chat/Question` (props Question, Time; nested `Chat/BotLabel`, 3 px accent bar, question icon in the accent);
  - `Chat/Estimate` (props Kcal, Protein, Carbs, Fat; semantic macro colors);
  - `Chat/MetaCard` (props Remaining, Percent);
  - `Row/SlotPick`, Selected = false | true (props Name, Time, Icon swap);
  - `Sheet/SlotList` (subtitle + four exposed `Row/SlotPick`), added to the preferred values of the `Sheet/Bottom` Content swap;
  - `Dialog/Confirm` (props Title, Secondary; exposed `Button/Primary` without icon; `radius/sheet` glass).
- The D1 `Chat/Bubble` (User | Bot) is reused as is: its time sits inside the bubble.

**Variables.** None added.

**`Release 1` → section `Chat · D5`** (3370 × 2056, the last section of the page):

| Gold | Light | Dark |
|---|---|---|
| `chat0` | `62:1745` | `63:2079` |
| `chatL` | `62:1796` | `63:2097` |
| `chatQ` | `62:1842` | `63:2118` |
| `chatE` | `62:1930` | `63:2136` |
| `chatT` | `63:1911` | `63:2176` |
| `chatP` | `63:2023` | `63:2201` |
| `chatX` | `62:1998` | `63:2158` |

- Frame names `<id> · <Stitch title without the theme suffix> · Light|Dark`, 390 × 844, Light row above Dark row, 80 px apart. Dark frames are clones with the Color mode set to Dark and no other change.
- The Chat screens are a column: `Chat/Header`, thread (gap 16), a flexible spacer, then chips (empty day), the actions slot and the composer.
- `chatT` is the `chatE` content in a `Chat` frame with an 8 px layer blur, an `overlay/scrim` layer and a `Sheet/Bottom` instance (title `Selecione a refeição`, Content `Sheet/SlotList`, primary `Confirmar refeição` with the arrow, secondary `Cancelar`).
- `chatP` is the `home0` content in a `Home` frame with the same blur and scrim and a centred `Dialog/Confirm` (`Pular Lanche?`, `Pular`, `Cancelar`).
- The edge bubbles (ADR-030) sit outside the 20 px content column.
- The pilot drafts `rascunho · Chat · Light|Dark` were deleted.

### Figma MCP budget

18 calls of the 110 budgeted: 1 `whoami`, 3 skill reads, 13 `use_figma` (6 read-only inspections and audits, 1 of them failed and retried; 7 writes), 1 `get_screenshot`. The review images come from the REST export (`tools/export-figma.mjs --dry-run`), which does not use the MCP.

### Validation

1. Discovery table: written above before the first Figma write.
2. Read-back of the 14 frames and of the D5 components:
   - every DS element is an instance (`Chat/Header`, `Chip/Date`, `Chat/Bubble`, `Chat/BotLabel`, `Chat/Question`, `Chat/Estimate`, `Chat/MetaCard`, `Chat/ActionBar`, `Chat/Composer`, `Loader/Aero`, `Chip/Log`, `Sheet/Bottom`, `Sheet/SlotList`, `Row/SlotPick`, `Dialog/Confirm`, `Button/Primary`, `IconButton/Glass`, icons); no detached copy;
   - 0 solid fills or strokes without a variable or a paint style, and 0 text without a text style, in the frames and in the new components. The one exception is the workout value `Informar` of the D3 `home0` content cloned behind the `chatP` dialog (blurred, owned by D3);
   - 0 overlapping in-flow siblings; the absolute layers are the edge bubbles, the scrims, the sheet, the dialog and the question accent bar;
   - `Componentes`: 0 components without a description, every gap between sections 160, 0 variables with `ALL_SCOPES`.
3. Visual: the 14 frames exported at 2× and sent to the owner on 2026-10-04. Owner review in Figma done the same day, no fixes requested.
4. Export and checks after the OK:
   - `node tools/export-figma.mjs --only chat0,chatL,chatQ,chatE,chatT,chatP,chatX`: 14 new files, 780 × 1688 px;
   - `node tools/check-figma.mjs`: 38 Figma gold PNGs verified (19 dark + 19 light);
   - `node tools/check-docs.mjs`: passed.

### For the owner review (accepted)

1. `chatP` is drawn as the app's skip dialog over the Home (`Pular Lanche?`, two actions), not the gold's richer dialog.
2. `chatT` uses the Aero `Sheet/Bottom` secondary pill for `Cancelar` (the app draws a text link) and the `chatE` thread behind it.
3. The composer has no `Blocked` variant and no counter (ADR-022); `TooLong` carries both the disabled buttons and the error copy.
4. The question text uses `Body/Strong` (16) where the code draws 18 sp; the estimate kcal is `text/primary`, not the accent.
5. Suggestion chips reuse `Chip/Log` Neutral with the emoji in the text.

### Owner review (2026-10-04)

The owner reviewed the section `Chat · D5` in Figma and gave the OK in the chat that approved D6 ("D5 aprovado: fechar e seguir"). The five points above were accepted with no changes.

### Exported golds

`docs/qa/figma/dark/` and `docs/qa/figma/light/`: `chat0.png`, `chatL.png`, `chatQ.png`, `chatE.png`, `chatT.png`, `chatP.png`, `chatX.png`, mapped in `tools/export-figma.mjs` (`DARK_FRAMES` / `LIGHT_FRAMES`). The inventory source of the seven ids stays `stitch` in `docs/qa/README.md`; [A42](../../../android/plans/a42-chat-core-aero.md) switches it.

### Figma MCP budget, total

18 of 110. The review and the closure used no MCP call (export through the REST API).
