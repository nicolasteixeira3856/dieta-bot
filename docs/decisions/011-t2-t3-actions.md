# 011 — T2/T3 taps write a meal or discard it

Date: 2026-09-25

## Decision

A tap on T2 or T3 writes a meal through `DayRepository.addLog` or it is discarded. T2 stays the existing full-screen route. After a fit result the T3 primary CTA is “Vou nesse” and does not call `/v1/fit` again. Home lists today’s `DaySnapshot.logs` under the composer. Room schema stays version 1.

Chosen: **B**.

## Why B

- **A** — keep `t2Answer` as an index and let Confirmar always insert; T3 “Encaixar N” re-calls `/v1/fit`. The ButtonGroup is decorative. A dish with `fits == false` can still be committed.
- **B** — Sim, Forma, Esquece, Ok and Desfazer are distinct ViewModel functions. 0-kcal never inserts. Fitting T3 cards are selectable; “Vou nesse” `addLog`s the selected fitting dish and closes the sheet; “Já comi” opens empty T1 and writes nothing. Remaining and the Home log list move together after `addLog`.
- **C** — auto-commit on T1 Enviar and skip T2. Drops the one-question-if-not-high rule.

## T2

High: show kcal + P + “Cabe. Sobra X”. Ok → `addLog(window, text, est.kcal, est.p, stable=true)` → HOME. Desfazer → T1 sheet, no log.

Low: one question + ButtonGroup Sim / Forma / Esquece, each a different function. Show estimate kcal when the API sent kcal > 0; hide the number when kcal is 0. Sim → `addLog` with the current estimate → HOME. Forma → T1 with the same text, no log. Esquece → HOME, no log. If kcal == 0, Confirmar is disabled until Forma or Esquece.

## T3

Cards are `FitOut.dish` plus options that fit. Selected card is highlighted. First CTA while `fit == null` still `requestFit()`. Once `fit != null`, that CTA is “Vou nesse”: commits the selected fitting dish or no-ops if nothing is selected. `fits == false` is not offered as “Vou nesse”. “Já comi” is the second action after a result.

## Home

Not a new screen. Under the composer: `{windowTitle} · {kcal} kcal · {p} g P`. Day 1 still has zero chips.

## Visual

Dark captures on emulator-5554: `docs/qa/android/current/dark/t2.png`, `t2q.png`, `t3ideia.png`. Splash is a cold start, not a freeze.

Diff vs `docs/qa/wire/` (status bar / clock / battery / gesture nav ignored):

- **t2** — layout, 28pt field, “Cabe. Sobra X”, Ok + Desfazer, CTA `#f3f5f7` on `#111`. Leftover empty.
- **t2q** — question + ButtonGroup Sim / Forma / Esquece match. Confirmar is disabled (kcal 0) per this ADR; gold shows it enabled. Range `~380–480 kcal` stays on the capture seed via `t2Range`.
- **t3ideia** — remaining 34pt, sheet radius 22, card 14, day-1 zero chips, gold accent on the selected card. After a result the primary CTA is “Vou nesse” and “Já comi” is second; gold `t3ideia` uses “Já comi” as the only CTA. Objective labels win.

## Refused

Room migration / new columns. Editing `server/`. Auto-commit on T1 Enviar. Changing `reservedUpcoming`. Appbar / FAB / BottomNav. Firebase. TDEE. New screens. Fake captures.
