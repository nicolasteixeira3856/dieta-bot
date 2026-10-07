# Plan — A61 Chat actions in the thread, copying messages, scrolling screenshot

- Status: Aguardando aprovação
- Date: 07/10/2026
- Owning context: `android`
- Executable boundary: `apps/android/` only, in three parts on one branch: (A) Chat actions in the thread plus the `app gap` items D20 hands over; (B) copying messages the WhatsApp way (selection bar with Copiar); (C) scrolling screenshot on every scrollable product screen. Plus the QA tooling `tools/capture-chat.sh`, a new `tools/capture-scroll.sh`, `tools/diff-gold.mjs` and `GoldTest`. No server change, no Room change.
- Related documentation: [ADR-048](../../produto/adrs/ADR-048-chat-actions-in-thread-copy-scroll-capture.md), [product Chat](../../produto/specifications/chat.md), [home-timeline](../../produto/specifications/home-timeline.md), [memoria-push](../../produto/specifications/memoria-push.md), [perfil-onboarding](../../produto/specifications/perfil-onboarding.md), [QA](../../qa/README.md).
- Prerequisites:
  - [D20](../../design/plans/pending_manual_validation/d20-figma-review-inline-actions.md) `Concluído` (golds `chatQ`, `chatE`, `chatR`, `chatRB`, `chatRK`, `chatRL` with the actions in the thread, the new `chatCP` and `chatCC`, plus its corrections) before parts A and B start; part C needs no gold and may run first on the same branch;
  - [A60](pending_manual_validation/a60-tone-formatting-planned-skips.md) merged (it is); no parallel Android plan.

Authorization and delivery follow [SDD](../../sdd/README.md). Approval: `Aprovo o plano docs/android/plans/a61-chat-copy-scroll-capture-inline-actions.md. Implemente o plano aprovado.`

## Objective

Three defects the owner reported on 0.0.18-dev (07/10/2026), fixed in one dev build: the Chat buttons scroll with their message instead of staying pinned above the composer, both sides of the conversation can be copied, and the phone's scrolling screenshot works again on every long screen and stays covered by an automated check.

## Delivery

- One branch from master; parts in the order C → A → B (A and B wait for D20); one commit or more per part; one PR at the end.
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

### Part B — Copying messages, as in WhatsApp (ADR-048 decision 2, D20 `chatCP`, `chatCC`)

- **Selection state** in `ChatViewModel`/`ChatUiState`: the ids of the selected messages (only user and Tali text bubbles; a photo bubble only when it has a caption). Long press on a bubble (platform long-press haptic, accessibility label `Selecionar mensagem`) starts the selection with that message; while selecting, a tap on a text bubble toggles it; removing the last one, ✕ or the system back (`BackHandler`) ends it; sending a message, the day change, a wipe and leaving the Chat end it. Survives recreation (saved state), not process death.
- **UI:** the selected row gets the D20 highlight across the thread width; the header is replaced by the D20 `Chat/SelectionBar` (`AeroSelectionBar` in `core/designsystem/aero`: ✕, the count, `Copy` icon button labelled **Copiar**). Receipts, cards (`chatS`, `chatU`, `chatSD`, addition/revision cards), chips, buttons, the greeting and the meta card ignore the long press and the taps of the selection. Taps on actions under a selected message keep working and end the selection first.
- **Copiar:** the selected messages in conversation order, separated by a blank line; a user message as typed; a Tali message as `ReplyMarkup.plain()` of its text (no `**`; table rows `{item}: {gramas}`; steps keep their number; bullets as `- `), without the estimate card, the day panel, times or the budget lines; a photo bubble's caption. `ClipboardManager` with a plain-text clip labelled `Fibrai`; then the selection ends. Confirmation: Android 13+ nothing of our own (the system overlay shows it); Android 12 and earlier the D20 `chatCC` pill (`Mensagem copiada` / `{n} mensagens copiadas`, about 2 s, above the composer, announced to accessibility).
- **Telemetry** (dev): `message_copied` with `count` and `has_user` / `has_tali` booleans; never text.
- Specification change: [Chat](../../produto/specifications/chat.md) rule 2 gains the selection and Copiar behaviour above; the states list gains `chatCP` and `chatCC`; acceptance gains "a long press selects a message; Copiar copies one or several messages as plain text".

### Part C — Scrolling screenshot (ADR-048 decision 3)

