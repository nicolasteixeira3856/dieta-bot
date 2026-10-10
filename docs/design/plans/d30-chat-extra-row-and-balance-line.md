# Plan — D30 Release 2: Trocar with the Extra row and the day balance on the extra receipt

- Status: Aguardando aprovação
- Date: 10/10/2026
- Owning context: `design`
- Affected code: none in `apps/` or `server/`. Figma `Design` → `Release 2` → section "Home history · D28" (the `chatGX` frames) and a new `chatT` pair next to them; `Chat/Receipt` in `Componentes` if the caption line needs a property. Repository: `docs/qa/figma/{dark,light}/{chatT,chatGX}.png` and the `chatT` node ids in `tools/export-figma.mjs`.
- Prerequisites: [D28](completed/d28-home-extras-and-history.md) and [D29](completed/d29-landing-phone-screens.md) `Concluído` (one design plan at a time).
- Figma MCP budget: ≤ 40 calls (at most 120 a day, ADR-031 § 6).

Authorization, lifecycle, cancellation and deferral: `docs/sdd/README.md`. Approval: `Aprovo o plano docs/design/plans/d30-chat-extra-row-and-balance-line.md. Implemente o plano aprovado.`

## Objective

Bring two Chat golds back in line with the app and the [Chat spec](../../produto/specifications/chat.md), so their `GoldTest` and emulator gates pass again instead of being reported ([ADR-027](../../android/adrs/ADR-027-golds-divergentes.md)):

- `chatT`: the Trocar sheet in the record mode ends with the `Extra · fora das refeições` row (Chat rule on extras, [ADR-058](../../produto/adrs/ADR-058-extras-and-history.md) decision 1). D28 drew the row behind `Show extra` (off) and kept `chatT` unchanged; the app always shows it, so `GoldTest.chatT` is report-only since [A72](../../android/plans/pending_manual_validation/a72-extras-and-history.md) (7.6 % Dark / 23.4 % Light).
- `chatGX`: the extra receipt carries the day balance in its caption line, `{comido} de {teto efetivo} kcal · faltam {p} g de proteína` (Chat rule 5, [A64](../../android/plans/completed/a64-chat-context-fields-day-balance.md)). D28 cloned `chatG`, drawn before A64, so the receipt and its actions sit one line higher than in the app (emulator thread tail 6.06 % Dark / 13.24 % Light).

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
6. **Hand-over** to [A73](../../android/plans/a73-chat-gold-gates.md): the new frame heights and the balance copy of the sample.

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

<Filled at Completion: discovery table, MCP calls used, owner OK date, exported files.>
