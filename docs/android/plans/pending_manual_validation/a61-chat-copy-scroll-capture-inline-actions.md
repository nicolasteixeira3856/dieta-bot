# Plan — A61 Chat actions in the thread, copying messages, scrolling screenshot

- Status: Pendente aprovação manual (aprovado e implementado 07/10/2026)
- Date: 07/10/2026
- Owning context: `android`
- Executable boundary: `apps/android/` only, in three parts on one branch: (A) Chat actions in the thread plus the `app gap` items D20 hands over; (B) copying messages the WhatsApp way (selection bar with Copiar); (C) scrolling screenshot on every scrollable product screen. Plus the QA tooling `tools/capture-chat.sh`, a new `tools/capture-scroll.sh`, `tools/diff-gold.mjs` and `GoldTest`. No server change, no Room change.
- Related documentation: [ADR-048](../../../produto/adrs/ADR-048-chat-actions-in-thread-copy-scroll-capture.md), [product Chat](../../../produto/specifications/chat.md), [home-timeline](../../../produto/specifications/home-timeline.md), [memoria-push](../../../produto/specifications/memoria-push.md), [perfil-onboarding](../../../produto/specifications/perfil-onboarding.md), [QA](../../../qa/README.md).
- Prerequisites:
  - [D20](../../../design/plans/completed/d20-figma-review-inline-actions.md) `Concluído` (golds `chatQ`, `chatE`, `chatR`, `chatRB`, `chatRK`, `chatRL` with the actions in the thread, the new `chatCP` and `chatCC`, plus its corrections) before parts A and B start; part C needs no gold and may run first on the same branch;
  - [A60](a60-tone-formatting-planned-skips.md) merged (it is); no parallel Android plan.

Authorization and delivery follow [SDD](../../../sdd/README.md). Approval: `Aprovo o plano docs/android/plans/a61-chat-copy-scroll-capture-inline-actions.md. Implemente o plano aprovado.`

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
- Specification change (written at Completion): [Chat](../../../produto/specifications/chat.md) rules for Registrar (rule 5/ADR-028), Registrar assim, Reservar and the choice (rule 16), Forçar estimativa (ADR-026): "below the answer, in the thread" instead of the actions slot; the acceptance list gains "the actions scroll with their message".

### Part B — Copying messages, as in WhatsApp (ADR-048 decision 2, D20 `chatCP`, `chatCC`)

- **Selection state** in `ChatViewModel`/`ChatUiState`: the ids of the selected messages (only user and Tali text bubbles; a photo bubble only when it has a caption). Long press on a bubble (platform long-press haptic, accessibility label `Selecionar mensagem`) starts the selection with that message; while selecting, a tap on a text bubble toggles it; removing the last one, ✕ or the system back (`BackHandler`) ends it; sending a message, the day change, a wipe and leaving the Chat end it. Survives recreation (saved state), not process death.
- **UI:** the selected row gets the D20 highlight across the thread width; the header is replaced by the D20 `Chat/SelectionBar` (`AeroSelectionBar` in `core/designsystem/aero`: ✕, the count, `Copy` icon button labelled **Copiar**). Receipts, cards (`chatS`, `chatU`, `chatSD`, addition/revision cards), chips, buttons, the greeting and the meta card ignore the long press and the taps of the selection. Taps on actions under a selected message keep working and end the selection first.
- **Copiar:** the selected messages in conversation order, separated by a blank line; a user message as typed; a Tali message as `ReplyMarkup.plain()` of its text (no `**`; table rows `{item}: {gramas}`; steps keep their number; bullets as `- `), without the estimate card, the day panel, times or the budget lines; a photo bubble's caption. `ClipboardManager` with a plain-text clip labelled `Fibrai`; then the selection ends. Confirmation: Android 13+ nothing of our own (the system overlay shows it); Android 12 and earlier the D20 `chatCC` pill (`Mensagem copiada` / `{n} mensagens copiadas`, about 2 s, above the composer, announced to accessibility).
- **Telemetry** (dev): `message_copied` with `count` and `has_user` / `has_tali` booleans; never text.
- Specification change: [Chat](../../../produto/specifications/chat.md) rule 2 gains the selection and Copiar behaviour above; the states list gains `chatCP` and `chatCC`; acceptance gains "a long press selects a message; Copiar copies one or several messages as plain text".

### Part C — Scrolling screenshot (ADR-048 decision 3)

