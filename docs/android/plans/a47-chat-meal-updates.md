# Plan — A47 Chat meal updates

- Status: Aguardando aprovação
- Date: 04/10/2026
- Owning context: `android`
- Executable boundary: `apps/android/` only: Chat UI/ViewModel/recorder, domain update rules, network DTOs/prompt building, Room entities/migration and relevant tests.
- Related documentation: product Chat, Room specification, Android validation and plan/index lifecycle. Fresh emulator captures belong in the existing QA folders.
- Prerequisites: acceptance of [ADR-032](../../produto/adrs/ADR-032-acrescimos-e-correcoes-de-refeicoes.md); [S18](../../server/plans/completed/s18-meal-additions-and-revisions.md) delivered and available on the dev server; [D9](../../design/plans/d9-chat-meal-updates.md) `Concluído` with exported golds; [A43](completed/a43-chat-records-memory-aero.md) delivered.

Authorization and delivery follow [SDD](../../sdd/README.md). Approval: `Aprovo o plano docs/android/plans/a47-chat-meal-updates.md. Implemente o plano aprovado.`

## Objective

Show and record the requested addition without changing previously recorded food or copying it into another meal. Keep revisions explicit, preserve undo, and render the distinction between the item amount and the whole-meal total from structured data.

## Sources

