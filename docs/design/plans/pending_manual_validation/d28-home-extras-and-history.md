# Plan — D28 Release 2: Home day strip, past day and extras

- Status: Pendente aprovação manual
- Date: 09/10/2026
- Owning context: `design`
- Affected code: none in `apps/` or `server/`. Figma `Design` → `Release 2` → section "Home history", new components in `Componentes`. Repository: `docs/qa/figma/{dark,light}/{home1,homeH,homeE,chatGX}.png` (`home1` redrawn, `homeH`, `homeE` and `chatGX` new), the "Home history" node ids in `tools/export-figma.mjs`.
- Prerequisites: [D27](../completed/d27-conversational-onboarding.md) `Concluído` (one design plan at a time); [ADR-058](../../../produto/adrs/ADR-058-extras-and-history.md) accepted by this approval.
- Figma MCP budget: ≤ 90 calls (at most 120 a day, ADR-031 § 6).

Authorization, lifecycle, cancellation and deferral: `docs/sdd/README.md`. Approval: `Aprovo o plano docs/design/plans/d28-home-extras-and-history.md. Implemente o plano aprovado.`

## Objective

Draw the Home of [ADR-058](../../../produto/adrs/ADR-058-extras-and-history.md) in Aero, in both themes: the 30-day strip above the ring, the timeline ordered by time with an extra node, a past day, the Trocar sheet with the `Extra` entry and the extra receipt. Reference for the strip: the owner's sketch of 09/10/2026 (circles with the day number on a horizontal line, today at the right, selected and unselected states).

## Sources (feature parity, ADR-031 § 4)

| Gold | Reference (current gold or frame, layout only) | Behavior source |
|---|---|---|
| `home1` | current `home1` | ADR-058 decisions 2–3 ([home-timeline](../../../produto/specifications/home-timeline.md) rules 1–13 plus the strip and the extra node `Extra · 15:40`) |
| `homeH` | `home1` | ADR-058 decision 4 (a past day selected: its ring, macros, workout, closure card, timeline; cards without gestures; FAB and Config visible) |
| `homeE` | `home0` | ADR-058 decision 3 (day 1: strip with a single circle, empty timeline) |
| `chatGX` | `chatG` | ADR-058 decision 1 (receipt `Registrado como extra · {hora}` with the receipt actions) and the Trocar sheet with the entry `Extra · fora das refeições` at the end of the slot list |

Code: `apps/android/app/src/main/java/app/fibrai/android/feature/home/` and `feature/chat/` (Trocar sheet, receipts).

## Scope

1. **Discovery (read only):**
   - per gold, list every visible element and classify it: `app` (exists in code or spec), `gold-only` (dropped) or `copy` (exact pt-BR text from ADR-058 or the code);
   - the table goes into Results before any write;
   - an element in the ADR but missing from the sketch is drawn from the ADR.
2. **Components** (in `Componentes`, readability rule):
   - `Home/DayStrip`: horizontal row of `Home/DayCircle` with a month label above the first circle of each month; `DayCircle` variants `today-selected`, `past-selected`, `past`, `no-record` (dimmed ring), all with WCAG AA contrast of the number on the circle in both themes;
   - `Timeline/Node` gains the `extra` variant (the regular node with the clock glyph) and the extra card (`Extra · {HH:mm}`, text, numbers in the macro colours);
   - `Sheet/Trocar` gains the `Extra · fora das refeições` row.

   Reuse the existing components and extend them with properties or variants where a state needs it.
3. **Screens** (section "Home history" on `Release 2`):
   - Light row: `home1`, `homeH`, `homeE`, `chatGX`, in product order, 390 px wide;
   - Dark row: clones with the `Dark` mode, no other change;
   - frame names `<id> · <screen title> · Light|Dark`; spacing per ADR-031 § 3;
   - the Release 1 `home1` frame is replaced in place (same node ids where possible); `homeW`, `homeC`, `homeK`, `homeP` and `homeX` are not redrawn: the strip is the same component instance and A72 validates them by the partial rule.
4. **Owner review** (Figma review gate):
   - one screenshot per frame is sent to the owner;
   - the plan goes to `pending_manual_validation/` until the owner's OK in Figma;
   - fixes follow, with one screenshot per changed frame.
5. **Export** after the OK:
   - fill the "Home history" ids in `tools/export-figma.mjs`;
   - `node tools/export-figma.mjs --only home1 homeH homeE chatGX`;
   - add `homeH`, `homeE` and `chatGX` to the inventory of `docs/qa/README.md`;
   - `node tools/check-figma.mjs`.

## Out of scope

- Compose (A72). Behavior or copy changes beyond ADR-058. The onboarding (D27). Other Chat states.