- **Diagnosis first:** on the emulator (API 34+, the AVD of the captures) and with Compose's own scroll-capture search (`View.dispatchScrollCaptureSearch` over the root `ComposeView`), find for Home, Chat, Config and each onboarding step whether a scroll-capture target exists and whether "Capturar mais" works. Bisect the cause with the dev tags (`dev-v0.0.N`) or the commits of the Aero migration (A39–A45) and later. Candidate causes to rule in or out: the page background and edge bubbles drawn outside the scroll container (`AeroPage`), the Haze glass layers and `Modifier.blur`, the Chat `LazyColumn` with `reverseLayout`, a parent clip or `graphicsLayer` that hides the scrollable node from the search, and the Compose BOM in use. The root cause is recorded in Results before the fix.
- **Fix:** the smallest change that gives each screen one scroll-capture target that covers its scrollable content, keeping the golds unchanged (the capture shows the page as it looks); no dependency upgrade unless the diagnosis proves the BOM is the cause, and then only within the current stack pins.
- **Gate:** a Robolectric test (`ScrollCaptureTest`, API 34) asserts one target per screen with bounds inside the window and a scrollable range; if Robolectric cannot run the framework search, the emulator script below is the gate and Results say so.
- Specification change: [home-timeline](../../../produto/specifications/home-timeline.md), [Chat](../../../produto/specifications/chat.md), [memoria-push](../../../produto/specifications/memoria-push.md) (Config) and [perfil-onboarding](../../../produto/specifications/perfil-onboarding.md) gain "the screen offers the system scrolling screenshot (Android 12+)" in their acceptance criteria.

## Out of scope

- Server, Room. Other actions in the selection bar (reply, delete, forward, star). Partial copy of a message (text handles). New screens or states other than `chatCP` and `chatCC`. iOS and Android below 12 for the scrolling screenshot (the system feature does not exist there). Any layout change not drawn by D20.

## Validation

- B: ViewModel tests: long press starts the selection, taps toggle, the last removal, ✕, back, send, day change and wipe end it; receipts and cards cannot be selected; the copied text of a user message, a formatted reply (fixture with bold, bullets, steps and a table), a photo caption and two messages in order; recreation keeps the selection; `message_copied` params. `GoldTest` and Roborazzi for `chatCP` and `chatCC`, dark and light.
- B: emulator, dev flavor, against the fake server (`SCENES=a61` in `tools/capture-chat.sh`): long press on a user bubble shows the selection bar with `1`; a tap on a formatted reply makes it `2`; a tap on a receipt changes nothing; **Copiar** ends the selection and a long press in the composer + Colar puts the expected plain text there (no `**`); captures of `chatCP` and of `chatCC` (the emulator AVD runs Android 13+: `chatCC` is captured from a debug render or an API 32 AVD, and Results say which) compared with the D20 golds under the QA rules of AGENTS, with a written diff list; back and ✕ end the selection.
- C: the diagnosis and root cause in Results; `ScrollCaptureTest`; `tools/capture-scroll.sh dark|light` on the emulator: for Home (with closure cards and a full timeline), Chat (a thread longer than the screen) and Config, it takes the system screenshot (`KEYCODE_SYSRQ`), finds and taps "Capturar mais" / "Capture more" in the system UI and checks the long-screenshot editor opens; one exported long screenshot per screen kept in the scratchpad (not committed).
- A: `GoldTest` and Roborazzi for the six D20 Chat golds, dark and light; emulator captures of `chatQ`, `chatE`, `chatR`, `chatRB`, `chatRK`, `chatRL` in `docs/qa/android/current/{dark,light}/` compared with the D20 golds under the QA rules of AGENTS, with a written diff list; a scene check that the action stack moves with the thread (bounds of `chat-record-plan` change after a swipe while the composer's stay); regress `chatG`, `chatU` (receipt actions already in the thread).
- A: the D20 `app gap` items, each with its capture or test.
- Whole plan: `testDevDebugUnitTest`, `verifyRoborazziDevDebug` and `assembleDevRelease` pass; `node tools/check-docs.mjs` passes; the dev build distributed with its version in Results.
- Manual acceptance (after delivery, [autonomous run](../../../sdd/autonomous-run.md) rules if run unattended): owner or tester on a device with the new dev build: copy a reply and paste it in another app; take a scrolling screenshot of Home and of a long Chat; scroll a plan's actions away and back.

## Results

Implemented 07/10/2026 on `feat/a61-chat-copy-scroll-inline`, parts C → A → B. Emulator: AVD `Medium_Phone` (API 36, AOSP SystemUI) at gold geometry (780 × 1688, density 320), devDebug against `tools/fake-chat-server.mjs`.

### Part C — scrolling screenshot

**Diagnosis.**

- **0.0.17-dev (before this branch):** on the emulator, the system screenshot (`KEYCODE_SYSRQ`) offered **Capture more** on Home, Chat and Config, and the long screenshot editor opened each time. SystemUI logged `LongScreenshot` heights of 3028 px (Home), 3422 px (Chat) and 2420 px (Config).
- **Framework search on this branch:** `ScrollCaptureTest` (Robolectric API 34, `View.dispatchScrollCaptureSearch` on the root view, the call SystemUI makes) finds exactly one target inside the window on Home, Chat (the reversed thread), Config and O1–O4.
- **Candidate causes, ruled out on AOSP:**
  - the page background and edge bubbles outside the scroll container: the target is the scroll node itself;
  - Haze glass and `Modifier.blur`: the captured image draws the glass;
  - the Chat `LazyColumn` with `reverseLayout`: the target is found and captured;
  - a parent clip or `graphicsLayer`: the search reaches the node;
  - the Compose BOM: `2026.09.00` since the first build, so there is nothing to bisect.
- **Bisect with the dev tags:** not run. No build fails on the emulator, so there is no failing side to bisect against.

**Root cause:** not reproduced. The failure the owner reported does not happen on AOSP SystemUI in any build tried. What differs on the owner's phone (the manufacturer's screenshot tool or the Android version) is unknown; finding it needs the phone model and Android version, or a screen recording of the attempt on the new build.

