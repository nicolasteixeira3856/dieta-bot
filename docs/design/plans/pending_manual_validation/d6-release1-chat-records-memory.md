# Plan — D6 Release 1: Chat records, photo and memory

- Status: Pendente aprovação manual
- Date: 03/10/2026
- Owning context: `design`
- Affected code: none in `apps/` or `server/`. Figma `Design` → `Release 1` → section "Chat" (second row block), new components in `Componentes`. Repository: `docs/qa/figma/{dark,light}/{chatF,chatA,chatG,chatU,chatD,chatR,chatM,chatS}.png` and their node ids in `tools/export-figma.mjs`.
- Prerequisites: [D5](../completed/d5-release1-chat-core.md) `Concluído`.
- Figma MCP budget: ≤ 110 calls, with the same split-day rule.

Authorization, lifecycle, cancellation and deferral: `docs/sdd/README.md`. Approval: `Aprovo o plano docs/design/plans/d6-release1-chat-records-memory.md. Implemente o plano aprovado.`

## Objective

Draw the remaining Chat states in Aero, both themes: photo, attachment, receipt, pending replacement, undone record, meal plan, memory notices and routine suggestion.

## Sources (feature parity, ADR-031 § 4)

| Gold | Stitch title (layout reference only) |
|---|---|
| `chatF` | Foto de refeição e estimativa no Chat |
| `chatA` | Chat com foto anexada |
| `chatG` | Confirmação pós-gravação com recibo duplo-check |
| `chatU` | Chat com substituição pendente |
| `chatD` | Chat com registro desfeito |
| `chatR` | Chat com plano de refeição |
| `chatM` | Chat com memória atualizada |
| `chatS` | Chat com sugestão da rotina |

Behavior: [chat](../../../produto/specifications/chat.md) (rules 17–19 and the receipt rules), [memoria-push](../../../produto/specifications/memoria-push.md), [ADR-017](../../../produto/adrs/ADR-017-registro-consolidado.md), [ADR-018](../../../android/adrs/ADR-018-foto-2048.md), [ADR-023](../../../produto/adrs/ADR-023-chat-v2-memoria-v2.md), [ADR-028](../../../produto/adrs/ADR-028-registro-autonomo.md). Code: `feature/chat/`.

## Scope

