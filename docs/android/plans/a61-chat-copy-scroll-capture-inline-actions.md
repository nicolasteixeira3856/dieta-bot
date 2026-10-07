# Plan — A61 Chat actions in the thread, copying messages, scrolling screenshot

- Status: Aguardando aprovação
- Date: 07/10/2026
- Owning context: `android`
- Executable boundary: `apps/android/` only, in three parts on one branch: (A) Chat actions in the thread plus the `app gap` items D20 hands over; (B) selectable messages; (C) scrolling screenshot on every scrollable product screen. Plus the QA tooling `tools/capture-chat.sh`, a new `tools/capture-scroll.sh`, `tools/diff-gold.mjs` and `GoldTest`. No server change, no Room change.
- Related documentation: [ADR-048](../../produto/adrs/ADR-048-chat-actions-in-thread-copy-scroll-capture.md), [product Chat](../../produto/specifications/chat.md), [home-timeline](../../produto/specifications/home-timeline.md), [memoria-push](../../produto/specifications/memoria-push.md), [perfil-onboarding](../../produto/specifications/perfil-onboarding.md), [QA](../../qa/README.md).
- Prerequisites:
  - [D20](../../design/plans/d20-figma-review-inline-actions.md) `Concluído` (golds `chatQ`, `chatE`, `chatR`, `chatRB`, `chatRK`, `chatRL` with the actions in the thread, plus its corrections) before part A starts; parts B and C need no gold and may run first on the same branch;
  - [A60](pending_manual_validation/a60-tone-formatting-planned-skips.md) merged (it is); no parallel Android plan.

Authorization and delivery follow [SDD](../../sdd/README.md). Approval: `Aprovo o plano docs/android/plans/a61-chat-copy-scroll-capture-inline-actions.md. Implemente o plano aprovado.`

## Objective

Three defects the owner reported on 0.0.18-dev (07/10/2026), fixed in one dev build: the Chat buttons scroll with their message instead of staying pinned above the composer, both sides of the conversation can be copied, and the phone's scrolling screenshot works again on every long screen and stays covered by an automated check.

## Delivery

- One branch from master; parts in the order B → C → A (A waits for D20); one commit or more per part; one PR at the end.
- A part whose validation fails twice is left out: the PR merges with the delivered parts, the plan stays `Em implementação`, Results list the missing part and what it needs.
- One dev build at the end with `tools/distribute-dev.ps1 -Notes` (pt-BR notes per part delivered).
- Visual QA per AGENTS for the frames part A touches; partial validation (only the flows this plan touches).

## Scope

### Part A — Chat actions in the thread (ADR-048 decision 1, D20)

- The `Footer` of `feature/chat/ChatScreen.kt` keeps only the composer. The action stack (`ForceBar`, `RegisterBar`, `PlanBar` + Reservar, `AeroChoiceBar`) becomes a thread element right under the message it belongs to: the latest question (Forçar estimativa) or the latest estimate/plan (Registrar, Registrar assim, Reservar, the pills), after its `BudgetLines` or `ReservedLabel`, with the gaps of the D20 golds. It is part of the same `LazyColumn` item as its message (or the item right after it with a stable key), so it scrolls, recomposes and keeps its tags (`chat-record-plan`, `chat-reserve`, `chat-budget-over-ok`, `chat-budget-fit`, `chat-register`, `chat-force-estimate`).
- What shows, when it shows and when it expires stays as today (`ChatUiState.actions`, `forceEstimate`): one stack at a time, for the latest answer only; the next send, the day change and a wipe remove it.
- Opening the Chat and every new answer still scroll the thread to its end so the latest answer and its actions are visible; with the keyboard open, the thread end stays above the composer.
- `app gap` items listed by D20's sweep and accepted by the owner in the D20 review are implemented here, limited to the frames D20 changed; anything else goes to a new plan.
- Specification change (written at Completion): [Chat](../../produto/specifications/chat.md) rules for Registrar (rule 5/ADR-028), Registrar assim, Reservar and the choice (rule 16), Forçar estimativa (ADR-026): "below the answer, in the thread" instead of the actions slot; the acceptance list gains "the actions scroll with their message".

### Part B — Copying messages (ADR-048 decision 2)

