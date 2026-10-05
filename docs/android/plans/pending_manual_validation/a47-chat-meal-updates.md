# Plan — A47 Chat meal updates

- Status: Pendente aprovação manual
- Date: 04/10/2026
- Owning context: `android`
- Executable boundary: `apps/android/` only: Chat UI/ViewModel/recorder, domain update rules, network DTOs/prompt building, Room entities/migration and relevant tests.
- Related documentation: product Chat, Room specification, Android validation and plan/index lifecycle. Fresh emulator captures belong in the existing QA folders.
- Prerequisites: acceptance of [ADR-032](../../../produto/adrs/ADR-032-acrescimos-e-correcoes-de-refeicoes.md); [S18](../../../server/plans/completed/s18-meal-additions-and-revisions.md) delivered and available on the dev server; [D9](../../../design/plans/completed/d9-chat-meal-updates.md) `Concluído` with exported golds; [A43](../completed/a43-chat-records-memory-aero.md) delivered.

Authorization and delivery follow [SDD](../../../sdd/README.md). Approval: `Aprovo o plano docs/android/plans/a47-chat-meal-updates.md. Implemente o plano aprovado.`

## Objective

Show and record the requested addition without changing previously recorded food or copying it into another meal. Keep revisions explicit, preserve undo, and render the distinction between the item amount and the whole-meal total from structured data.

## Sources