- **Diagnosis first:** on the emulator (API 34+, the AVD of the captures) and with Compose's own scroll-capture search (`View.dispatchScrollCaptureSearch` over the root `ComposeView`), find for Home, Chat, Config and each onboarding step whether a scroll-capture target exists and whether "Capturar mais" works. Bisect the cause with the dev tags (`dev-v0.0.N`) or the commits of the Aero migration (A39–A45) and later. Candidate causes to rule in or out: the page background and edge bubbles drawn outside the scroll container (`AeroPage`), the Haze glass layers and `Modifier.blur`, the Chat `LazyColumn` with `reverseLayout`, a parent clip or `graphicsLayer` that hides the scrollable node from the search, and the Compose BOM in use. The root cause is recorded in Results before the fix.
- **Fix:** the smallest change that gives each screen one scroll-capture target that covers its scrollable content, keeping the golds unchanged (the capture shows the page as it looks); no dependency upgrade unless the diagnosis proves the BOM is the cause, and then only within the current stack pins.
- **Gate:** a Robolectric test (`ScrollCaptureTest`, API 34) asserts one target per screen with bounds inside the window and a scrollable range; if Robolectric cannot run the framework search, the emulator script below is the gate and Results say so.
- Specification change: [home-timeline](../../produto/specifications/home-timeline.md), [Chat](../../produto/specifications/chat.md), [memoria-push](../../produto/specifications/memoria-push.md) (Config) and [perfil-onboarding](../../produto/specifications/perfil-onboarding.md) gain "the screen offers the system scrolling screenshot (Android 12+)" in their acceptance criteria.

## Out of scope

- Server, Room. Other actions in the selection bar (reply, delete, forward, star). Partial copy of a message (text handles). New screens or states other than `chatCP` and `chatCC`. iOS and Android below 12 for the scrolling screenshot (the system feature does not exist there). Any layout change not drawn by D20.

## Validation

- B: ViewModel tests: long press starts the selection, taps toggle, the last removal, ✕, back, send, day change and wipe end it; receipts and cards cannot be selected; the copied text of a user message, a formatted reply (fixture with bold, bullets, steps and a table), a photo caption and two messages in order; recreation keeps the selection; `message_copied` params. `GoldTest` and Roborazzi for `chatCP` and `chatCC`, dark and light.
- B: emulator, dev flavor, against the fake server (`SCENES=a61` in `tools/capture-chat.sh`): long press on a user bubble shows the selection bar with `1`; a tap on a formatted reply makes it `2`; a tap on a receipt changes nothing; **Copiar** ends the selection and a long press in the composer + Colar puts the expected plain text there (no `**`); captures of `chatCP` and of `chatCC` (the emulator AVD runs Android 13+: `chatCC` is captured from a debug render or an API 32 AVD, and Results say which) compared with the D20 golds under the QA rules of AGENTS, with a written diff list; back and ✕ end the selection.
- C: the diagnosis and root cause in Results; `ScrollCaptureTest`; `tools/capture-scroll.sh dark|light` on the emulator: for Home (with closure cards and a full timeline), Chat (a thread longer than the screen) and Config, it takes the system screenshot (`KEYCODE_SYSRQ`), finds and taps "Capturar mais" / "Capture more" in the system UI and checks the long-screenshot editor opens; one exported long screenshot per screen kept in the scratchpad (not committed).
- A: `GoldTest` and Roborazzi for the six D20 Chat golds, dark and light; emulator captures of `chatQ`, `chatE`, `chatR`, `chatRB`, `chatRK`, `chatRL` in `docs/qa/android/current/{dark,light}/` compared with the D20 golds under the QA rules of AGENTS, with a written diff list; a scene check that the action stack moves with the thread (bounds of `chat-record-plan` change after a swipe while the composer's stay); regress `chatG`, `chatU` (receipt actions already in the thread).
- A: the D20 `app gap` items, each with its capture or test.
- Whole plan: `testDevDebugUnitTest`, `verifyRoborazziDevDebug` and `assembleDevRelease` pass; `node tools/check-docs.mjs` passes; the dev build distributed with its version in Results.
- Manual acceptance (after delivery, [autonomous run](../../sdd/autonomous-run.md) rules if run unattended): owner or tester on a device with the new dev build: copy a reply and paste it in another app; take a scrolling screenshot of Home and of a long Chat; scroll a plan's actions away and back.

## Results

Planning only.