ADR-032 owns the behavior/copy. S18 owns the proposed capability; after its delivery use the [HTTP contract](../../api-contract.md). [Product Chat](../../produto/specifications/chat.md), [Room](../specifications/room-v2.md), the [gold inventory](../../qa/README.md#golds) and D9 own the live implementation and visual boundaries.

Read the delivered A43 code at start. This plan is a separate successor; do not amend A43, implement from its uncommitted checkout or change its acceptance evidence. Rebase against the then-current master before implementation and use the current approved stack without adding a new framework.

## Scope

### 1. Consume and preserve structured proposals

Send S18's capability only on normal supported Chat turns. Decode its metadata with nullable defaults for older servers. Never infer an addition, target or numeric delta from `reply`, food names, button copy or a guessed difference between two estimates.

Capture the full slot state used in the outgoing request, not only the total. Persist a versioned proposal payload on the assistant message containing the operation, request date, source identity and captured base, the validated addition, and the selected destination/expected state when applicable. Insert this with the assistant row before any record attempt so recreation cannot turn an addition into a new whole-meal record.

Before expiring a previous pending action on send, capture S18's `pending_addition` context if the last proposal is still unrecorded and its source matches the current day. Send it only for the immediately preceding addition being continued; do not serialize receipts as user statements. Test that clarification of that proposal, an explicit second portion and a previously confirmed addition cannot share the same accounting path.

Use one nullable JSON column in `chat_message`, with the next available Room schema version discovered from the live spec at implementation start. Existing rows remain null. Add a non-destructive migration, exported schema and migration tests; preserve all records, photos, memory, undo payloads and older receipt actions. Keep daily state in Room. No backfill guessing what an old response intended.

Missing metadata from an older server keeps the established legacy UI and confirmation semantics; do not label it as a verified addition. New metadata that is malformed or contradicts the base/total is non-actionable. Distinguish absent legacy data from invalid opted-in data. If the response arrives after the day or captured source changed, show it as not recorded, with no write or routine application.

### 2. Apply additions using explicit before/after states

Implement the rules as domain operations over the captured `SlotState`, the proposed addition and the selected target. Use the rounding/composition policy of the contract. The UI, candidate record and transaction must use the same calculated values; do not display one total and save another.

- Original occupied target: present D9's addition confirmation. No write before the confirmation; create one consolidated record with the preserved base plus the addition.
- Different empty target: record only the addition, leaving the source untouched. A skipped target clears its skip with the existing reversible receipt semantics.
- Different occupied target: capture that target, calculate its base plus the same addition and show the confirmation for that destination. Nothing is written on merely selecting it.
- No initial target: open the addition destination selector without an arbitrary preselection. Once chosen, use the same empty/skipped/occupied paths.
- Description composition retains existing text as the contract requires. Consume S18's increased normal-description bound and composed-addition exception consistently across DTOs, local validation, Room and outgoing DAY. Count Unicode code points as the server does, rather than Kotlin UTF-16 code units. If complete composition exceeds the supported bound, do not save a truncated meal or silently replace it; leave the proposal unrecorded with a short explanation.

Before confirming, recheck date, wipe boundary, slot availability and every captured state on which the proposal depends. The transaction must also validate the original source when rerouting an addition derived from it, even though that source is not mutated. Extend the repository's read-condition validation within `apps/android/` if necessary; never simulate a check by rewriting the untouched source. A mismatch expires the proposal and writes nothing. Double taps, recreation, retry and repeated confirmation cannot apply the same proposal twice.

Use `DayRepository.commitRecord` and the existing receipt machinery for atomic writes. A transaction changes only its actual target, clears the relevant skip, consumes the proposal and inserts the receipt together. Preserve full before/after states for Desfazer.

### 3. Keep revisions and whole-meal moves distinct

A pending revision is bound to its source. Confirm with the original state check; Cancelar marks it not recorded and leaves Room/memory unchanged. It cannot be redirected as if it were a new addition.

The receipt's **Trocar refeição** keeps its existing whole-record meaning and atomic source/target operation, including confirmation for an occupied destination. It must never use the addition-only payload. Legacy pending replacements keep their legacy behavior; no silent reinterpretation of stored proposals.

After an addition, Desfazer restores exactly the earlier target and its skip/memory state. After rerouting, it does not delete or recreate the original source. Excluir still excludes the consolidated target record; Editar and routine memory retain their existing boundaries. Apply routine updates only after the successful transaction and for the final meal; never reinforce the abandoned source or apply the routine twice. Immediate preference/portion updates retain existing rules, except a rejected malformed response must not introduce them.

### 4. Render the approved Chat states

Implement only D9's exported states with Aero and the existing Chat component hierarchy. The addition summary names the new foods/quantities, labels the added kcal, shows the chosen target's previous amount and labels its resulting kcal/P/C/G. It must remain understandable without interpreting a large unlabeled number or reading the model's prose.

On destination selection, the actionable summary/confirmation reflects that destination and its computed values. Historical reply text is not evidence that data was saved; the local receipt is. Keep a single active proposal and no obsolete buttons pointing at a previous destination. New send, day change, wipe and conflicting writes expire it consistently.

Preserve scroll behavior, keyboard handling, photo attachment/retry behavior, clarification rounds, accessibility semantics, text scaling, memory notices and receipt availability. Do not remove existing actions from ordinary new meals or whole-meal moves. No new navigation route, settings toggle or Home redesign.

### 5. Intended specification changes

At Completion, update [product Chat](../../produto/specifications/chat.md) with the new-capability presentation, destination semantics, proposal expiry, confirmations and D9's states. Update [Room](../specifications/room-v2.md) with the migration, versioned payload and transaction read checks. Add Provenance links. The gold inventory remains the owner of ids/sources; do not repeat it in a new Android index. Preserve compatibility notes while older responses can still appear in local history.

## Out of scope

Server/prompt or infrastructure edits, Figma/gold edits, A43 migration work, model changes, nutrition lookup, retroactive recording, rewriting historical data, a meal-item database redesign, automatic occupied-slot confirmation or distribution outside the existing dev process.

## Validation

1. DTO/domain tests: new and old response shapes; invalid operation/base/total; round-once arithmetic; occupied, empty, skipped and unknown targets; revisions and removals; long descriptions; unresolved operation. Verify complete description round trips at S18's normal and composed boundaries, including supplementary Unicode characters, without a residual legacy cut in persistence or prompt building. No prose parsing in the record path.
2. ViewModel and real Room transaction tests: source unchanged on reroute; correct destination amount; destination occupied confirmation; original source or target changed while awaiting response/confirmation; day rollover, wipe and removed slot; duplicate tap/retry; process recreation with a persisted proposal; malformed/null payloads. A numeric equality alone is not enough: compare food text, record counts and every affected slot.
3. Migration from the then-current schema with messages, meal logs and active receipts. Existing undo continues to work. For additions test Desfazer, Excluir, Editar, routine application/reversion and whole-meal receipt movement separately. Do not reuse a whole-meal move test as proof of addition rerouting.
4. Use synthetic fixtures reflecting S18's scenarios, including its follow-up dinner incident. Through a fake service, assert exact before/addition/after numbers and negative side effects. Include an occupied dinner containing a drink, a pending food addition and a follow-up confirming dinner/repeating quantities: preserve the original base until confirmation, capture the pending addition before action expiry, then save base plus delta exactly once. The unrelated lunch and snack stay unchanged. Assert that the receipt, persisted description and next outgoing DAY agree; the rejected proposal must not appear as eaten in the next prompt. Through dev API, verify at least one photo addition and one text revision on the matching server; capture request ids without committing user content. Nutrition estimates need not repeat identically; ledger arithmetic must.
5. Run `testDevDebugUnitTest`, `verifyRoborazziDevDebug` and `assembleDevRelease` using the repository's dev setup. No baseline refresh to hide a failing contract or visual mismatch.
6. Visual QA: matching D9 golds and fresh emulator captures in both themes; written diff of layout, tokens, type, radius, CTA/ButtonGroup and semantic macros. Also regress the existing normal-estimate, photo, receipt, undo and whole-meal move states touched by the code. Use the repository's current capture tooling or `android` CLI, keeping captures in `docs/qa/android/current/{theme}/`. Iterate until the app matches each inventory gold.
7. Owner device check: distinguish the added item from the meal total; confirm an addition; route another addition to empty and occupied destinations; cancel a correction; undo and move a recorded whole meal. Record actual execution separately from automated tests. If a dev test build is requested, use `tools/distribute-dev.ps1 -Notes <scratchpad-notes.md>`; no manual version edits.
8. `node tools/check-docs.mjs` passes. Follow SDD git delivery. Keep the plan pending manual validation until its required device checks are recorded; automated success alone is not completion.

## Results

Planning only. No Android changes, migration, test build, device checks or emulator comparisons were performed for this plan. Fill with the actual validation and any remaining manual gate during delivery.