**Fix:** none in the app. Every screen already has one scroll-capture target that covers its scrollable content, so the golds are untouched.

**Visible defect found:** in the stitched Chat image, the page gradient repeats in bands. The background is fixed behind the thread, outside the scroll container; the content itself is complete. It is not fixed here: changing how the page background scrolls is a visual change D20 did not draw.

**Gate:**

- `ScrollCaptureTest` is part of `testDevDebugUnitTest`.
- `tools/capture-scroll.sh dark|light` takes the system screenshot, taps Capture more, checks the long screenshot editor opens, and saves.
- Limitation (the owner's decision, 07/10/2026: skip this check and record it as a limitation of the agent's interaction with the emulator):
  - In the last run the editor opened on Home, Chat and Config in Light, and on Home and Chat in Dark.
  - Config Dark missed the screenshot UI once.
  - The script could not read the saved image back.
  - The manual acceptance below is the real check.

### Part A — actions in the thread

- **The action stack:** a thread row with its own key (`actions-{id}`, `force-{id}`).
  - It sits 12 dp under its answer or under the latest question (after the budget lines, `Reservado para o {slot}`, or the follow-up question of `chatE`), with 10 dp between actions, and scrolls with the thread.
  - The footer keeps only the composer.
  - When an action shows and when it expires is unchanged (`ChatUiState.actions`, `forceEstimate`).
- **App gap from D20:** O1–O4 read `ONBOARDING n/5` and `Stepper/Progress` has five segments (the `AeroStepper` default).
- **`GoldTest`, D20 golds gated whole in both themes:**
  - `chatQ` 0.02 %, `chatE` 0.01 %, `chatR` 0.05 %, `chatRL` 0.05–0.06 %;
  - `chatRB` 0.09–0.11 %, after its fixture got the D17 markup the server sends;
  - `chatRK` is gated by two regions (0.23–0.30 %): the app fits "finalize" on one line where the Figma text wraps it (font raster), so the rest of the bubble shifts down one line;
  - `cfgS` 0.00 %, so the report-only exception is gone;
  - O1–O4 0.00–0.10 %.
- **`chatM` gold out of date:** it was not in D20 and still draws Registrar above the composer. It is gated on the header and the thread, and the action zone is reported. To be redrawn by the design plan [D22](../../../design/plans/d22-chatm-action-in-thread.md), by owner decision.
- **Roborazzi:** baselines refreshed (the old Chat baselines still showed "Chat Dieta Bot", passing only under the 1 % threshold). New baselines: `chatRB`, `chatRK`, `chatRL`, `chatCP`, `chatCC`.
- **`ChatInThreadUiTest`:**
  - the stack sits 12 dp under the answer, Reservar 10 dp under Registrar assim;
  - a drag moves the stack while the composer stays;
  - Forçar estimativa sits under the latest question;
  - an action tap ends the selection first.
- **Emulator (`SCENES=a61`, both themes):**
  - Registrar assim is drawn 12 dp under the plan and above the composer;
  - a drag moves it off screen while the composer stays at `166,1482–614,1578`;
  - Registrar assim still records from the thread.
