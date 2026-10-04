# Plan — A42 Chat core on Aero

- Status: Aguardando aprovação
- Date: 03/10/2026
- Owning context: `android`
- Affected code: `apps/android/` only:
  - `feature/chat/` (screen, header, composer, estimate, question, loader, slot sheet, skip dialog), chat components in `core/designsystem/aero/`, tests;
  - docs updated with the delivery: the source of `chat0`, `chatL`, `chatQ`, `chatE`, `chatT`, `chatP` and `chatX` in `docs/qa/README.md`, captures.
- Prerequisites: [A41](a41-splash-onboarding-aero.md) and [D5](../../design/plans/completed/d5-release1-chat-core.md) `Concluído`. A predecessor in `pending_manual_validation/` whose only open item is the consolidated phone check ([A40 § Validation](pending_manual_validation/a40-home-aero.md#validation) step 3) counts as `Concluído` here.

Authorization, lifecycle, cancellation and deferral: `docs/sdd/README.md`. Approval: `Aprovo o plano docs/android/plans/a42-chat-core-aero.md. Implemente o plano aprovado.`

## Objective

Move the core Chat states onto Aero, matching the Figma golds, with no behavior change.

## Scope

Procedure as [A40 § Scope](pending_manual_validation/a40-home-aero.md#scope) steps 1–5, for the seven golds of this flow (`node tools/fake-chat-server.mjs` + `tools/capture-chat.sh`). Flow-specific work:

1. **`AeroLoader`** replaces the M3 `LoadingIndicator` in the estimate/fit loading.
2. **`AeroComposer`:** keep the 2000-character limit and `chatX` error (ADR-022), photo entry and send.
3. **Gold conflicts:** the conflicts listed in `docs/qa/README.md` (`GOLD_CONFLICTS`) for `chat0`, `chatL` and `chatE` end when these ids switch to Figma, because D5 draws them from one canonical component tree. The entries are removed in this delivery once the new golds pass.
4. **Spec:** [chat](../../produto/specifications/chat.md) changes only its visual references.

## Out of scope

- `chatF`, `chatA`, `chatG`, `chatU`, `chatD`, `chatR`, `chatM` and `chatS` ([A43](a43-chat-records-memory-aero.md)). The rest as A40.

## Validation

As [A40 § Validation](pending_manual_validation/a40-home-aero.md#validation), with the chat captures and 14 images.

## Results

<Filled at Completion.>
