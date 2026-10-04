# Plan — D4 Release 1: Splash and onboarding

- Status: Pendente aprovação manual
- Date: 03/10/2026
- Owning context: `design`
- Affected code: none in `apps/` or `server/`. Figma `Design` → `Release 1` → section "Splash e onboarding", new components in `Componentes`. Repository: `docs/qa/figma/{dark,light}/{splash,o1,o1e,o2,o3,o3t,o3s,o4}.png` and their node ids in `tools/export-figma.mjs`.
- Prerequisites: [D3](../completed/d3-release1-home.md) `Concluído` (it adds `Field/Number` and `Sheet/Bottom`).
- Figma MCP budget: ≤ 110 calls. If the cap is reached, stop after a complete screen and resume the next day.

Authorization, lifecycle, cancellation and deferral: `docs/sdd/README.md`. Approval: `Aprovo o plano docs/design/plans/d4-release1-splash-onboarding.md. Implemente o plano aprovado.`

## Objective

Draw splash and onboarding O1–O4 with their states in Aero, both themes, with the features the app has today.

## Sources (feature parity, ADR-031 § 4)

| Gold | Stitch title (layout reference only) |
|---|---|
| `splash` | Nutri Splash Screen |
| `o1` | Onboarding 1/4 - Teto do dia |
| `o1e` | Onboarding 1/4 - Teto do dia sem perfil |
| `o2` | Onboarding 2/4 - Compensação de treinos |
| `o3` | Onboarding 3/4 - Distribuição das refeições |
| `o3t` | Onboarding 3/4 - Seletor de horário |
| `o3s` | Onboarding 3/4 - Refeições Sáb e Dom |
| `o4` | Onboarding 4/4 - Alvos de macronutrientes |

Behavior: [perfil-onboarding](../../../produto/specifications/perfil-onboarding.md), [ADR-012](../../../produto/adrs/ADR-012-chat-home-perfil.md) (4 screens), [ADR-021](../../../produto/adrs/ADR-021-refeicoes-por-dia.md). Code: `apps/android/app/src/main/java/com/nutri/android/feature/onboarding/` and the splash.

## Scope

