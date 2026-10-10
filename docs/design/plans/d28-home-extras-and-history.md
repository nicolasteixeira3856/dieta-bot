# Plan — D28 Release 2: Home day strip, past day and extras

- Status: Aguardando aprovação
- Date: 09/10/2026
- Owning context: `design`
- Affected code: none in `apps/` or `server/`. Figma `Design` → `Release 2` → section "Home history", new components in `Componentes`. Repository: `docs/qa/figma/{dark,light}/{home1,homeH,homeE,chatGX}.png` (`home1` redrawn, `homeH`, `homeE` and `chatGX` new), the "Home history" node ids in `tools/export-figma.mjs`.
- Prerequisites: [D27](pending_manual_validation/d27-conversational-onboarding.md) `Concluído` (one design plan at a time); [ADR-058](../../produto/adrs/ADR-058-extras-and-history.md) accepted by this approval.
- Figma MCP budget: ≤ 90 calls (at most 120 a day, ADR-031 § 6).

Authorization, lifecycle, cancellation and deferral: `docs/sdd/README.md`. Approval: `Aprovo o plano docs/design/plans/d28-home-extras-and-history.md. Implemente o plano aprovado.`

## Objective

Draw the Home of [ADR-058](../../produto/adrs/ADR-058-extras-and-history.md) in Aero, in both themes: the 30-day strip above the ring, the timeline ordered by time with an extra node, a past day, the Trocar sheet with the `Extra` entry and the extra receipt. Reference for the strip: the owner's sketch of 09/10/2026 (circles with the day number on a horizontal line, today at the right, selected and unselected states).

## Sources (feature parity, ADR-031 § 4)

| Gold | Reference (current gold or frame, layout only) | Behavior source |
|---|---|---|
| `home1` | current `home1` | ADR-058 decisions 2–3 ([home-timeline](../../produto/specifications/home-timeline.md) rules 1–13 plus the strip and the extra node `Extra · 15:40`) |
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

<Filled at Completion: discovery table, MCP calls used, owner OK date, exported files.>