## Validation

1. The discovery table exists in Results before the first write.
2. A read-back confirms instances (not detached frames) for every DS component, no raw hex fills outside the variables, and no overlapping nodes; the contrast of the day number on every `DayCircle` variant is measured and written in Results.
3. Visual: one screenshot per frame; manual owner OK.
4. `check-figma` and `check-docs` pass.

## Results

Order: the batch message of 09/10/2026 runs D28 right after D27's frames, with D27 in `Pendente aprovação manual` (owner's order; one design plan at a time is kept: D27's Figma work ended before D28's started).

### Discovery (09/10/2026, before any write)

Read only: Figma `Release 1` frames `home1`, `home0`, `homeC`, `homeP`, `chatG`, `chatT` (structure and review PNGs), the components `Card/Meal`, `Timeline/Node`, `Sheet/SlotList`, `Row/SlotPick`, `Chat/Receipt`, `Card/Closure`, [home-timeline](../../../produto/specifications/home-timeline.md) rules 1–13, [ADR-058](../../../produto/adrs/ADR-058-extras-and-history.md).

Sample data (synthetic, ADR-033): `home1` is day 15 (first day 19/09), 3 de outubro; Café 520 kcal · 28P · 52C · 22G, Almoço 620 kcal · 48P · 42C · 18G, extra at 15:40 `Energético, 1 lata (350 ml)` 160 kcal · 0P · 40C · 0G, Lanche skipped, Jantar empty; totals unchanged from the current gold (1300 kcal, P 76, C 134, G 40, meta 2175 with 350 kcal of workout), so the ring and the macro bars keep their geometry. `homeH` is 1 de outubro (day 13): Café 520, Almoço 780, Lanche skipped, Jantar 610 kcal · 42P · 60C · 20G; 1910 de 2175 kcal, P 118, C 194, G 60, workout 350 kcal.

| Gold | Element | Class | Source / copy |
|---|---|---|---|
| `home1` | Header `DIA 15` · `3 de outubro`, Config, ring, macros, workout line, timeline header, disclaimer, FAB | app (unchanged) | rules 1–3, 6, 7, 10, 11 |
| | Day strip above the ring: 9 circles 25–30 set, 1–3 out, today (3) at the right end and selected, `Outubro` above the 1, the leftmost circle cut by the edge (scroll affordance), 28 without record (dimmed ring) | app (new) | ADR-058 decision 3; owner's sketch of 09/10/2026 |
| | Timeline by time: Café 07:30, Almoço 12:30, extra node `Extra · 15:40` (clock glyph), Lanche 16:00 skipped, Jantar 20:00 empty | app (new) | ADR-058 decision 2 |
| | Extra card: `Extra · 15:40`, `Energético, 1 lata (350 ml)`, `160 kcal · 0P · 40C · 0G` in the macro colours, no number | copy | ADR-058 decisions 1–2 |
| | `Outros` block | not drawn (no removed or foreign slot in the sample) | ADR-021 rule 7 |
| `homeH` | Strip with 1 out `past-selected` and 3 out `today` (unselected, accent ring); header `DIA 13` · `1 de outubro`; that day's ring (1910), macros, workout line titled `Treino do dia` without the chevron; closure card of the day; timeline with no gesture prompts; FAB and Config visible | app (new) | ADR-058 decision 4; rule 12 (closure card) |
| `homeE` | `home0` plus the strip with one circle (25 set, `Setembro` above it, `today-selected`) at the right end; empty timeline | app (new) | ADR-058 decision 3 |
| `chatGX` | Chat header, date chip `Hoje, 3 de outubro`, user `Tomei um energético agora, uma lata de 350 ml`, Tali estimate (~160 kcal, 0 g P, 40 g C, 0 g G), receipt `Registrado como extra · 15:40` `+160 kcal` with Excluir, Trocar refeição, Editar (the receipt actions of `chatG`), composer | app / copy | ADR-058 decision 1; `chatG` layout |
| | Trocar sheet with the entry `Extra` · `fora das refeições` (clock) after the slots | app (new), drawn in the component | ADR-058 decision 1 |

Decisions of this discovery:

- `chatGX` is one screen: it shows the receipt (the state the id derives from, `chatG`). The Trocar entry is a new row of `Sheet/SlotList` (`Mode=Record`) behind a `Show extra` boolean (default off, so `chatT` and `chatTI` do not change), with a specimen instance in the `Componentes` section; a gold of the open sheet would be `chatT` redrawn, outside this plan. Flagged for the owner review.
- `home1` is drawn in the `Release 2` section `Home history` (Release 1 stays frozen as history, ADR-031 § 2, precedent D23 `chatRK`); the export map moves `home1` to the new ids after the OK.
- The extra card is its own component `Card/Extra` (title with the time, text, macro line), not a `Card/Meal` variant: the meal card has the time on the right and the accent chip.
- `Home/DayCircle` gets a fifth state, `today` (today not selected, on `homeH`), next to the four of the plan.

