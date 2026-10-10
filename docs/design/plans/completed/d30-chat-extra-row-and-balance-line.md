# Plan — D30 Release 2: Trocar with the Extra row and the day balance on the extra receipt

- Status: Concluído
- Date: 10/10/2026
- Owning context: `design`
- Affected code: none in `apps/` or `server/`. Figma `Design` → `Release 2` → section "Home history · D28" (the `chatGX` frames) and a new `chatT` pair next to them; `Chat/Receipt` in `Componentes` if the caption line needs a property. Repository: `docs/qa/figma/{dark,light}/{chatT,chatGX}.png` and the `chatT` node ids in `tools/export-figma.mjs`.
- Prerequisites: [D28](d28-home-extras-and-history.md) and [D29](d29-landing-phone-screens.md) `Concluído` (one design plan at a time).
- Figma MCP budget: ≤ 40 calls (at most 120 a day, ADR-031 § 6).

Authorization, lifecycle, cancellation and deferral: `docs/sdd/README.md`. Approval: `Aprovo o plano docs/design/plans/d30-chat-extra-row-and-balance-line.md. Implemente o plano aprovado.`

## Objective

Bring two Chat golds back in line with the app and the [Chat spec](../../../produto/specifications/chat.md), so their `GoldTest` and emulator gates pass again instead of being reported ([ADR-027](../../../android/adrs/ADR-027-golds-divergentes.md)):

- `chatT`: the Trocar sheet in the record mode ends with the `Extra · fora das refeições` row (Chat rule on extras, [ADR-058](../../../produto/adrs/ADR-058-extras-and-history.md) decision 1). D28 drew the row behind `Show extra` (off) and kept `chatT` unchanged; the app always shows it, so `GoldTest.chatT` is report-only since [A72](../../../android/plans/pending_manual_validation/a72-extras-and-history.md) (7.6 % Dark / 23.4 % Light).
- `chatGX`: the extra receipt carries the day balance in its caption line, `{comido} de {teto efetivo} kcal · faltam {p} g de proteína` (Chat rule 5, [A64](../../../android/plans/completed/a64-chat-context-fields-day-balance.md)). D28 cloned `chatG`, drawn before A64, so the receipt and its actions sit one line higher than in the app (emulator thread tail 6.06 % Dark / 13.24 % Light).

## Sources (feature parity, ADR-031 § 4)

| Gold | Reference (current gold or frame, layout only) | Behavior source |
|---|---|---|
| `chatT` | current `chatT` (`Release 1`, `63:1911` Light, `63:2176` Dark) | Chat spec: Trocar in the record mode with the slots of today and `Extra · fora das refeições` at the end (not in the target of an addition, so `chatTI` does not change) |
| `chatGX` | current `chatGX` (`214:1832` Light, `215:2262` Dark) | Chat rule 5 (the newest active record receipt of today carries the balance) and the extra rule (`Registrado como extra · {HH:mm}`); copy from `DayBalance.line` |

Code: `apps/android/app/src/main/java/app/fibrai/android/feature/chat/` (`ChatScreen` Trocar sheet, tag `chat-sheet-slot-extra`; `ChatRecordComponents` receipt, tag `chat-receipt-balance`) and `domain/DayBalance.kt`.

## Scope

1. **Discovery (read only):**
   - per gold, list every visible element and classify it: `app`, `gold-only` (dropped) or `copy` (exact pt-BR text from the code);
   - `chatGX` balance copy for the D28 sample day (1300 kcal eaten with the extra, effective ceiling 2175, protein 76 of 150 g): `1.300 de 2.175 kcal · faltam 74 g de proteína`;
   - check whether `Chat/Receipt` already has a caption line (the A60 plan line `Plano: … · Registrado: …` shipped without a gold) and how the app wraps the balance at 390 px;
   - the table goes into Results before any write.
2. **Components** (in `Componentes`, readability rule):
   - `Chat/Receipt`: a caption line under the title row (`Caption`, `text/muted`), behind a boolean `Show balance` (default off, so every other receipt gold stays the same) with a text property; added only if the discovery finds no such line.
   - `Sheet/SlotList`: no change (the `Show extra` row of D28 is reused).
