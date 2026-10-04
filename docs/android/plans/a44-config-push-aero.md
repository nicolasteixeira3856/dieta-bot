# Plan — A44 Config and push on Aero

- Status: Aguardando aprovação
- Date: 03/10/2026
- Owning context: `android`
- Affected code: `apps/android/` only:
  - the Config screens, the wipe dialog, the push notification builder (small icon, accent color, actions), tests;
  - docs updated with the delivery: the source of `cfg`, `cfgS`, `wipe` and `push` in `docs/qa/README.md`, captures.
- Prerequisites: [A43](a43-chat-records-memory-aero.md) and [D7](../../design/plans/completed/d7-release1-config-push.md) `Concluído`. A predecessor in `pending_manual_validation/` whose only open item is the consolidated phone check ([A40 § Validation](a40-home-aero.md#validation) step 3) counts as `Concluído` here.

Authorization, lifecycle, cancellation and deferral: `docs/sdd/README.md`. Approval: `Aprovo o plano docs/android/plans/a44-config-push-aero.md. Implemente o plano aprovado.`

## Objective

Move Config, the wipe dialog and the meal reminder onto Aero, matching the Figma golds, with no behavior change. After this plan every gold has source `figma`.

## Scope

Procedure as [A40 § Scope](a40-home-aero.md#scope) steps 1–5, for the four golds of this flow (`tools/capture-config.sh`, `tools/capture-push.sh`). Flow-specific work:

1. **Push:** only the fields the app controls change (small icon, accent color, text and actions). `push` stays a lock-screen gold measured and reported, not gated ([docs/qa/README.md](../../qa/README.md) § Gate), unless D7 makes it comparable.
2. **Dev tools:** `Memória da IA (dev)` may adopt Aero components, but it has no gold ([ADR-019](../../produto/adrs/ADR-019-ferramentas-dev.md)) and keeps `debug.nutri.hide_dev_tools=1` for the cfg capture.
3. **Spec:** [memoria-push](../../produto/specifications/memoria-push.md) changes only its visual references.

## Out of scope

As A40.

## Validation

As [A40 § Validation](a40-home-aero.md#validation), with 8 images.

## Results

<Filled at Completion.>
