# Plan — A73 `chatT` and `chatGX` gated again on the D30 golds

- Status: Aguardando aprovação
- Date: 10/10/2026
- Owning context: `android`
- Affected code: `apps/android/` tests only (`ui/GoldTest.kt`, `feature/chat/ChatFixtures.kt`), the capture tooling (`tools/capture-history.sh`, `tools/diff-gold.mjs`), `docs/qa/android/current/{dark,light}/{chatT,chatGX}.png`. No production code.
- Prerequisites: [D30](../../design/plans/completed/d30-chat-extra-row-and-balance-line.md) `Concluído` (the new `chatT` and `chatGX` golds exported); [A72](pending_manual_validation/a72-extras-and-history.md) merged.

Authorization, lifecycle, cancellation and deferral: `docs/sdd/README.md`. Approval: `Aprovo o plano docs/android/plans/a73-chat-gold-gates.md. Implemente o plano aprovado.`

## Objective

End the two spec-over-gold conflicts that A72 left open ([ADR-027](../adrs/ADR-027-golds-divergentes.md)): `GoldTest.chatT` is report-only since the Trocar sheet gained the `Extra · fora das refeições` row, and the `chatGX` emulator tail is over 2 % because the receipt carries the day balance line. With the D30 golds both become gates again, so a regression in the sheet or the extra receipt fails the build instead of being reported.

## Scope

1. **`GoldTest.chatT`**: `reportOnly` removed in both themes; the KDoc that explains the report goes.
2. **`ChatFixtures.chatGX`**: the receipt gains the balance of the D30 sample (`1.300 de 2.175 kcal · faltam 74 g de proteína`, or the copy D30 hands over), so `GoldTest.chatGX` draws what the app shows; the frame height follows the new gold.
3. **Emulator** (fake server, `tools/capture-history.sh dark|light`):
   - the scene that opens Trocar on the extra receipt saves it as `chatT` in `docs/qa/android/current/{theme}/` (today an evidence file);
   - `tools/diff-gold.mjs`: the `chatT` footer region follows the taller sheet; `chatGX` keeps the header gate and its thread tail passes within 2 %;
   - the seeded day makes the balance line read the gold copy, or the difference is the number only and stays within 2 %.
4. **No spec change:** the Chat spec already describes both states; the QA README inventory keeps the same ids.

## Out of scope

- Production code, behavior or copy. Other golds (`chatTI`, `chatF`, `chatG`, `ob1`). Model calls (0 in this plan).

## Validation

1. `./gradlew :app:testDevDebugUnitTest` and `:app:verifyRoborazziDevDebug`: pass, with `GoldTest.chatT_*` and `GoldTest.chatGX_*` gating (blurred diff ≤ 2 %).
2. Emulator, both themes, only the two touched states (partial rule): `chatT` and `chatGX` captured and `node tools/diff-gold.mjs` within 2 %.
3. `node tools/check-docs.mjs` passes.

## Results

<Filled at Completion: commands and real numbers, captures, pending items.>