3. **Screens:**
   - `chatT`: a new Light frame in `Release 2` (section "Home history · D28", next to `chatGX`) cloned from the `Release 1` `chatT`, with `Show extra` on; the sheet grows by one row and the frame height follows the sheet; Dark clone with the `Dark` mode, no other change. The `Release 1` frames stay as history (precedent: D28 `home1`).
   - `chatGX`: the receipt of both frames gains the balance line; the actions and the composer move down with it; the frame height follows.
   - frame names `<id> · <screen title> · Light|Dark`; spacing per ADR-031 § 3.
4. **Owner review** (Figma review gate):
   - one screenshot per changed frame (4) is sent to the owner;
   - the plan goes to `pending_manual_validation/` until the owner's OK in Figma;
   - fixes follow, with one screenshot per changed frame.
5. **Export** after the OK:
   - `chatT` in `tools/export-figma.mjs` moves to the new `Release 2` ids; `chatGX` keeps its ids;
   - `node tools/export-figma.mjs --only chatT,chatGX`;
   - the inventory of `docs/qa/README.md` keeps the same ids (no new gold);
   - `node tools/check-figma.mjs`.
6. **Hand-over** to [A73](../../../android/plans/completed/a73-chat-gold-gates.md): the new frame heights and the balance copy of the sample.

## Out of scope

- Compose and the tests (A73). Behavior or copy changes.
- `chatTI` (the Trocar of an addition has no Extra row, by the spec).
- Other golds whose receipt lacks the A64 balance line in the app captures (`chatF`, `chatG` and the other receipt states): their emulator gates stop above the receipt today; a later design plan redraws them if the owner wants.
- `ob1` / `ob1e` progress bar (the gold draws 5/16 with `9 de 16`; A71 fills n/16): another design plan.

## Validation

1. The discovery table exists in Results before the first write.
2. A read-back confirms instances (not detached frames) for every DS component, no raw hex fills outside the variables, no overlapping nodes, Light frames in mode Light and Dark frames in mode Dark.
3. Visual: one screenshot per changed frame; manual owner OK.
4. `node tools/check-figma.mjs` and `node tools/check-docs.mjs` pass.

## Results

### Discovery (10/10/2026, before any write)

Read only: Figma `Release 1` `chatT` (`63:1911`, section `Chat · D5`), `Release 2` `chatGX` (`214:1832`) and the section `Home history · D28` (`214:1381`, 1960 × 3952, `chatGX` is its rightmost column at x 1490), the component set `Chat/Receipt` (`70:228`); code `ChatScreen` (Trocar sheet) and `ChatRecordComponents.ReceiptCard`, `DayBalance.line`.

| Gold | Element | Class | Source / copy |
|---|---|---|---|
| `chatT` | Chat behind the scrim (header, date, user, the estimate answer, composer), `Sheet` with `Selecione a refeição`, the subtitle `Escolha o momento do dia para salvar este registro:`, the four slots (Jantar `20:00 (atual)` selected), `Confirmar refeição`, `Cancelar` | app (unchanged) | Chat spec, Trocar; `ChatScreen` |
| | Row `Extra` · `fora das refeições` with the clock, after Jantar, unselected | app (new in the gold) | Chat spec, extras; `ChatScreen` `chat-sheet-slot-extra`; the `Show extra` row of `Sheet/SlotList` (D28) |
| `chatGX` | Header, date chip, user message, Tali estimate, receipt `Registrado como extra · 15:40` `+160 kcal`, Excluir · Trocar refeição · Editar, composer | app (unchanged) | D28 |
| | Day balance under the kcal chip, inside the receipt card: `1.300 de 2.175 kcal · faltam 74 g de proteína` | app (new in the gold) / copy | Chat rule 5; `DayBalance.line` (D28 sample: 1300 kcal eaten with the extra, ceiling 2175 with the workout, protein 76 of 150 g) |

Decisions of this discovery:

