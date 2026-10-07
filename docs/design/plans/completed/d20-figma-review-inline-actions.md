# Plan — D20 Figma review: stale frames, Chat actions in the thread, copying messages

- Status: Concluído (07/10/2026, owner OK, golds exported)
- Date: 07/10/2026
- Owning context: `design`
- Affected code: none in `apps/` or `server/`. Figma `Design` → `Release 1` → sections "Splash e onboarding", "Chat", "Chat · D6" and "Config e push" (only the frames listed below); components in `Componentes` only if a variant is missing. Repository: `docs/qa/figma/{dark,light}/<ids>.png` of the changed frames, `tools/export-figma.mjs` only if a node id changes, and `tools/diff-gold.mjs` rules that exist only because of a stale frame.
- Prerequisites: D12, D16, D17, D18 and D19 `Concluído` ([history](./)); A60 merged (the app the frames are compared with, 0.0.18-dev).
- Figma MCP budget: ≤ 110 calls (at most 120 a day, ADR-031 § 6).

One review plan for several flows by owner decision (07/10/2026): the frames are small corrections of existing golds plus one layout change (the Chat actions) and the two copy states, and the client plan [A61](../../../android/plans/pending_manual_validation/a61-chat-copy-scroll-capture-inline-actions.md) waits for all of them. Approving this plan accepts [ADR-048](../../../produto/adrs/ADR-048-chat-actions-in-thread-copy-scroll-capture.md). Authorization, lifecycle, cancellation and deferral: `docs/sdd/README.md`. Approval: `Aprovo o plano docs/design/plans/d20-figma-review-inline-actions.md. Implemente o plano aprovado.`

## Objective