Procedure as [D3 § Scope](../completed/d3-release1-home.md#scope) steps 1, 3, 4 and 5 (discovery table first, Light row then Dark clone, owner review, export), applied to the eight golds above. Flow-specific work:

1. **Components:**
   - `Choice/Segmented`, the Aero replacement for the M3 `ButtonGroup` used in O1/O2: glass container with a glossy accent pill for the selected item, labels up to 3 lines;
   - `Option/Card` from D1 for the eat-back choices, with the typed-% field when "Porcentagem personalizada" is selected (default 50, ADR-012);
   - `Stepper/Progress`, 4 steps;
   - `Row/MealSlot`: slot name, time and toggle, for O3;
   - `Dialog/TimeWheel` for `o3t`;
   - `Tabs/Weekday` for `o3s`;
   - `Field/Number` with unit, for O1 and O4.
2. **Splash:**
   - Aero page background, the current mark and the current wordmark/copy ([splash gold](../../../qa/stitch/dark/splash.png) and the code);
   - the mark is uploaded with `upload_assets` from `design/brand/`;
   - no new brand: `Branding` stays empty;
   - splash is a cold start ≤ 2 s, not a freeze.
3. **States:** `o1e` (without a profile) follows its own gold geometry, per [ADR-027](../../../android/adrs/ADR-027-golds-divergentes.md).

## Out of scope

- Compose ([A41](../../../android/plans/a41-splash-onboarding-aero.md)). New branding. Behavior or copy changes.

## Validation

As [D3 § Validation](../completed/d3-release1-home.md#validation), with 16 frames and the split-day rule.

## Results

Approved by the owner on 2026-10-04 ("Aprovo o plano docs/design/plans/d4-release1-splash-onboarding.md. Implemente o plano aprovado.").

### Discovery (before the first write)

Sources read: `SplashScreen.kt` and `SplashBoot` (`DietaBotTokens.kt`), `OnboardingChrome.kt`, `OnboardingScreens.kt`, `OnboardingUiState.kt`, `SlotScheduleControls.kt`, `SlotScheduleDraft.kt`, `SlotsOfDay.kt` (`SlotModes`), `SlotSuggestions.kt`, `core/designsystem/TimeWheelDialog.kt`, the [perfil-onboarding](../../../produto/specifications/perfil-onboarding.md) specification and the eight Stitch light golds. Class: `app` = exists in code or spec and is drawn; `gold-only` = dropped; `copy` = exact pt-BR text from the code; `spec` = missing from the gold, drawn from the spec.

**Common to O1–O4**

| Element | Class | Figma |
|---|---|---|
| Status bar, gesture bar | gold-only | dropped (system UI) |
| Step progress: continuous bar in O1/O2, 4 segments in O3/O4 | app | `Stepper/Progress` Step = 1–4 on every screen (one Aero control for the same 4-step progress) |
| O1 top bar: progress only, no back (`onBack = null`) | app | no back button |
| O2/O3 top bar: back + `DIETA BOT INTAKE` | copy | `IconButton/Glass` + `Icon/arrow-left` + label |
| O4 top bar: back + `Dieta Bot` + help (`Ajuda`) | copy | `IconButton/Glass` back, wordmark, `IconButton/Glass` + new `Icon/question` |
| Eyebrow `ONBOARDING n/4 • SECTION` | copy | `Label/Section` text, section in `accent/default` |
| Title + subtitle | copy | `Title` + `Body` muted |
| Section labels (`IDADE, ALTURA E PESO`, …) | copy | `Label/Section` muted |
| CTA pill with arrow (`Continuar`, `Concluir e começar`) | copy | `Button/Primary`; disabled = 38 % layer opacity, as the code (`DISABLED_ALPHA`) |
| Info note card with icon | copy | glass card + `Icon/info` / new `Icon/bell-ringing` + `Caption` muted |
| CTA bottom fade over the scroll | app | dropped as a layer: the frame shows the whole scroll, the CTA closes the column |
| No disclaimer on O1–O4 | spec | none drawn |

**Per gold**

| Gold | Element | Class | Figma |
|---|---|---|---|
| `splash` | Page background, soft glow | app | `Background/Page` + edge bubbles (ADR-030), no new brand |
| `splash` | Mark: the brand icon in a 120 box, symbol about half of it | app | `design/brand/icon.png` uploaded with `upload_assets`, 120 × 120 image fill (FIT) |
| `splash` | Wordmark `Dieta Bot`, 48 × 4 accent bar, copy `Estimativa nutricional, não substitui consulta médica ou nutricional.` | copy | `Hero/Number` text, `accent/default` pill, `Body` muted centred, 280 wide |
| `o1` | `Homem` / `Mulher` toggle, `Homem` selected | copy | `Choice/Segmented` Count=2 |
| `o1` | Fields `27 anos`, `180 cm`, `116 kg` | copy | `Field/Number` Size=Compact, no icon or helper |
| `o1` | `MODO DO TETO`: three radio rows with title and description | copy | three `Option/Card` (no badge), first selected |
| `o1` | `META DIÁRIA` field `2000` + `kcal` pill with a bolt | copy | `Field/Number` Size=Large, Unit `kcal`, `Icon/lightning` |
| `o1` | Caption hidden under the CTA in the gold | spec | drawn: sparkle + `Sugerido 2160 kcal com base no seu perfil. Você pode alterar quando quiser.` (TMB of 27 y, 180 cm, 116 kg, male; the meta was edited to 2000, so field and caption differ, as in the app) |
| `o1e` | Same top as `o1`, `Homem` selected, empty body fields `idade · anos`, `altura · cm`, `peso · kg` | copy | `Field/Number` State=Empty |
| `o1e` | Hint `Preencha idade, altura e peso para ver a meta sugerida.` with an info icon | copy | screen row |
| `o1e` | Modes and meta field disabled (38 %), meta `—`, no caption, CTA disabled | app | 38 % layer opacity; `Field/Number` State=Empty |
| `o1e` | Tighter geometry than `o1` | app | own spacing (ADR-027) |
| `o2` | Three cards `0% (Não compensar)` + `Padrão` badge (selected, default), `Porcentagem personalizada`, `100% (Compensação total)`, with the code bodies | copy | `Option/Card` |
| `o2` | Typed-% field (`50` + `% do treino`) inside the partial card when selected | app | `Option/Card` boolean `Show field` with a nested `Field/Number` Compact; off in `o2` (zero is selected) |
| `o2` | Note `Você poderá registrar ou ajustar os treinos a qualquer momento nas configurações.` | copy | info note |
| `o3` | `DIAS DA SEMANA`: `Todos os dias`, `Seg–Sex · Sáb–Dom`, `Cada dia`, first selected | copy | `Tabs/Weekday` Selected=Same |
| `o3` | `QUANTIDADE DE REFEIÇÕES` 2–6, `4` selected | copy | `Choice/Segmented` Count=5 |
| `o3` | Four slots `Café da manhã 07:30`, `Almoço 12:30`, `Lanche 16:00`, `Jantar 20:00`: band icon + name field, time field + clock, `Sugestões:` + two chips | copy | `Row/MealSlot` (Icon swap: `coffee`, `fork-knife`, `cookie`, `bowl-food`, `moon`), chips `Chip/Log` Neutral |
| `o3` | Slot toggle named in the plan | — | none in the app (a slot has only name and time), so `Row/MealSlot` has no toggle |
| `o3` | Note `Você poderá ajustar intervalos, adicionar refeições intermediárias ou desativar alertas a qualquer momento.` with a bell | copy | info note + `Icon/bell-ringing` |
| `o3t` | O3 blurred and dimmed behind the dialog | app | `o3` content + layer blur + `overlay/scrim` |
| `o3t` | Gold background without the `DIAS DA SEMANA` block | gold-only | drawn with it: the code shows the mode chips above the count |
| `o3t` | Dialog `Café da manhã`, `Horário da refeição`, wheels `05 06 07 08 09` : `28 29 30 31 32`, selection band, `Cancelar` / `OK` | copy | `Dialog/TimeWheel` |
| `o3s` | `Seg–Sex · Sáb–Dom` selected, group `Sáb e Dom`, `Etapa 2 de 2`, group segments | copy | `Tabs/Weekday` Selected=Split, screen row, `Progress/Bar` full |
| `o3s` | `Copiar de Seg a Sex` with a copy icon | copy | glass pill + new `Icon/copy` |
| `o3s` | Count `3`, slots `Café da manhã 09:30`, `Almoço 13:30`, `Jantar 20:30` | copy | `Choice/Segmented`, `Row/MealSlot` |
| `o4` | Split card `30% Proteína`, `40% Carbos`, `30% Gorduras` + three-part bar | copy | screen card, macro semantic colors |
| `o4` | Rows `Proteína 4 kcal/g • 30%` `150 g`, `Carboidrato 4 kcal/g • 40%` `200 g`, `Gordura 9 kcal/g • 30%` `67 g`, colour well, adjust button (`Ajustar {nome}`) | copy | `Card/MacroTarget` Macro = Protein / Carbs / Fat (nested `Field/Number` Compact, `IconButton/Glass` + `Icon/sliders-horizontal`) |
| `o4` | Note `Proporção balanceada: 30% Proteína · 40% Carboidratos · 30% Gorduras.` + `2000 KCAL TOTAL ESTIMADA` | copy | info note with an accent footer |
| `o4` | Help dialog text | app | not drawn (no gold id) |

Not drawn (no gold id): the mode-discard confirmation `Descartar os horários de {grupo}?`, the O4 help dialog, the O1 `Metas diárias` / `Meta por dia` variants of the meta field. They exist in the app and get a gold only through a product decision.

### Delivered in Figma (2026-10-04)

**`Componentes`**

- Ten new icon components in `Ícones` (Phosphor core 2.1.1, MIT, colour bound to `icon/primary`): `question`, `clock`, `copy`, `bell-ringing`, `coffee`, `cookie`, `fork-knife`, `bowl-food`, `moon` (regular) and `sparkle` (fill).
- New section `Onboarding`:
  - `Stepper/Progress`, Step = 1 | 2 | 3 | 4 (filled segments `accent/default` + the bound sheen gradient, empty `surface/2`, height `size/bar`);
  - `Choice/Segment` (Selected = true | false, prop Label, up to 3 lines) and `Choice/Segmented`, Count = 2 | 5: glass track with a glossy accent pill; the options are exposed `Choice/Segment` instances;
  - `Tabs/Weekday`, Selected = Same | Split | Each (`Todos os dias`, `Seg–Sex · Sáb–Dom`, `Cada dia`; wraps to two rows at 350 px, as the app);
  - `Row/MealSlot` (props Name, Time, Icon swap; two exposed `Chip/Log` Neutral suggestions; no toggle);
  - `Dialog/TimeWheel` (prop Title; `radius/sheet` glass, selection band, `Cancelar` pill + `Button/Primary` `OK` without icon);
  - `Card/MacroTarget`, Macro = Protein | Carbs | Fat (props Name, Detail; exposed `Field/Number` Compact; `IconButton/Glass` + `sliders-horizontal`);
  - `Card/Note` (props Text, Icon swap, Show footer, Footer), the info note of O2–O4.
- Extended D1/D3 components (instances stay linked):
  - `Field/Number`: State = Default | Focused | Empty and Size = Large | Compact (6 variants); booleans Show unit, Show icon, Show helper; no error variant (the app has none);
  - `Option/Card`: boolean Show field with a nested, exposed `Field/Number` Compact (`50`, `% do treino`); description now covers the O1 mode rows.
- Disabled controls in `o1e` (mode cards, meta field, CTA) are instances at 38 % layer opacity, the app's `DISABLED_ALPHA`; no new variant.
- Every section refitted to its content, 160 px apart; every component and set has a description; 0 variables with `ALL_SCOPES`.

**Variables.** None added.

**Brand.** `design/brand/icon.png` uploaded with `upload_assets` as the image fill (FIT) of the 120 × 120 `Mark` in both splash frames. `Branding` stays empty.

**`Release 1` → section `Splash e onboarding · D4`** (3840 × 2908; `Home · D3` and `Chat · D5` moved right to keep 160 px):

| Gold | Light | Dark |
|---|---|---|
| `splash` | `51:732` | `54:1233` |
| `o1` | `51:743` | `54:1244` |
| `o1e` | `51:890` | `54:1276` |
| `o2` | `53:801` | `54:1308` |
| `o3` | `53:911` | `54:1332` |
| `o3t` | `53:1049` | `54:1362` |
| `o3s` | `53:1220` | `54:1395` |
| `o4` | `53:1345` | `54:1434` |

- Frame names `<id> · <Stitch title without the theme suffix> · Light|Dark`, 390 px wide, Light row above Dark row, 80 px apart. Dark frames are clones with the Color mode set to Dark and no other change.
- Short screens (`splash`, `o2`, `o4`) keep an 844 px viewport; `o2` and `o4` push the CTA to its bottom with a spacer. Longer screens show the whole scroll.
- `o3t` is the `o3` content in a `Content` frame with an 8 px layer blur, an `overlay/scrim` layer and a `Dialog/TimeWheel` instance.
- The edge bubbles (ADR-030) sit in the 20 px side margins or above the top edge, never behind text.
- The pilot drafts `rascunho · O2 · Eat-back · Light|Dark` were deleted.

### Figma MCP budget

21 calls of the 110 budgeted: 1 `whoami`, 3 skill reads, 16 `use_figma` (6 read-only inspections and audits, 9 writes, 1 failed write rolled back by Figma and retried), 1 `upload_assets`. The review images come from the REST export (`tools/export-figma.mjs --dry-run`), which does not use the MCP.

### Validation

1. Discovery table: written above before the first Figma write.
2. Read-back of the 16 frames and of `Componentes`:
   - every DS element is an instance (`Stepper/Progress`, `Choice/Segmented`, `Tabs/Weekday`, `Field/Number`, `Option/Card`, `Row/MealSlot`, `Dialog/TimeWheel`, `Card/MacroTarget`, `Card/Note`, `Button/Primary`, `IconButton/Glass`, icons); no detached copy;
   - 0 solid fills or strokes without a variable or a paint style, and 0 text without a text style, in the frames and in the new components. The only unbound gradients are the gloss of the D1 `Button/Primary` (CTA) and the D1 `Option/Card` radio;
   - 0 overlapping in-flow siblings; the absolute layers are the edge bubbles, the `o3t` scrim and dialog and the time-wheel band.
3. Visual: the 16 frames exported at 2× and sent to the owner on 2026-10-04. Owner review in Figma pending.

<Export and closure follow after the owner's OK.>