- `Chat/Receipt` has no caption line (the A60 plan line and the A64 balance shipped without a gold). It gains `Balance` (TEXT) and `Show balance` (BOOLEAN, default off) on `State=Saved`: a `Caption` text in `text/muted` below the kcal chip, 5 px under it, as in `ReceiptCard`. Every other receipt gold keeps the property off and does not change.
- `chatT` is a sheet over the screen: the frame stays 390 × 844 and the sheet, anchored at the bottom, grows upward by the Extra row (the plan said the frame height follows the sheet; a bottom sheet does not lengthen the screen). The new frame goes to `Home history · D28` after `chatGX` (product order: the receipt, then its Trocar), and the section widens by one column.

### Build (10/10/2026)

- **`Chat/Receipt`** (set `70:228`): properties `Balance#221:0` (TEXT, default the sample copy) and `Show balance#221:7` (BOOLEAN, default off); text `Balance` (`221:2313`, `Caption`, fill bound to `text/muted`, fill width) as the last child of the `Text` column of `State=Saved`, under `Title row` and `Chip below`, 5 px apart (the column spacing). The set description names the property. Every other receipt instance keeps it off.
- **`chatGX`** (`214:1832` Light, `215:2262` Dark, same ids): the receipt turns on `Show balance` with `1.300 de 2.175 kcal · faltam 74 g de proteína`; the line wraps in two at the receipt width, as the app does at 390 dp (A64 saw the same wrap on the emulator). The receipt grows from 231 to 272 px, the frames from 902 to 943 px.
- **`chatT`** (`Release 2`, section `Home history · D28`, column x 1960 after `chatGX`; the section widens from 1960 to 2430 px): Light `221:2329` cloned from the `Release 1` frame `63:1911`, the `Content` instance of the sheet with `Show extra` on (row `Extra` · `fora das refeições`, unselected), the sheet moved up to y 128 (636 → 716 px) so it stays anchored at the bottom of the 390 × 844 frame; Dark `221:2506` cloned from it in mode `Dark`, 9 loose text paint segments rebound with the Dark value. The `Release 1` `chatT` frames (`63:1911`, `63:2176`) are untouched.

Fix during the build (before the review): the first write put `Balance` between the title row and the kcal chip; the app draws it under the chip (`ReceiptCard`), so it moved to the end of the column.

### Validation (before the owner review)

1. Discovery table above, written before the first write.
2. Read-back (`use_figma`) of the four frames: instances for every DS component (`chatGX` 22, `chatT` 31 in each theme); zero visible solid paints without a variable or style outside instances; no overlapping children in the section and no page node overlapping it; Light frames in mode `Color=Light`, Dark frames in `Color=Dark`.
3. Visual: one review PNG per frame (4), from the dry-run export (`node tools/export-figma.mjs --only <node id> --dry-run`), sent to the owner on 10/10/2026.

Figma MCP budget: 8 of 40 calls (whoami, 2 skill reads, 2 reads (the first one over the 20 KB return limit, nothing written), 2 writes, 1 read-back).

### Owner review

**OK (10/10/2026)** on the first round, with no fixes ("Aprovado no Figma, pode exportar"), covering the open point of the discovery: `chatT` keeps the 844 px screen and the sheet grows upward.

### Export (10/10/2026)

- `tools/export-figma.mjs`: `chatT` moved to the Release 2 ids (`221:2329` Light, `221:2506` Dark); `chatGX` keeps its ids. The Release 1 `chatT` frames (`63:1911`, `63:2176`) stay in the file as history.
- `node tools/export-figma.mjs --only chatT,chatGX`: `chatT` 780 × 1688 (16.49 % of the pixels changed in Light, 4.95 % in Dark), `chatGX` 780 × 1804 → 780 × 1886 (both themes).
- Inventory of `docs/qa/README.md`: unchanged (same ids).
- `node tools/check-figma.mjs`: 114 golds verified (57 dark + 57 light). `node tools/check-docs.mjs` passes.
- **Hand-over to [A73](../../../android/plans/completed/a73-chat-gold-gates.md):** `chatGX` is 943 px tall, its receipt 272 px with the balance in two lines under the chip; `chatT` stays 390 × 844 with the sheet at y 128 (716 px), the Extra row 70 px after Jantar. `web/` does not use either gold.

Figma MCP budget: 8 of 40 calls in total (no call after the review: the export uses the REST API).