### Build (09/10/2026)

- **Components** (`Componentes`, new section `Home history · D28`, `213:274`, below `Onboarding v2 · D27`):
  - `Home/DayCircle` (set `213:297`): `State=today-selected` (`213:277`), `today` (`213:281`), `past-selected` (`213:285`), `past` (`213:289`), `no-record` (`213:293`); properties `Day#213:0`, `Month#213:6` (three-letter month, uppercased by Label/Section, centred above the circle), `Show month#213:12`. The month label is absolute, so a circle with or without it keeps the row aligned.
  - `Home/DayStrip` (`213:298`): 9 `Home/DayCircle` instances, 10 px apart, packed to the right and clipped at 350 px (the leftmost circle shows cut: the strip scrolls).
  - `Card/Extra` (`213:335`): `Extra · {HH:mm}` (Body/Strong), the text (Body), `160 kcal · 0P · 40C · 0G` with P, C, G in `macro/*`; properties `Title#213:18`, `Description#213:19`, `Kcal#213:20`, `P#213:21`, `C#213:22`, `G#213:23`.
  - `Timeline/Node` gains `State=Extra` (`213:346`): the `Done` ring with the clock glyph.
  - `Sheet/SlotList` gains `Show extra#213:24` (default off) and the row `Slot · Extra` (`213:349`, `Row/SlotPick` with `Extra` · `fora das refeições` and the clock) at the end of `Mode=Record`; specimen instance with it on: `213:412`. `chatT` and `chatTI` do not change.
- **Frames** (`Release 2`, new section `Home history · D28`, `214:1381`, at y 12640 under `Onboarding v2 · D27`, 1960 × 3952): Light row at y 208, Dark row at y 2080 (clones in mode `Dark`, 27 text paint segments rebound with the Dark resolved value). `home1`, `homeE` and `chatGX` start from clones of the Release 1 `home1`, `home0` and `chatG`; the Release 1 frames are untouched.

| Gold | Light | Dark | Size |
|---|---|---|---|
| `home1` · Home no dia com extra | `214:1384` | `215:1814` | 390 × 1640 |
| `homeH` · Home de um dia anterior | `214:1567` | `215:1979` | 390 × 1792 |
| `homeE` · Home vazia com faixa - Day 1 | `214:1747` | `215:2142` | 390 × 1406 |
| `chatGX` · Chat com registro de extra | `214:1832` | `215:2262` | 390 × 902 |

Review PNGs without MCP calls: `node tools/export-figma.mjs --only <node id> --dry-run --out <dir>`.

Fixes during the build (before the review): `homeH` macro values restored to the two-tone style of `home1` and the bars resized; the month label moved from the full name (it overflowed the right edge on `homeE`) to the centred abbreviation; the `today` number moved from `accent/default` (4.47:1 in Light) to `text/primary` with a 2 px accent ring.

### Validation (before the owner review)

1. Discovery table above, written before the first write.
2. Read-back (`use_figma`) of the 8 frames: instances for every DS component (`home1` 39, `homeH` 38, `homeE` 26, `chatGX` 22, the same in Dark); no frame named after a DS component left detached; zero visible solid paints without a variable or style; no overlapping children in the section and no page node overlapping it; Light frames in mode Light, Dark frames in mode Dark.
3. Contrast of the day number on every `DayCircle` state, measured on the exported pixels (circle fill next to the number, worst of six samples):

| State | Light | Dark |
|---|---|---|
| `today-selected` (`accent/on` on `accent/default`) | 5.77:1 | 8.24:1 |
| `today` (`text/primary` on glass) | 11.35:1 | 13.29:1 |
| `past-selected` (`text/primary` on `surface/selected`) | 5.21:1 | 6.66:1 |
| `past` (`text/primary` on glass) | 11.35:1 | 13.32:1 |
| `no-record` (`text/dim` on glass) | 6.02:1 | 5.24:1 |

   State cue of `today` (the `accent/default` ring on glass, WCAG 1.4.11): 4.47:1 Light, 7.29:1 Dark.
4. Visual: one review PNG per frame (8) plus the components section, from the dry-run export, sent to the owner on 09/10/2026.

Figma MCP budget: 7 of 90 calls (1 read, 6 writes, one of them a script that failed on a syntax error and was rolled back whole). Day total with D27: 19 of 120. Owner review pending.