Bring every gold that no longer matches the app or the live specifications back in line, in both themes; redraw the Chat frames whose actions sit pinned above the composer so the actions sit in the thread under their message (ADR-048 decision 1); and draw the two new states of copying messages the WhatsApp way (ADR-048 decision 2): `chatCP` (messages selected, selection bar with **Copiar**) and `chatCC` (the app's own copy confirmation, Android 12 and earlier). After this plan, the A60 visual QA exceptions (`tools/diff-gold.mjs` gold conflicts added for stale frames, the `GoldTest` report-only `cfgS`) are no longer needed.

## Sources (feature parity, ADR-031 § 4)

| Gold | What is stale or changes | Behavior source |
|---|---|---|
| `chatQ`, `chatE`, `chatR`, `chatRB`, `chatRK`, `chatRL` | Actions (Forçar estimativa; Registrar; Registrar assim + Reservar; Pode passar · Ajustar para caber) drawn in the slot pinned above the composer | ADR-048 decision 1; ADR-026, ADR-028, ADR-039, ADR-046 for what each action is |
| `chatRB` | D12 drew the plan as plain lines; the app renders the D17 blocks (bullets with the 17 dp marker column, numbered steps with the 21 dp column, 12 dp between blocks, P/C/G colours) | ADR-045, [Chat](../../../produto/specifications/chat.md) rule 2 |
| `chatR`, `chatE` (Dark) | The Dark clones paint the reply's first paragraph in the bubble colour, so it is invisible | Light frames of the same ids |
| `o1`, `o1e`, `o2`, `o3`, `o3s`, `o3t`, `o4` | Progress reads `n/4`; onboarding has 5 steps since D16 (`n/5`); `o4` CTA reads Concluir e começar, the app reads **Continuar** (Concluir e começar moved to `o5`) | [perfil-onboarding](../../../produto/specifications/perfil-onboarding.md) rules 4 and 4a, ADR-044 |
| `cfgS` | Predates the `Tom da Tali` row of the targets card | [memoria-push](../../../produto/specifications/memoria-push.md) rule 12 |
| `chatCP` (new) | none: long press on a bubble selects it; the header becomes the selection bar (✕, count, **Copiar**) | ADR-048 decision 2 |
| `chatCC` (new) | none: after **Copiar**, `Mensagem copiada` / `{n} mensagens copiadas` for about 2 s (Android 12 and earlier; 13+ uses the system overlay) | ADR-048 decision 2 |
| `cfgT` (Light) | Config behind the sheet is sharp under the scrim; the app's shared Sheet/Bottom blurs the screen behind it, as the `chatT` frame draws | `chatT` frame; the shared `AeroSheet` |

Code: `apps/android/app/src/main/java/app/fibrai/android/feature/{chat,onboarding,config}`; latest app captures in `docs/qa/android/current/{dark,light}/` (A60 run, 0.0.18-dev).

## Scope

1. **Discovery (read only):**
   - **Sweep:** every gold of the inventory in [docs/qa/README.md](../../../qa/README.md) against its latest app capture and the live specifications; each difference classified `stale gold` (the app follows the spec; the frame changes here), `app gap` (the frame follows the spec; the app changes in A61), `ignored` (clock, status bar, sample text, long-thread crop) or `known` (the table above). A stale gold outside the table is added to this plan's frames only if it is a correction of the same kind (copy, counter, colour, a row the spec added); anything that changes a layout or a behavior is listed for the owner and stays out.
   - **Per changed frame:** every visible element classified `app`, `gold-only` or `copy`, as in the template.
   - Both tables go into Results before any write.
2. **Chat actions in the thread** (`chatQ`, `chatE`, `chatR`, `chatRB`, `chatRK`, `chatRL`):
   - the action stack moves from the composer slot into the thread, under its message: after the bubble, or after its budget lines (`chatRB`) or its `Reservado para o Jantar` mark (`chatRL`); gaps from the spacing tokens (the 8–12 dp of the existing under-bubble elements to the first action, 10 dp between stacked actions as today, then the thread's 16 dp to anything below);
   - components unchanged: `Chat/ActionBar` (full width of the thread, 20 dp margins), the choice pills, the `Forçar estimativa` bar;
   - the composer sits alone at the bottom with the frame's existing bottom margin; when the thread is short the stack sits right under the message, not above the composer;
   - long-thread frames keep showing the whole thread (the export height follows the content).
3. **Copying messages** (`chatCP`, `chatCC`, section "Chat", after `chatX`):
   - **Component `Chat/SelectionBar`** (in `Componentes`): same height, margins and glass as `Chat/Header`, in its place while selecting; left the round back-style button with `X` (Phosphor, as the other icon buttons), then the count (`1`, `2`…) in the header title style; right one icon button `Copy` with the accessible label **Copiar**. Light and Dark from the variables; no new colour.
   - **Selected message:** the whole row of the bubble (thread width) with a highlight fill from the accent family at low emphasis, behind the bubble; the bubble itself unchanged; works on the user bubble (tinted glass) and on a Tali bubble with an estimate card.
   - **`chatCP`:** a thread with a user message and a formatted Tali reply, both selected (count `2`), plus one unselected receipt to show it is never selected; composer unchanged at the bottom.
   - **`chatCC`:** the thread back to normal (header restored) and the confirmation `Mensagem copiada` as a small glass pill (`Chip/Log` style or the closest DS pill, `Check` icon) centred above the composer; the plural `2 mensagens copiadas` checked as a long-label variant.
   - Discovery for both: the WhatsApp behaviour is the reference for the interaction only; the look is Aero only (no WhatsApp colours, icons or layout copied).
4. **Corrections** (no layout change): `chatRB` plan text in D17 blocks with the macro colours; `chatR`, `chatE` Dark first paragraph in `text/primary`; `o1`–`o4` counter `n/5` and the `o4` CTA **Continuar** (same component and arrow as the other steps); `cfgS` with the `Tom da Tali` · `Seco` row after the macros, as `cfg`; `cfgT` Light with the page behind the sheet blurred as `chatT` (Dark only if the sweep finds the same).
5. **Owner review** (Figma review gate): one screenshot per changed frame; the plan goes to `pending_manual_validation/` until the owner's OK in Figma; fixes follow with one screenshot per changed frame.
6. **Export** after the OK:
   - `chatCP` and `chatCC` node ids added to `tools/export-figma.mjs` and to the inventory of [docs/qa/README.md](../../../qa/README.md) (and the Chat row of the golds table in this folder's README);
   - `node tools/export-figma.mjs --only <changed and new ids>`; untouched golds keep their bytes; other node ids change only if a frame was replaced;
   - `node tools/check-figma.mjs`;
   - `tools/diff-gold.mjs`: the gold-conflict entries that existed only because of a stale frame (`chatRB` tail start, `themeConflicts` of `chatE` Dark and `cfgT` Light, and the `cfgT` sheet region) are removed; long-thread rules stay;
   - Results list every exported id and the `app gap` items handed to A61.

## Out of scope

- Compose (A61). Behavior or copy changes beyond ADR-048 and the live specifications. Other actions in the selection bar (reply, delete, forward: the bar has **Copiar** only). A frame for the scrolling screenshot (system UI) or for the Android 13+ clipboard overlay (system UI). Frames not listed, unless the sweep adds a correction of the same kind. `land`, `landM`, `priv` (site).

## Validation

1. The sweep and the discovery tables exist in Results before the first write.
2. Read-back: instances for every DS component, no raw hex outside the variables, no overlapping nodes; the moved action stacks are children of the thread, not of the footer; `Chat/SelectionBar` is a component with instances in both themes.
3. Visual: one screenshot per changed frame; manual owner OK.
4. `node tools/check-figma.mjs` and `node tools/check-docs.mjs` pass.

## Results

### Discovery (before the first write)

Figma budget: whoami, 2 skill reads and 4 read-only `use_figma` inspections before these tables (7 of 110).

**Sweep** (2026-10-07): every inventory gold against its latest capture in `docs/qa/android/current/{dark,light}/` with `node tools/diff-gold.mjs`, plus a side-by-side read of each capture that failed or is report-only. 48 golds per theme.

| Golds | Finding | Class |
|---|---|---|
| `home0`, `home1`, `homeX`, `homeC`, `homeK`, `homeP`, `splash`, `chat0`, `chatL`, `chatT`, `chatP`, `chatX`, `chatA`, `chatS`, `chatSK`, `chatSD`, `cfg`, `cfgR`, `wipe` | pass (≤ 1.7 %, or their region and tail gates pass) | no change |
| `chatG`, `chatD`, `chatM`, `chatU` | report-only full screen, header and thread tail pass | ignored (long thread) |
| `homeW`, `push` | report-only (A40 page geometry; system notification) | ignored |
| `chatF` (Light) | region 7.5 %: the capture is from 2026-10-04 and still shows `Chat Dieta Bot` and `Dieta Bot AI` (before A49) | ignored (stale capture, the gold follows the spec) |
| `chatI`, `chatIC`, `chatTI` | fail: captures from 2026-10-05, before A49 (same old header and label) | ignored (stale captures) |
| `chatQ`, `chatE`, `chatR`, `chatRB`, `chatRK`, `chatRL` | the action stack sits pinned above the composer in gold and app | known (ADR-048 decision 1) |
| `chatRB` | plan as plain lines; the app renders the D17 blocks | known |
| `chatR`, `chatE` (Dark) | first paragraph of the reply resolves to the Light `text/primary` (`#0b2a47`) on the dark bubble: an explicit Light mode left on the text nodes | known |
| `o1`, `o1e`, `o2`, `o3`, `o3s`, `o3t`, `o4` | pass the pixel gate; gold **and app** read `ONBOARDING n/4` with a 4-segment stepper while `o5` reads `5/5` with 5 segments; `o4` gold CTA `Concluir e começar`, app **Continuar** | known; the counter is also an `app gap` (below) |
| `cfgS` | Light ink 1.27 fails: no `Tom da Tali` row (`cfg` has it) | known |
| `cfgT` | Light report-only: page sharp behind the sheet; Dark passes (0.32 %) | known; Dark unchanged |
| `land`, `landM`, `priv` | site | out of scope |

No stale gold outside the table of Sources. `app gap` items handed to A61: **(1)** O1–O4 eyebrow `ONBOARDING n/5` and the 5-segment stepper (`OnboardingAeroScreens.kt` passes `Onboarding n/4` and `count = 4`); the app already shows **Continuar** on O4. Nothing else.

**Per changed frame** (`app` = in code or in the live specs; `gold-only` = dropped; `copy` = exact pt-BR):

| Frame | Element | Class | Source |
|---|---|---|---|
| `chatQ` | **Forçar estimativa** (`Chat/ActionBar`, fast-forward icon) under the latest question, in the thread | app (A61 moves it) | ADR-026, ADR-048 d1; `ChatScreen.kt` `ForceBar` |
| `chatE` | **Registrar** under the estimate | app (A61) | ADR-028; `RegisterBar` |
| `chatR` | **Registrar assim**, then **Reservar para o Jantar** (calendar-check), stacked, 10 dp apart | app (A61) | ADR-039, ADR-046; `PlanBar` |
| `chatRB` | **Pode passar** · **Ajustar para caber** side by side under the budget lines | app (A61) | ADR-039 rule 16; `AeroChoiceBar` |
| `chatRB` | plan text in D17 blocks: bullets (17 dp marker), steps (21 dp), 12 dp between blocks, P/C/G colours | app | ADR-045; `AeroReply.kt` |
| `chatRK` | **Registrar assim** under the recipe | app (A61) | `PlanBar` |
| `chatRL` | **Registrar assim** under `Reservado para o Jantar` | app (A61) | ADR-046; `PlanBar` without Reservar |
| all six | header, date chip, bubbles, cards, composer unchanged; composer alone in the footer | app | `ChatScreen.kt` |
| `chatCP` | `Chat/SelectionBar` in place of `Chat/Header`: `X` icon button, count `2` (header title style), `Copy` icon button labelled **Copiar** | app (A61) | ADR-048 d2 |
| `chatCP` | highlighted rows (thread width) of the selected user bubble and Tali reply with its estimate card | app (A61) | ADR-048 d2 |
| `chatCP` | a receipt (`Registrado em Almoço`) and its actions, not highlighted | app | ADR-048 d2 (receipts never selected); thread of `chatG` |
| `chatCC` | header back; pill `Mensagem copiada` with a check icon centred above the composer; long label `2 mensagens copiadas` | app (A61) + copy | ADR-048 d2 |
| `o1`–`o4`, `o1e`, `o3s`, `o3t` | eyebrow `ONBOARDING n/5`, 5-segment stepper with n filled | app gap (A61) | AGENTS (5 screens), perfil-onboarding rule 4a |
| `o4` | CTA **Continuar** with the arrow | app | perfil-onboarding rule 4 |
| `cfgS` | row `Tom da Tali` · `Seco` after the macros | app | memoria-push rule 12; `cfg` |
| `cfgT` (Light) | page behind the sheet blurred under the scrim | app | shared `AeroSheet`; `chatT` |

### Build (2026-10-07)

**Chat actions in the thread** (`chatQ`, `chatE`, `chatR`, `chatRB`, `chatRK`, `chatRL`, Light and Dark, same node ids): in each frame the last thread element (the question or the `Bot` group) moved into a new `Answer` frame (vertical, 12 dp), followed by an `Actions` frame (vertical, 10 dp, thread width) holding the action instances that were in the `Footer`: `Chat/ActionBar` Forçar estimativa, Registrar, Registrar assim, Reservar para o Jantar, and the `Choice` row (Pode passar · Ajustar para caber). The `Footer` keeps only `Chat/Composer`. Fixed 844 px frames keep their height (the `Spacer` absorbs the difference); long frames grow with the content: `chatR` 928 → 930, `chatRB` 904 → 1002 (with the blocks below), `chatRK` 1065 → 1067, `chatRL` 896 → 898.

**Corrections:**

- `chatRB` (both themes): the plan text is now D17 blocks: `Macarrão com atum ao sugo:`, four `List/Bullet`, three `List/Step` and `Total: ~620 kcal · 42P · 70C · 18G` with `620 kcal` in `Body/Strong` and P/C/G in `macro/*`, as in `chatR`.
- `chatR`, `chatE` (Dark): the cause was not an explicit mode. The reply's first paragraph is a plain text node whose bound `text/primary` paint kept the Light value as its placeholder colour, and the renderer drew the placeholder. Every text range bound to a colour variable outside instances in the 12 Chat frames now carries its frame mode's resolved value (2 ranges changed).
- `o1`, `o1e`, `o2`, `o3`, `o3s`, `o3t`, `o4` (both themes): eyebrow `ONBOARDING n/5` and frame names `Onboarding n/5`. `Stepper/Progress` `Step=1`–`Step=4` now have 5 segments (65 px, filled up to n), so every onboarding stepper shows five. `o4` CTA **Continuar** (`Button/Primary` `Label`).
- `cfgS` (both themes): divider and `Row · Tom da Tali` (`Seco`) after the macros, cloned from `cfg` of the same theme; frame 845 → 902 px (hug).
- `cfgT`: the `Config` page under the scrim gets the `chatT` layer blur (8). Applied to Dark as well: the Dark frame had the same sharp page and only passed the pixel gate because of its low contrast; Dark stays a clone of Light.

**Copying messages:**

- Components (`Componentes` → section "Chat", under the existing entries; the section grew 910 px and the sections below moved down by the same amount, which also clears an overlap that existed before): `Chat/SelectionBar` (`171:7016`): 350 × 44, `IconButton/Glass` `Close` (Icon/x), `Count` (TEXT property, `Body/Strong`, `text/primary`, fills the row), `IconButton/Glass` `Copiar` (Icon/copy). `Chat/CopyToast` (`171:7038`): the `Chip/Date` glass pill (Surface/Glass, border, Glass effect), `Icon/check (bold)` 16 in `accent/default`, `Label` (TEXT property, `Caption/Strong`, `text/primary`), 165 × 36. Both with descriptions.
- Frames (section "Chat", after `chatX`; the section widened to 4310 px):

| Gold | Light | Dark | Size |
|---|---|---|---|
| `chatCP` · Chat com mensagens selecionadas | `173:5878` (x 3370) | `173:6008` | 390 × 902 |
| `chatCC` · Chat com mensagem copiada | `173:5960` (x 3840) | `173:6031` | 390 × 948 |

  Both start from `chatG` (user message, Tali reply with the estimate card, receipt `Registrado em Café da manhã` with Excluir · Trocar refeição · Editar). `chatCP`: `Chat/SelectionBar` with `2` in place of `Chat/Header`; behind the user row and the Tali row a `Selected` rectangle 390 px wide (the screen width, 6 px over and under the row) in `accent/default` at 15 % layer opacity (no new token); the receipt and its actions are not highlighted. `chatCC`: header back, `Chat/CopyToast` `Mensagem copiada` centred above the composer, 10 px over it. Long label: `2 mensagens copiadas` measures 185 px, on one line. Dark frames are clones with the Color mode Dark.

### Validation (before the owner review)

1. Discovery tables above, written before the first write.
2. Read-back (`use_figma`) of the 34 changed frames: instances everywhere a DS component exists (16–36 per frame), zero visible solid paints without a variable or style, Dark frames in mode Dark and Light frames in Light (the onboarding and `cfgS` Light frames use the collection default, Light, as before); in the six action frames and in `chatCP`/`chatCC` the `Actions` stacks are inside `Thread` and the `Footer` holds only the composer (plus the toast in `chatCC`); `Chat/SelectionBar` and `Chat/CopyToast` are components with instances in both themes.
3. Review images via `node tools/export-figma.mjs --only <node ids> --dry-run` (no MCP calls). Fixed during the build: `620 kcal` was regular in the first pass of `chatRB`.

Figma MCP budget: 18 of 110 calls (whoami, 2 skill reads, 15 `use_figma`) at the first review.

### Owner review

**Round 1 (07/10/2026).** The owner found the `chatCP` highlight weak in both themes (low contrast, no sense of "selected") and asked for WCAG: darker in Light, lighter in Dark. Measured on the exported frames against the real page gradient behind the rows: WCAG 1.4.11 (3:1 for a state) cannot be met by the band alone in either theme, because a band 3:1 away from the page would push the `Tali` label (`text/primary`) under 4.5:1 (Light needs band luminance ≤ 0.12 for 3:1 and ≥ 0.27 for the label; Dark ≥ 0.17 and ≤ 0.16). Fix, with the owner's direction and two new variables (mirrored into `docs/design/tokens.json`, `docs/tokens.md` with `node tools/gen-tokens.mjs`, and `web/public/css/tokens.css` with `npm --prefix web run tokens`; the app generates `AeroColors.surfaceSelected` and `borderSelected` from the JSON):

- `surface/selected` (`#0c66bc4d` Light, `#4fc3f74d` Dark): the band behind the selected row, twice the earlier emphasis, darker than the page in Light and lighter in Dark.
- `border/selected` (`#0a4f91` Light, `#4fc3f7` Dark): a 2 px inside ring on each selected bubble, the 3:1 state cue. The bubble keeps its size (stroke in layout, padding minus 1).
- An opaque `bg/page` backing under each selected bubble, so the band does not show through the glass. This is also how the app renders: the Haze glass blurs the page background, not the row band.

| Measure (exported pixels) | Light | Dark | Needs |
|---|---|---|---|
| Band vs the unselected page beside it | 1.34–1.40 | 1.92–1.95 | (cue below) |
| Ring vs the bubble / vs the band | 7.58 / 3.50 | 8.26 / 4.98 | 3:1 |
| `Tali` label on the band | 5.90 | 7.41 | 4.5:1 |
| Time `20:15` in the selected Tali bubble | 5.10 (4.63 unselected) | 5.93 (5.85) | 4.5:1 |
| Time in the selected user bubble | 4.15 (3.69 unselected) | 4.93 (4.72) | 4.5:1 |

The Light user-bubble time (`text/dim` on the tinted bubble) is under 4.5:1 with or without selection, in every Chat gold: a token issue outside this plan, reported to the owner. Figma MCP: 5 more calls (23 of 110).

**OK (07/10/2026).** The owner approved every frame in Figma ("Aprovado no Figma"), including the selection with `surface/selected`, `border/selected` and the `bg/page` backing.

### Export (07/10/2026)

- `chatCP` (Dark `173:6008`, Light `173:5878`) and `chatCC` (Dark `173:6031`, Light `173:5960`) mapped in `tools/export-figma.mjs`; both added to the inventory of [docs/qa/README.md](../../../qa/README.md) and to the Chat core row of the golds table in [this folder's README](../README.md). No other node id changed.
- `node tools/export-figma.mjs --only chatQ,chatE,chatR,chatRB,chatRK,chatRL,chatCP,chatCC,o1,o1e,o2,o3,o3s,o3t,o4,cfgS,cfgT`: 34 PNGs. The noise filter restored Dark `o1`, `o1e` and `o3t` (0.045–0.049 % of pixels changed, under its 0.05 % threshold: the counter digit and the low-contrast fifth segment); the fresh exports were kept, since the change is real. Sizes: `chatRB` 780 × 2004, `chatCP` 780 × 1804, `chatCC` 780 × 1896, `cfgS` 780 × 1804; `chatR`, `chatRK` and `chatRL` are 4 px taller than before (2 dp), the others keep their size.
- `node tools/check-figma.mjs`: 100 golds verified (50 dark + 50 light).
- `tools/diff-gold.mjs`: removed the stale-frame exceptions (the `chatRB` tail start, `themeConflicts` of `chatE` Dark and `cfgT` Light, the `cfgT` sheet region); the `chatQ` and `chatE` bottom zones are the composer alone (268 px); `cfgT` gets the bottom-anchored sheet zone of `chatT` (1042 px) and passes in both themes against the A60 captures; `chatCP` and `chatCC` join the long-thread rules (header box and tail). Until A61 recaptures, the current captures of the changed golds no longer match, by design.
- Handed to [A61](../../../android/plans/pending_manual_validation/a61-chat-copy-scroll-capture-inline-actions.md): the `app gap` O1–O4 counter `n/5` with five segments; the selection look (`surface/selected` band, `border/selected` 2 px ring, opaque `bg/page` backing under the selected bubble, since the Haze glass blurs the page and not the band); the `GoldTest` report-only exception of `cfgS` (in `apps/`, outside this plan's boundary) can go once the row is rendered.
- Outside this plan: the site's phone screens (`web/public/img/screens/*/chatE.webp`) follow `chatE` and were already stale before D20 (`npm --prefix web run check`); the Light `text/dim` contrast of the user-bubble time (Owner review, round 1).

Figma MCP budget: 23 of 110 calls (whoami, 2 skill reads, 20 `use_figma`).