- **Captures against the D20 golds:**
  - `chatE`: Light 0.31 %, Dark 0.12 %.
  - `chatQ`: header 0.00 %; thread 1.48 % Light, 0.52 % Dark.
  - `chatR`, `chatRB`, `chatRK`, `chatRL`: header 0.00–0.06 %, thread tail 0.07–0.80 %. The whole screen is report-only, because a long thread is cropped by the phone.
  - O1–O4, `o1e`, `o3s`, `o3t`: 0.00–1.26 %.
  - `cfgS`: 0.00 % in both themes.
  - `cfg`, `cfgR`, `wipe`: 0.00–0.21 %.
  - `homeP`: 0.63–0.83 %.
- **Diff list (against the gold):**
  - **Layout:** the action stack is under its message; the composer is alone in the footer.
  - **Tokens:** no new ones; only `surface/selected` and `border/selected`.
  - **ButtonGroup, CTA, sheet radius:** unchanged.
  - **Type size, timeline, semantic macros:** unchanged.
  - **Ignored:** the clock and sample times, the long-thread crop (`chatR`, `chatRB`, `chatRK`, `chatRL`, `chatCP`), the `chatRK` font-raster wrap, and the A30 sample text of `chatE` (re-captured from the D17 thread).

### Part B — copying messages

- **ViewModel:** the selection is a set of item keys held with the day and the latest wipe at its start.
  - Long press starts it; a tap toggles; removing the last one, ✕, back, a send, the day change, a wipe and leaving the Chat end it.
  - Only text bubbles count (user text, a photo with a caption, Tali prose, questions).
  - Copiar joins the messages in conversation order with a blank line between them: Tali text as `ReplyMarkup.plain(text, bullet = "- ")`, user text as typed.
  - Telemetry `message_copied` (`count`, `has_user`, `has_tali`).
- **UI:**
  - `AeroSelectionBar` (✕, the count, Copiar) replaces the header.
  - The selected row gets the `surface/selected` band across the screen, 6 dp over and under.
  - The selected bubble gets an opaque `bg/page` backing and a 2 dp `border/selected` ring.
  - The row uses plain gestures, not a clickable, so the bubble's own semantics stay unmerged; the long-press accessibility action is labelled `Selecionar mensagem`.
  - The clip is plain text labelled `Fibrai`.
  - On Android 12 and earlier, `AeroCopyToast` shows `Mensagem copiada` / `{n} mensagens copiadas` for 2 s as a polite live region.
- **Tests:**
  - `ChatCopyTest` (7): start, toggle and the last removal; a receipt is never selected; two messages copied in order with no markers and the telemetry params; one Tali reply plus the confirmation; ✕, send and the day change; a wipe; the selectable kinds.
  - `ChatInThreadUiTest`: the selection bar, Copiar and ✕; the receipt ignores the long press; the confirmation in the singular and the plural.
  - `GoldTest`: `chatCP` 0.04–0.06 %, `chatCC` 0.03 %.
- **Emulator (`SCENES=a61`, both themes):**
  - a long press on the user bubble shows `1`; a tap on the Tali reply makes it `2`; a tap on the receipt changes nothing; captured `chatCP`;
  - Copiar ends the selection;
  - the paste key in the composer gave exactly `2 pães franceses com 2 ovos mexidos no café da manhã\n\nIdentifiquei 2 pães franceses e 2 ovos mexidos. A estimativa total é de:`;
  - back and ✕ end the selection.
- **`chatCP` capture:** header 0.00 %, thread tail 0.04–0.26 %. The whole screen is report-only (long thread).
- **`chatCC`:** checked on the JVM render (`GoldTest`, 0.03 %) only. The emulator runs Android 16, where the system overlay confirms the copy instead. The scene now checks the app's own pill on API 32 or lower, and its absence on 13+.
- **Paste check is opt-in (`PASTE=1`):** the paste key comes from a keyboard source, and Gboard then stays in its physical-keyboard mode until the AVD restarts without saving its snapshot, which broke the keyboard checks of `capture-onboarding.sh`.

### Whole plan

- `testDevDebugUnitTest`, `verifyRoborazziDevDebug` and `assembleDevRelease` pass.
- `node tools/check-docs.mjs` passes.
- Specifications updated: [Chat](../../../produto/specifications/chat.md) (rules 2, 6, 11, 14 and 16, the states, the acceptance), [home-timeline](../../../produto/specifications/home-timeline.md), [memoria-push](../../../produto/specifications/memoria-push.md) and [perfil-onboarding](../../../produto/specifications/perfil-onboarding.md) (rule 4b, the acceptance).
- Dev build **0.0.19-dev** (versionCode 19, tag `dev-v0.0.19`) distributed to the `testers` group on 07/10/2026.
- Manual acceptance pending, on a device:
  - copy a reply and paste it in another app;
  - take a scrolling screenshot of Home and of a long Chat; if it fails, report the phone model and Android version;
  - scroll a plan's actions away and back.
