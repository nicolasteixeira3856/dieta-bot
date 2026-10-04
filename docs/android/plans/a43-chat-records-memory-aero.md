# Plan — A43 Chat records, photo and memory on Aero

- Status: Aguardando aprovação
- Date: 03/10/2026
- Owning context: `android`
- Affected code: `apps/android/` only:
  - `feature/chat/` (receipt, photo bubble, attachment, photo source sheet, memory chips, routine card, meal plan), components in `core/designsystem/aero/`, tests;
  - docs updated with the delivery: the source of `chatF`, `chatA`, `chatG`, `chatU`, `chatD`, `chatR`, `chatM` and `chatS` in `docs/qa/README.md`, captures.
- Prerequisites: [A42](a42-chat-core-aero.md) and [D6](../../design/plans/completed/d6-release1-chat-records-memory.md) `Concluído`. A predecessor in `pending_manual_validation/` whose only open item is the consolidated phone check ([A40 § Validation](pending_manual_validation/a40-home-aero.md#validation) step 3) counts as `Concluído` here.

Authorization, lifecycle, cancellation and deferral: `docs/sdd/README.md`. Approval: `Aprovo o plano docs/android/plans/a43-chat-records-memory-aero.md. Implemente o plano aprovado.`

## Objective

Move the remaining Chat states onto Aero, matching the Figma golds, with no behavior change.

## Scope

Procedure as [A40 § Scope](pending_manual_validation/a40-home-aero.md#scope) steps 1–5, for the eight golds of this flow. Captures:

- `tools/capture-photo.sh`;
- `tools/capture-replace.sh`;
- `tools/capture-chat.sh` with `SCENES=v2` and `SCENES=a34`.

Flow-specific work:

1. **Receipts** keep every action and rule of ADR-028 (Desfazer, Excluir, Trocar refeição, Editar, Registrar on doubt).
2. **Photo** keeps ADR-018 (JPEG q85, 2048 px, EXIF stripped, 16 MB guard).
3. **Gold conflicts:** the regional gates for `chatF` and `chatA` and the `GOLD_CONFLICTS` entries for `chatG`, `chatF` and `chatA` are removed once the new golds pass.
4. **Spec:** [chat](../../produto/specifications/chat.md) and [memoria-push](../../produto/specifications/memoria-push.md) change only their visual references.

## Out of scope

As A40.

## Validation

As [A40 § Validation](pending_manual_validation/a40-home-aero.md#validation), with 16 images.

## Results

<Filled at Completion.>