ADR-032 owns the behavior/copy. S18 owns the proposed capability; after its delivery use the [HTTP contract](../../../api-contract.md). [Product Chat](../../../produto/specifications/chat.md), [Room](../../specifications/room-v2.md), the [gold inventory](../../../qa/README.md#golds) and D9 own the live implementation and visual boundaries.

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

At Completion, update [product Chat](../../../produto/specifications/chat.md) with the new-capability presentation, destination semantics, proposal expiry, confirmations and D9's states. Update [Room](../../specifications/room-v2.md) with the migration, versioned payload and transaction read checks. Add Provenance links. The gold inventory remains the owner of ids/sources; do not repeat it in a new Android index. Preserve compatibility notes while older responses can still appear in local history.

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

Approved by the owner on 2026-10-05 ("Aprovo o plano docs/android/plans/a47-chat-meal-updates.md. Implemente o plano aprovado."). Prerequisites checked at start: ADR-032 `Accepted`; S18, D9 and A43 in `completed/`; S18 deployed on the dev server (verified below). Implemented on `feat/a47-chat-meal-updates` from the current master (`49cd314`).

### Delivered (`apps/android/` only)

- **Wire.** `ChatIn.meal_changes: true` on every normal turn (no default, always encoded) and `pending_addition` in the S18 shape; a compact request drops it. `ChatOut.meal_change` stays a raw `JsonElement`: absent (legacy server) and explicit null are distinct, and malformed metadata never fails the whole answer.
- **Domain** (`domain/MealChanges.kt`). Strict parse (exact keys, JSON numbers only, finite and nonnegative, round once with ties upward, item kcal summing to the delta, a caloric input never rounding to zero, 1–100 items, 500-code-point descriptions). The proposal is checked against the captured DAY states: target in today's slots, base equal to the occupied target, composed text (`base; addition`) and every total equal to base + delta; revision and new-meal bounds. Composition counts code points and returns nothing past 2000 instead of cutting. `MealProposal` (version 1) keeps operation, day, latest wipe id, source and target states, the addition and a picked destination with its state.
- **Room v10.** Nullable `chat_message.mealChange`, `MIGRATION_9_10` (one `ADD COLUMN`), exported `10.json`. `recordState` gains `pending_add` / `pending_revise`. `commitRecord` takes a `RecordGuard` checked in the same transaction (request day = today, latest wipe unchanged, read-only source slots unchanged, answer still undecided); `closeOpenRecord` and `setMealChange` change only undecided rows.
- **ViewModel / recorder.** Slot states and the wipe id are captured with the request; the proposal is stored with the answer before any record. Stale on arrival (day, wipe or source changed) → `Não registrado`, no routine. Malformed/contradictory → no card, `Não registrado`, no memory updates. Addition to a meal with a record → `pending_add` (chatI) whatever `record` says; Adicionar composes with the same function the card uses. Escolher outra refeição → picker in addition mode; empty/skipped meal records only the addition with the source as a read-only check; another occupied meal → `pending_add` with that meal's state, nothing written; the source returns to its own confirmation; overflow → note, nothing written. Revision → `pending_revise` (chatIC); Atualizar replaces the unchanged source; Cancelar → `Não registrado`. New send, day change, wipe and changes by another path expire the proposal; one tap at a time per answer. The immediately preceding open addition is sent back as `pending_addition` (taken before the send expires it, rechecked against DAY, kept on retry). Receipts reuse A34 (`replaced` / `logged`, Desfazer, Excluir, Editar, Trocar refeição on the consolidated record); the answer's routine applies once to the meal that received it. Telemetry `meal_update` (`op`, `action`, `reason` enums only).
- **UI (Aero).** `Chat/Estimate` Kind=Addition (`+{kcal}`, `+` macros) and Kind=Revision (`NOVO TOTAL`, no `~`); addition and revision answers show no prose (D9). `AdditionCard` (chatI) and `RevisionCard` (chatIC) below the answer; the meal picker in addition mode (chatTI) with title, subtitle, the added food box, nothing picked and no `(atual)`. No new route, setting or Home change.

### Automated validation

- `:app:testDevDebugUnitTest`: **548 tests, 545 passed, 3 failed**. The 3 failures are GoldTest `chatF_dark`, `chatF_light` (region 3.64 % / 2.90 %) and `chatL_dark` (ink 1.258): the same 3 GoldTests fail on clean `master` (checked with this delivery stashed); they follow the D10 Tali golds that [A49](../a49-fibrai-tali-visible-rename.md) implements. No baseline or gold was changed.
- `:app:verifyRoborazziDevDebug`: Roborazzi comparisons pass; the task reports the same 3 GoldTest failures above. `:app:assembleDevRelease`: built `app-dev-release.apk`.
- New tests: `MealChangesTest` (12: absent/null/object, 16 malformed shapes, rounding and alcohol, 500/501 code points with supplementary characters, contradicting total/text/base incl. the lunch-into-dinner incident, empty/skipped/unknown targets, revision/new checks, 2000/2001 composition, staleness), `ChatMealUpdatesTest` (23, fake service + real Room: base + delta exactly once with double/repeated taps; reroute to empty, skipped and occupied meals with Excluir/Desfazer; source back to its own confirmation; source and destination changed while waiting, and just before the transaction; answer arriving after a change; wipe and day rollover; malformed and contradicting metadata; legacy server keeps chatU; revision Cancelar/Atualizar; the follow-up dinner incident with a drink, a similar lunch and repeated quantities; `pending_addition` on retry and not after record or a source change; recreation; Editar and Trocar refeição on the consolidated record; routine to the final meal; unknown target; overflow note; 2000-code-point round trip into the next DAY), `DayRepositoryTest` guard, `MigrationV9V10Test` (v9 file with messages, logs, an open replace and an active receipt; Desfazer works after migration), `PromptBuilderTest` (wire shape, compact drop), `ChatRecordUiTest` (3), GoldTest `chatI`, `chatIC`, `chatTI` in both themes.

### Visual QA

JVM GoldTest against the D9 golds (gold fixture data, blurred gate 2 %): `chatI` dark 1.00 % / light 0.76 %, `chatIC` 0.88 % / 0.60 %, `chatTI` 0.41 % / 0.84 %. Existing Chat golds unchanged (`chatE`, `chatT`, `chatU`, `chatG`, `chatD`, `chatQ`, `chatM`, `chatR`, `chatS`, `chatA`, `chatX` pass).

Emulator (`Medium_Phone`, 780×1688 @ 320 dpi, dev debug APK against the dev server, live answers): fresh captures `docs/qa/android/current/{dark,light}/chatI.png`, `chatTI.png`, `chatIC.png`. `tools/diff-gold.mjs` full-frame numbers are not a gate for these frames: the live foods and numbers differ from the fictional gold data and the 930 dp `chatI` frame is taller than the screen (`chatTI` dark 0.56 %, light 2.72 %; `chatI`, `chatIC` content differs). Measured instead (px @2x, capture vs gold):

| Check | Result |
|---|---|
| Layout | chatI: bubble with the Addition estimate, then the AddPending card, then the composer; chatIC: Revision estimate then RevisePending card; chatTI: Sheet/Bottom over the blurred thread. Same order and alignment as the golds. |
| Card size | chatI card 700 × 590 vs 700 × 586; chatIC card 700 × 386 vs 700 × 386; chatTI addition box 678 × 142 vs 678 × 145 (one-line vs two-line food). |
| Tokens | glass card, `surface/2` estimate and sheet box, `text/muted` labels, `text/primary` values, accent only on the primary CTA and the picked row. |
| Type size | Body labels, Body/Strong totals, Title `+kcal`, Caption/Strong macros, Label/Section `NOVO TOTAL`: as gold. |
| Radius | card 20, pills 50 %, sheet 28: as gold. |
| ButtonGroup / CTA | Adicionar (Button/Primary, 58) over Escolher outra refeição (outline 58), 10 dp apart; Atualizar | Cancelar side by side (162 : 139). As gold. |
| Semantic macros | P/C/G boxes and the `P · C · G` total line in the macro colours. |
| Timeline | not touched. |

Ignored per AGENTS: status bar, gesture nav inset (the sheet sits higher by it), font raster. Pre-existing difference owned by A49: the header still reads "Chat Dieta Bot" / bot label "Dieta Bot AI" where the golds show Tali. Other Chat flows were not re-captured on the emulator (partial-validation rule): their code paths are covered by the unchanged GoldTests and the VM tests.

### Dev API verification (2026-10-05, synthetic input, matching S18 server)

Request ids from the dev conversation log (only ids, operation and outcome read; no content copied):

- `70da475e-cf38-4fd1-b9a7-afac105080aa` — dinner as a new meal, recorded by itself (637 kcal).
- `f052d420-68c3-4573-a8a8-d5e3932c5b2e` — **text addition** to the dinner: chatI showed 637 → 887 kcal; Escolher outra refeição → Lanche recorded only the addition (250 kcal); the dinner stayed 637 in Room.
- `e8e54c12-6faf-4c2c-847e-13ede405ef10` — **text revision** of the dinner: chatIC 637 → 960; Cancelar left Room unchanged (`not_recorded`).
- `089b837c-7cec-41c1-8685-d231281099a4` — **photo addition** to the Lanche: Adicionar recorded 250 + 670 = 920 kcal (61P · 111C · 24G), source `photo`, receipt `250 → 920 kcal` without Editar; Desfazer restored 250 kcal.
- Server findings for S19/server follow-up (no client change): the revision `e8e54c12` re-estimated the dinner including the pudding that had been rerouted to the Lanche (the client showed it as a revision and nothing was written); two photo-addition turns to the dinner (`73a51107-1952-4b29-bf31-8e9554e95294`, and the text follow-up `109429f3-d641-493b-ab7b-7b7c4d85335f`) and one earlier text turn (`2a6604a5-993a-43f6-bb17-237da60223e2`) ended in the server's safe fallback (`ValueError`); the app showed the fallback and recorded nothing.

### Pending manual validation

Owner device check (Validation 7), not performed: distinguish the added item from the meal total; confirm an addition; route another addition to an empty and to an occupied meal; cancel a correction; undo and move a recorded whole meal. A dev test build was not requested and not distributed. The plan stays in `pending_manual_validation/` until the owner records that check.

### Documentation

[Product Chat](../../../produto/specifications/chat.md) rule 22, states and acceptance criteria; [Room](../../specifications/room-v2.md) v10 rule 7 and migration; Provenance in both. Plan moved to `pending_manual_validation/` and incoming links updated. No ADR, gold, token or server change.