- The user bubble text and the whole Tali bubble (paragraphs, bullets, steps, the portions table, the estimate card and day panel numbers) sit in a `SelectionContainer` per bubble: long press selects, the platform toolbar offers Copiar and Selecionar tudo. Receipts, cards (`chatS`, `chatU`, `chatSD`, addition/revision cards), chips, buttons, the greeting and the meta card are not selectable (`DisableSelection` where they share a container).
- Formatted replies copy their rendered text (no `**`, no `|`); bullet and step markers are copied as shown.
- No gesture conflict: the thread keeps scrolling with a drag that starts on a bubble; a tap on a bubble keeps doing nothing; links do not exist in bubbles. Selection clears on send and when the item scrolls out.
- No telemetry with text; no new event.
- Specification change: [Chat](../../produto/specifications/chat.md) rule 2 gains "the text of every bubble can be selected and copied with the platform selection".

### Part C — Scrolling screenshot (ADR-048 decision 3)

- **Diagnosis first:** on the emulator (API 34+, the AVD of the captures) and with Compose's own scroll-capture search (`View.dispatchScrollCaptureSearch` over the root `ComposeView`), find for Home, Chat, Config and each onboarding step whether a scroll-capture target exists and whether "Capturar mais" works. Bisect the cause with the dev tags (`dev-v0.0.N`) or the commits of the Aero migration (A39–A45) and later. Candidate causes to rule in or out: the page background and edge bubbles drawn outside the scroll container (`AeroPage`), the Haze glass layers and `Modifier.blur`, the Chat `LazyColumn` with `reverseLayout`, a parent clip or `graphicsLayer` that hides the scrollable node from the search, and the Compose BOM in use. The root cause is recorded in Results before the fix.
- **Fix:** the smallest change that gives each screen one scroll-capture target that covers its scrollable content, keeping the golds unchanged (the capture shows the page as it looks); no dependency upgrade unless the diagnosis proves the BOM is the cause, and then only within the current stack pins.
- **Gate:** a Robolectric test (`ScrollCaptureTest`, API 34) asserts one target per screen with bounds inside the window and a scrollable range; if Robolectric cannot run the framework search, the emulator script below is the gate and Results say so.
- Specification change: [home-timeline](../../produto/specifications/home-timeline.md), [Chat](../../produto/specifications/chat.md), [memoria-push](../../produto/specifications/memoria-push.md) (Config) and [perfil-onboarding](../../produto/specifications/perfil-onboarding.md) gain "the screen offers the system scrolling screenshot (Android 12+)" in their acceptance criteria.

## Out of scope

- Server, Room, copy texts, new screens or states (a "Copiar" menu, a toast of our own; the system clipboard confirmation is the platform's). iOS and Android below 12 for the scrolling screenshot (the system feature does not exist there). Any layout change not drawn by D20.

## Validation

- B: unit/Compose tests: a user bubble and a formatted Tali reply are inside a selection container (semantics), receipts and cards are not; `plain()` of the rendered reply equals the selected-all text of a fixture.
- B: emulator, dev flavor, against the fake server (`SCENES=a61` in `tools/capture-chat.sh`): long press on a user bubble and on a formatted reply shows the system Copiar; Selecionar tudo + Copiar, then a long press in the composer and Colar puts the same text in the composer (no `**`); a drag that starts on a bubble still scrolls the thread.
- C: the diagnosis and root cause in Results; `ScrollCaptureTest`; `tools/capture-scroll.sh dark|light` on the emulator: for Home (with closure cards and a full timeline), Chat (a thread longer than the screen) and Config, it takes the system screenshot (`KEYCODE_SYSRQ`), finds and taps "Capturar mais" / "Capture more" in the system UI and checks the long-screenshot editor opens; one exported long screenshot per screen kept in the scratchpad (not committed).
- A: `GoldTest` and Roborazzi for the six D20 Chat golds, dark and light; emulator captures of `chatQ`, `chatE`, `chatR`, `chatRB`, `chatRK`, `chatRL` in `docs/qa/android/current/{dark,light}/` compared with the D20 golds under the QA rules of AGENTS, with a written diff list; a scene check that the action stack moves with the thread (bounds of `chat-record-plan` change after a swipe while the composer's stay); regress `chatG`, `chatU` (receipt actions already in the thread).
- A: the D20 `app gap` items, each with its capture or test.
- Whole plan: `testDevDebugUnitTest`, `verifyRoborazziDevDebug` and `assembleDevRelease` pass; `node tools/check-docs.mjs` passes; the dev build distributed with its version in Results.
- Manual acceptance (after delivery, [autonomous run](../../sdd/autonomous-run.md) rules if run unattended): owner or tester on a device with the new dev build: copy a reply and paste it in another app; take a scrolling screenshot of Home and of a long Chat; scroll a plan's actions away and back.

## Results

Planning only.
