# Plan — A52 Home: tap to record, long press to skip

- Status: Aguardando aprovação
- Date: 06/10/2026
- Owning context: `android`
- Executable boundary: `apps/android/` (Home screen, Aero meal card, navigation from Home to Chat, tests) plus `tools/capture-home.sh`.
- Related documentation: [home-timeline](../../produto/specifications/home-timeline.md), Android validation and plan indexes. Fresh captures go to the existing QA folders.
- Prerequisites: [D14](../../design/plans/completed/d14-home-card-gestures.md) `Concluído` with the Home golds re-exported; ADR-040 accepted.

Authorization and delivery follow [SDD](../../sdd/README.md). Approval: `Aprovo o plano docs/android/plans/a52-home-card-gestures.md. Implemente o plano aprovado.`

## Objective

An empty meal card on the Home opens the Chat on tap and asks to skip on long press ([ADR-040](../../produto/adrs/ADR-040-home-card-gestures-app-reset.md) decision 1).

## Scope

1. **Copy.** `HomePanelScreen` description for `NEXT`/`EMPTY`: `Nenhum registro · Toque para registrar, segura para pular`, wrapping allowed, as the D14 gold.
2. **Gestures.** `AeroMealCard` gains an optional `onLongClick` (combined clickable, keeping the current ripple and press feedback). For a tappable card (same condition as today):
   - `onClick` → the Home's existing `onChat` (the FAB's navigation), nothing else;
   - `onLongClick` → long-press haptic, then today's `confirmSkip` dialog; confirm calls `onSkip` unchanged;
   - semantics: `onClickLabel = "Registrar"`, `onLongClickLabel = "Pular"`.
   Other card states keep `onClick = null` and no long click.
3. **QA tooling.** `tools/capture-home.sh` replaces the tap-to-skip step with a long press (`input swipe x y x y 800`) and adds a tap that asserts the Chat opened.

### Intended specification changes

At Completion: home-timeline rule 4 (copy, tap → Chat, long press → `Pular {nome}?`), rule 7 (FAB and an empty card open the Chat; the Chat stays the only place that records), acceptance criterion "Pular na timeline…" (by long press); Provenance gains A52.

## Out of scope

Config, Chat behavior or content, slot context in the Chat, push actions, any card other than empty/next, skip undo.

## Validation

1. Compose UI/unit tests: tap on an empty card navigates to Chat and writes no skip; long press shows `Pular {nome}?`; confirm writes the skip once; cancel writes nothing; logged/skipped/`Outros` cards react to neither gesture; accessibility labels present.
2. Roborazzi baselines updated for the Home states that changed.
3. Emulator, dev flavor: fresh `home0`, `home1`, `homeW`, `homeX`, `chatP` (as changed by D14) in `docs/qa/android/current/{dark,light}/`, compared with the gold under the QA rules of AGENTS, with a written diff list. Partial validation: Home flows only.
4. `testDevDebugUnitTest`, `verifyRoborazziDevDebug`, `assembleDevRelease` and `node tools/check-docs.mjs` pass.
5. Manual: owner confirms tap and long press on a device (dev build).

## Results

Planning only.