Procedure as [D3 § Scope](../completed/d3-release1-home.md#scope) steps 1, 3, 4 and 5, applied to the eight golds above. Flow-specific work:

1. **Components:**
   - `Chat/Receipt`: double check, Desfazer/Excluir/Trocar refeição/Editar per ADR-028; states `Saved`, `Undone`, `ReplacePending`;
   - `Chat/Photo` bubble;
   - `Chat/Composer` state `Attached` (thumbnail and ✕);
   - `Sheet/PhotoSource` (Tirar foto / Escolher da galeria);
   - `Chip/Memory`: 28 px, radius 14, the three labels of chat rule 17, informative only;
   - `Card/Routine` with Registrar / Quase igual;
   - `Card/MealPlan`.
2. **Meal photo:**
   - the Plugin API cannot fetch images, so the photo used by `chatF`/`chatA` is a file the owner provides, uploaded with `upload_assets`;
   - without one, the agent asks before generating an image (Figma AI credits);
   - the photo is content, not a UI screenshot.

## Out of scope

- Compose ([A43](../../../android/plans/a43-chat-records-memory-aero.md)). Behavior or copy changes.

## Validation

As [D3 § Validation](../completed/d3-release1-home.md#validation), with 16 frames.

## Results

Approved by the owner on 2026-10-04 ("Aprovo o plano docs/design/plans/d6-release1-chat-records-memory.md. Implemente o plano aprovado."). The D5 prerequisite was closed first the same day, on the owner's OK.

### Discovery (before the first write)

Sources read: `ChatScreen.kt` (thread items, `AssistantBubble`, `PlanBar`, `Composer`, `PhotoSheet`), `ChatRecordComponents.kt` (`ReceiptCard`, `ReceiptButton`, `ReplaceCard`), `ChatV2Components.kt` (`PlanText`, `PlanPanel`, `MemoryChip`, `RoutineSuggestionCard`), `ChatPhoto.kt` (`PhotoBubble`, `AttachmentThumb`), `ChatUiState.kt`, the test fixtures `ChatFixtures.kt` (the exact content of the eight golds), the [chat](../../../produto/specifications/chat.md) specification (rules 3, 5, 15–19), ADR-017, ADR-018, ADR-023, ADR-028 and the eight Stitch light golds. Classes as in D5: `app` (in code or spec, drawn), `gold-only` (dropped), `copy` (exact pt-BR text from the code), `spec` (missing from the gold, drawn from the spec).

**Common to the eight screens**

| Element | Class | Figma |
|---|---|---|
| Status bar, gesture bar, phone frame | gold-only | dropped (system UI) |
| Header, date chip `Hoje, 25 de setembro`, user bubble, bot label, composer | copy | the D5 components (`Chat/Header`, `Chip/Date`, `Chat/Bubble`, `Chat/BotLabel`, `Chat/Composer`) |
| Tune button top right (`chatU`, `chatR`, `chatM`, `chatS`), microphone in the composer | gold-only | dropped, as in D5 |
| Estimate card inside the bot bubble (`ENERGIA TOTAL`, kcal, three macros) | copy | `Chat/Estimate` (D5); the golds' `ENERGIA ESTIMADA` / `ESTIMATIVA NUTRICIONAL` labels are gold-only, the code has one label |
| A thread longer than the screen | app | the frame grows to show the whole thread (no cut), 390 px wide |

**Per gold**

| Gold | Element | Class | Figma |
|---|---|---|---|
| `chatF` | User photo bubble: photo of a prato feito, tag `Visão Computacional` with a sparkle, caption `Almoço de hoje`, `12:41` + double check | copy | new `Chat/Photo`; the photo is the owner's Stitch photo `apps/android/app/src/test/resources/chatF-photo.jpg`, uploaded with `upload_assets` |
| `chatF` | Bot `Identifiquei um Prato Feito com filé de frango grelhado, arroz, feijão e salada verde.` (`Prato Feito` in the accent), estimate `~680` · `48g P` · `82g C` · `18g G`, time `12:41` | copy | bot bubble + `Chat/Estimate` |
| `chatF` | Back chevron inside the bot bubble header | gold-only | dropped: the bot label is `Chat/BotLabel` |
| `chatF` | Receipt `Registrado em Almoço · 12:30`, chip `+680 kcal`, double check in a `status/good` well | copy | new `Chat/Receipt` State=Saved |
| `chatF` | Actions `Excluir` (bad), `Trocar refeição`; a photo has no `Editar` (rule 19) | copy | `Chat/Receipt` actions, Editar hidden |
| `chatA` | Empty day: greeting, `Meta calórica de hoje` card | copy | as `chat0` |
| `chatA` | Suggestion chips | spec | dropped: rule 15 hides them with a photo attached |
| `chatA` | Composer with the attachment: 64 px thumbnail (owner's photo `chatA-photo.jpg`) with a ✕ (`Remover foto`), caption `almoço de hoje, comi tudo`, camera and accent send | copy | new `Chat/Composer` State=Attached |
| `chatA` | Header subtitle `Assistente de refeições` in sentence case | gold-only | `Chat/Header` keeps `ASSISTENTE DE REFEIÇÕES` |
| `chatG` | User `2 pães franceses com 2 ovos mexidos no café da manhã` `20:15`; bot as `chatE` without the slot question | copy | `Chat/Bubble` + bot bubble |
| `chatG` | Receipt `Registrado em Café da manhã · 07:30` `+380 kcal`; actions `Excluir`, `Trocar refeição`, `Editar` | copy | `Chat/Receipt` State=Saved |
| `chatG` | Bordered composer text field | gold-only | `Chat/Composer` State=Default |
| `chatU` | User `Também comi um pudim de leite no jantar` `21:02`; bot `Juntei o pudim ao jantar. A estimativa total é de:` with `~620` · `30g P` · `82g C` · `19g G`, `21:02` | copy | bubbles + `Chat/Estimate` |
| `chatU` | Card `Substituir Jantar?`, `Jantar tem 380 kcal. Fica com 620 kcal.`, `Substituir` (CTA tokens) and `Outra refeição` (outline), 88 % of the width | copy | `Chat/Receipt` State=ReplacePending (`Button/Primary` without icon + outline pill) |
| `chatD` | Bot `Juntei o pudim ao jantar.` with the 620 estimate | copy | bot bubble + `Chat/Estimate` |
| `chatD` | Receipt `Atualizado em Jantar · 20:00` `380 → 620 kcal` at 50 % with the mark `Desfeito` (undo icon, muted) | copy | `Chat/Receipt` State=Undone |
| `chatD` | Receipt `Restaurado em Jantar · 20:00` with `380 kcal` on the title row, history icon; actions `Excluir`, `Trocar refeição`, `Editar` | copy | `Chat/Receipt` State=Saved, inline chip |
| `chatR` | User `Vou fazer uma pizza de pão sírio na janta. Quantas gramas de cada item?` `20:15` | copy | `Chat/Bubble` |
| `chatR` | Plan text line by line (`Para caber nas 560 kcal que sobram hoje:`, five bullets, `Monte e leve ao forno a 200 °C por 8 a 10 min.`, `Total: ~420 kcal · 40P · 38C · 12G` with P/C/G in the macro colors) | copy | bot bubble text with macro-colored ranges |
| `chatR` | Projected day `Dia: 1.640 → 2.060 de 2.200 kcal`, `P 126/167 · C 190/223 · G 58/74` (values in the macro colors), `bad` above the ceiling (rule 16) | copy / spec | new `Card/MealPlan`, Over = false / true |
| `chatR` | `Registrar assim` with a check-circle in the actions slot | copy | `Chat/ActionBar` |
| `chatM` | User `Café da manhã igual ao de sempre, mas hoje com pão integral` `20:15`; bot `Usei o seu café de sempre, com pão integral no lugar do francês e leite semidesnatado, como você costuma usar.`, estimate `~430` · `26g P` · `36g C` · `20g G`, `Deseja registrar essa refeição no Café da manhã?` | copy | bot bubble + `Chat/Estimate` |
| `chatM` | Chips `Memória atualizada` (accent check), `Memória permanente` (pin), `Memória dinâmica` (sync), one per line under the bubble, above the time; 28 px, radius 14, no tap | copy | new `Chip/Memory`, Kind = Updated / Permanent / Dynamic |
| `chatM` | Action bar `Gravar café` · `Trocar` · `Pular` | gold-only | replaced by the one `Registrar` pill of an `ask` estimate (ADR-028, `Chat/ActionBar`) |
| `chatS` | Empty day: greeting, meta card, suggestion chips | copy | as `chat0` |
| `chatS` | Card `O de sempre no Café da manhã?`, `2 ovos mexidos, 1 pão francês c/ manteiga, 200 ml leite semidesnatado, café`, `440 kcal · 25P · 38C · 22G` (macro colors), chip `Memória dinâmica`, `Registrar` (CTA) and `Quase igual` | copy | new `Card/Routine` |
| — | Photo chooser `Enviar foto do prato`, `O texto digitado vai junto como legenda. Até 16 MB.`, `Tirar foto`, `Escolher da galeria`, `Cancelar` | app | new `Sheet/PhotoSource` (content of `Sheet/Bottom`); no gold id, so no frame |

Not drawn (no gold id in D6): `Não registrado`, the skip receipt `Pulado {slot}`, the move receipt and its marks `Excluído`, `Movido`, `Removido para editar` (the `Chat/Receipt` mark is a text property).

### Delivered in Figma (2026-10-04)

**`Componentes`** (Ícones section grown to its grid, Chat section grown to its components; every section restacked 160 px apart)

- Nine new icons in `Ícones` (Phosphor core 2.1.1 regular, MIT, colour bound to `icon/primary`): `arrow-counter-clockwise`, `trash`, `arrows-left-right`, `pencil-simple`, `clock-counter-clockwise`, `push-pin`, `arrows-clockwise`, `image`, `x`.
- New components in the `Chat` section, each with a description:
  - `Chip/Memory`, Kind = Updated | Permanent | Dynamic: 28 px, pill radius (the 14 of a 28 px chip), `surface/2` + `border/line`, Caption; the accent only on the `Memória atualizada` check;
  - `Chat/ReceiptAction`, Tone = Neutral | Danger (props Label, Icon swap): 44 px glass row, `radius/card`, `Excluir` in `status/bad`;
  - `Chat/Receipt`, State = Saved | Undone | ReplacePending: `status/good` well with the double check (Icon swap, `clock-counter-clockwise` for `Restaurado`), `{Title} {Slot} · {Time}`, kcal chip below or inline, the four stacked actions behind booleans (`Show actions`, `Show Desfazer`, `Show Editar`); Undone dims the card to 50 % with the `Desfeito` mark; ReplacePending is the `Substituir {slot}?` card with `Button/Primary` and an outline `Outra refeição`. The 15 % `status/good` tints are layers at 15 % opacity (a paint opacity on a bound paint did not survive in instances);
  - `Chat/Photo` (props Caption, Time): user-bubble paints, the photo as an image fill, the `Visão Computacional` glass tag, time and double check;
  - `Chat/Composer` gained State = Attached: `radius/card` box, 64 px thumbnail with the ✕ (`x` icon) on its corner, camera, caption and send;
  - `Card/MealPlan`, Over = false | true (props Eaten, Projected, Ceiling, P, C, G);
  - `Card/Routine` (props Title, Text, Kcal, P, C, G; nested `Chip/Memory`, `Button/Primary`, `surface/2` pill `Quase igual`);
  - `Sheet/PhotoSource`, added to the preferred values of the `Sheet/Bottom` Content swap.
- Meal photo: no separate file from the owner, so the owner's Stitch photo in the repository (`apps/android/app/src/test/resources/chatF-photo.jpg`) was used, cropped to x 64–484, y 0–208 to drop the baked-in `Visão Computacional` tag, and uploaded once with `upload_assets` (image hash `66ccb689…`) into `Chat/Photo` and the `Attached` thumbnail. `chatA-photo.jpg` was not used: it carries the gold's ✕. No image was generated.

**Variables.** None added.

**`Release 1` → section `Chat · D6`** (below `Chat · D5`, 160 px apart):

| Gold | Light | Dark |
|---|---|---|
| `chatF` | `72:2465` | `72:3213` |
| `chatA` | `72:2586` | `72:3234` |
| `chatG` | `72:2635` | `72:3248` |
| `chatU` | `72:2727` | `72:3269` |
| `chatD` | `72:2849` | `72:3290` |
| `chatR` | `72:2959` | `72:3312` |
| `chatM` | `72:3034` | `72:3333` |
| `chatS` | `72:3104` | `72:3359` |

- Frame names `<id> · <Stitch title without the theme suffix> · Light|Dark`, 390 px wide, Light row above Dark row, 80 px apart. Dark frames are clones with the Color mode set to Dark and no other change.
- The thread screens start from the D5 `chatE` column and the empty-day screens (`chatA`, `chatS`) from `chat0`. A frame keeps 844 px when the thread fits and grows to the whole thread otherwise: `chatF` 961, `chatG` 902, `chatD` 942, `chatR` 882, `chatM` 925 px.
- `chatM` moves the time below the memory chips (rule 17), out of the bubble.

### Figma MCP budget

23 calls of the 110 budgeted: 1 `whoami`, 3 skill reads, 18 `use_figma` (7 read-only inspections and audits, 1 of them failed and retried; 11 writes), 1 `upload_assets`. Review images from the REST export (`tools/export-figma.mjs --dry-run`), no MCP call.

### Validation

1. Discovery table: written above before the first Figma write.
2. Read-back of the 16 frames and the D6 components:
   - every DS element is an instance (`Chat/Header`, `Chip/Date`, `Chat/Bubble`, `Chat/Photo`, `Chat/BotLabel`, `Chat/Estimate`, `Card/MealPlan`, `Chip/Memory`, `Chat/Receipt`, `Chat/ReceiptAction`, `Chat/MetaCard`, `Card/Routine`, `Chat/ActionBar`, `Chat/Composer`, `Chip/Log`, `Button/Primary`, `IconButton/Glass`, icons); 0 orphan instances;
   - 0 solid fills or strokes without a variable or a paint style and 0 text without a text style, in the 16 frames and in the new components;
   - the frames are auto-layout columns: 0 overlapping in-flow siblings; the absolute layers are the edge bubbles, the photo tag, the ✕ of the thumbnail and the receipt tints.
3. Visual: the 16 frames exported at 2× and sent to the owner on 2026-10-04. Owner review in Figma pending.

### For the owner review

1. The photo is a crop of the Stitch photo (no separate file was provided); a new photo can replace it in `Chat/Photo` and the `Attached` thumbnail in one place.
2. The `Visão Computacional` tag is an Aero glass pill (theme colors) instead of the app's black 60 % pill with white text, which has no token.
3. `chatU`: the `Substituir Jantar?` card fills the thread width (the app uses 88 %) so `Substituir` and `Outra refeição` fit side by side at the Button size.
4. `chatM`: the gold's `Gravar café · Trocar · Pular` bar is replaced by the one `Registrar` pill (ADR-028).
5. Receipt and photo-chooser rows use glass buttons and `Body`/`Button` styles; the app's 13.5–15 sp sizes map to the Aero ramp in A43.
6. Frames taller than 844 px show the whole thread (`chatF`, `chatG`, `chatD`, `chatR`, `chatM`).
