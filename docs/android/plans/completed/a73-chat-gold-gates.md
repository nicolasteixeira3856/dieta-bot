# Plan — A73 `chatT` and `chatGX` gated again on the D30 golds

- Status: Concluído
- Date: 10/10/2026
- Owning context: `android`
- Affected code: `apps/android/` tests only (`ui/GoldTest.kt`, `feature/chat/ChatFixtures.kt`), the capture tooling (`tools/capture-history.sh`, `tools/diff-gold.mjs`), `docs/qa/android/current/{dark,light}/{chatT,chatGX}.png`. No production code.
- Prerequisites: [D30](../../../design/plans/completed/d30-chat-extra-row-and-balance-line.md) `Concluído` (the new `chatT` and `chatGX` golds exported); [A72](../pending_manual_validation/a72-extras-and-history.md) merged.

Authorization, lifecycle, cancellation and deferral: `docs/sdd/README.md`. Approval: `Aprovo o plano docs/android/plans/a73-chat-gold-gates.md. Implemente o plano aprovado.`

## Objective

End the two spec-over-gold conflicts that A72 left open ([ADR-027](../../adrs/ADR-027-golds-divergentes.md)): `GoldTest.chatT` is report-only since the Trocar sheet gained the `Extra · fora das refeições` row, and the `chatGX` emulator tail is over 2 % because the receipt carries the day balance line. With the D30 golds both become gates again, so a regression in the sheet or the extra receipt fails the build instead of being reported.

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

Delivered on 10/10/2026, right after the D30 export ([#245](https://github.com/nicolasteixeira3856/dieta-bot/pull/245)).

- **`GoldTest.chatT`** gates again in both themes (`reportOnly` and its KDoc removed).
- **`ChatFixtures.chatGX`**: the receipt carries `1.300 de 2.175 kcal · faltam 74 g de proteína`; `GoldTest.chatGX` renders at 943 dp, the new gold height.
- **Capture tooling.**
  - `tools/capture-chat.sh` gains `SCENES=a73`: quick onboarding, then the seeded gold conversation with no slot, Registrar opens Trocar, the last meal is picked and the screen is saved as `chatT`. `seed_gold` moved out of the full-run block so the scene can call it. Deviation: the plan put this capture in `capture-history.sh`, but the Trocar there opens from an extra receipt, with `Extra` selected and no `(atual)`, so it does not match the `chatT` state. The `chatT` state is an estimate without a slot, already seeded by `capture-chat.sh`.
  - The scene sets the device clock to 20:15 in São Paulo, because the gold marks Jantar `(atual)` and the app picks the current meal by the hour. A morning run put `(atual)` on Café (light 2.11 %). Auto time comes back at the end of the script, as in the A34 scene.
  - `tools/diff-gold.mjs`: the `chatT` footer zone grows from 1368 to 1528 px (the 716 dp sheet plus 48 dp).
  - `tools/capture-history.sh`: a swipe before the "the extra shows on today's timeline" check. The timeline is ordered by time, so after about 07:30 the extra sits below Café and off the screen. The check failed in both themes this morning; it passed in the A72 run at 00:46.
- **QA README:** the `SCENES=a73` scene in the capture list.

Validation:

1. `./gradlew :app:testDevDebugUnitTest :app:verifyRoborazziDevDebug`: 727 tests, 0 failures, Roborazzi passes. `GoldTest` blurred diff:
   - `chatT`: 0.00 % in both themes, gated. Before: 7.63 % dark / 23.44 % light, reported only.
   - `chatGX`: 0.01 % in both themes.
   - `chatTI`: 0.29 % dark / 0.76 % light, unchanged (no Extra row on an addition).
2. Emulator (Medium_Phone, 780 × 1688 @ 320, devDebug against `tools/fake-chat-server.mjs`), only the two touched states (partial rule):
   - `SCENES=a73 tools/capture-chat.sh`: `chatT` 0.09 % dark / 1.99 % light. The light margin is the seeded thread showing through the glass sheet, which differs from the gold's thread.
   - `tools/capture-history.sh`, the `chatGX` gates:

     | | Dark | Light | Before |
     |---|---|---|---|
     | Header | 0.00 % | 0.00 % | — |
     | Thread tail | 0.36 % | 0.27 % | 6.06 % / 13.24 % |

   - The tail compares only the bottom of the thread because the phone's thread is shorter than the gold frame; the whole screen stays reported, as for the other long threads.
   - The history script's other captures (`home1`, `homeH`, `homeE`, `homeW`, `homeC`, `homeK`, `homeP`) were restored from `develop`, outside this plan.
   - All the script's checks pass after the swipe fix, verified in dark.
3. `node tools/check-docs.mjs`: passes.

No model calls. Nothing left to validate by hand: the plan changes only tests and capture tools.
