# Plan — A32 Chat: opens at the bottom, reverse paging, keyboard off for the photo

- Status: Concluído
- Date: 01/10/2026
- Owning context: `android`
- Affected code: `apps/android/` (`feature/chat/*`, `core/database/*` chat messages, Room v7, tests and captures)
- Prerequisites: none. No new or changed gold: the paging indicator reuses the existing `LoadingIndicator` (owner decision of 01/10/2026). Independent of [ST8](../../../stitch/plans/completed/st8-teto-sem-perfil.md) / [A31](../a31-o1-perfil-obrigatorio-teclado.md).

## Authorization gate

This plan is documentation only. Implementation starts only after an explicit approval naming this file:

> Aprovo o plano `docs/android/plans/a32-chat-rolagem-paginacao.md`. Implemente o plano aprovado.

If implementation reveals an uncovered decision, stop, update the artifacts and ask for a new approval.

## Goal

The Chat behaves like a messaging app: it opens already at the last message, with no scroll animation from the top; it loads the last 20 messages and, as the user scrolls up, older ones 20 at a time with a centered loading indicator. Tapping the photo button closes the keyboard before the photo sheet opens.

## Owner decisions (01/10/2026)

1. Opening the Chat shows the bottom of the conversation at once.
2. Reverse lazy loading: last 20 messages first, then 20 older ones per page while scrolling up, with a centered loading indicator at the top.
3. The indicator reuses the existing `LoadingIndicator`; no Stitch gate.
4. Tapping the photo icon closes the keyboard so the photo `ModalBottomSheet` is fully visible.
5. History stays limited to the current 60 days (`MESSAGE_DAYS`); paging changes how it loads, not how far back it goes. (Agent default kept from the current behavior; the owner can change it at approval.)
6. New message with the user scrolled up: no jump and no "new messages" button (would need a gold).

## Root cause (current code)

- `ChatScreen.Thread` runs `animateScrollToItem(lastIndex)` whenever `items.size` changes. On first composition the list starts at index 0 and animates down. That happens with any history size.
- `DayRepository.observeMessages()` loads 60 days into one `Flow`; `ChatViewModel.render` rebuilds every item (date pills, plans, memory notices) over that whole list on each emission and on the minute tick.

## Sources of truth

- [chat](../../../produto/specifications/chat.md) rules 3, 11 and 18; golds `chat0`, `chatE`, `chatQ`, `chatR`, `chatS` (no visual change expected).
- [Room spec](../../specifications/room-v2.md), [ADR-010](../../../decisions/010-room.md).
- Skills `dieta-bot-android-ui`, `room-ksp-coroutines`, `compose-stability`, `android-architecture`, `screenshot-testing`, `dieta-bot-android-visual`.

## Implementation scope

### 1. Keyboard off for the photo (`ChatScreen`)

- Camera button in the composer and the "Tirar foto do prato" chip: `keyboard?.hide()` + `focusManager.clearFocus()` before `onPhoto`. Closing the sheet does not reopen the keyboard.

### 2. Thread opens at the bottom (`ChatScreen.Thread`)

- `LazyColumn(reverseLayout = true)` over the items in newest-first order (the VM exposes them reversed; no copy per frame in the composable). The first frame is already the bottom; no initial scroll.
- Gaps rewritten for the reversed order with the same values (16 dp, 5 dp question under its estimate, 12 dp routine card, 0 dp at the oldest item); `contentPadding` keeps 15 dp at the visual top and 2 dp at the bottom.
- The thread is composed only after the first page arrives (`ChatUiState.loaded`), so it never draws an empty list and then fills it.
- New item: auto-scroll to the newest (index 0, animated) only when the user is already at the bottom (first visible index ≤ 1) or the new item is the user's own send / its loading bubble. Otherwise the position stays.
- The greeting and routine card (rules 15 and 18) stay at the bottom as today.

### 3. Paging (`ChatMessageDao`, `DayRepository`, `ChatViewModel`)

- DAO: `observeLatest(fromDate, limit)`: `WHERE date >= :fromDate ORDER BY createdAtEpochMs DESC, id DESC LIMIT :limit`, a `Flow` (Room keeps it reactive to inserts).
- DAO: `getById(id)` and the existing `getByDate(date)` for logic that cannot depend on the loaded window.
- Room v7: index `(createdAtEpochMs, id)` on `chat_message`; migration 6 → 7 only creates the index (no data change).
- `ChatViewModel`: `pageLimit` starts at 20; `loadOlder()` adds 20 while the last emission returned `limit` rows (`hasOlder`), at most one request in flight (`loadingOlder`). The message flow is `pageLimit.flatMapLatest { observeLatest(from, it) }`.
- Trigger: the composable calls `loadOlder()` when the last visible index (the oldest drawn item) is within 5 items of the end of the list and `hasOlder`.
- Indicator: while `loadingOlder`, one extra item at the visual top with the existing `LoadingIndicator` (`DietaBotComponents`), centered horizontally (`testTag("chat-loading-older")`). Local Room usually answers in milliseconds, so it may barely show; no minimum display time.
- Scroll position is preserved when older items are prepended: stable `key`s, and in a reversed list the items added at the far end do not move the visible ones.

### 4. Logic that must not depend on the window

The VM uses `messages` today for more than drawing. After paging:

- Today's full conversation (`clarifyRounds`, Forçar estimativa, `send` history, routine card): a separate `Flow` of `getByDate(today)` equivalent (`observeByDate(today)`), not the paged window.
- `record` / Substituir (`estimateId`): `getById`, so an estimate outside the loaded window still records.
- `descriptionOf` / `chainStart` / `userBefore`: read the estimate's day with `getByDate(estimate.date)`.
- `PromptBuilder` keeps its own 7-day query; unchanged.
- Date pills, plan bubbles and memory notices are computed only over the loaded window.

### 5. Specs

- [chat](../../../produto/specifications/chat.md): rule 3 (photo closes the keyboard), rule 11 (thread opens at the bottom; pages of 20 within 60 days with the indicator; no jump when scrolled up).
- [Room spec](../../specifications/room-v2.md): v7 index.

## Affected files

- `feature/chat/ChatScreen.kt`, `ChatViewModel.kt`, `ChatUiState.kt`.
- `core/database/ChatMessageDao.kt`, `ChatMessageEntity.kt`, `DietaBotDatabase.kt`, `Migrations.kt`, `DayRepository.kt`, exported schema `7.json`.
- Tests: `MigrationV6V7Test` (new), `ChatViewModelTest`, `ChatFixtures`, Roborazzi chat baselines (only if the reversed list moves a pixel).
- `tools/capture-chat.sh` (long history scenario), `docs/produto/specifications/chat.md`, `docs/android/specifications/room-v2.md`, `docs/android/README.md` (Room v7).

## Planned validation

1. `testDevDebugUnitTest`:
   - DAO/repository: 45 messages → first emission has the newest 20 in order; `loadOlder` → 40; then 45 and `hasOlder` false; insert → still the newest at the top of the window.
   - `MigrationV6V7Test`: v6 data intact, index present.
   - VM: estimate outside the window still records (`getById`); `clarifyRounds` counts today's questions even with today's messages beyond 20; `chainStart` across the window edge.
2. `verifyRoborazziDevDebug` and `StitchGoldTest` chat goldens (`chat0`, `chatE`, `chatQ`, `chatR`, `chatS`) without regression.
3. Emulator (dev, fake server): seed 60+ messages over several days.
   - Open Chat: first frame at the last message, no visible scroll (screen recording or two captures 100 ms apart).
   - Scroll up: indicator at the top when slow, older messages appear without the visible ones jumping; stops at 60 days.
   - Scrolled up + new reply arrives: no jump; at the bottom + send: follows.
   - Keyboard open, tap camera: keyboard closes, sheet fully visible; close sheet: keyboard stays closed.
   - Captures `chat0`, `chatE`, `chatQ` in `docs/qa/android/current/{dark,light}/` + `node tools/diff-gold.mjs` + written diff list (expected: no visual change).

## Out of scope

- Paging 3 (new dependency) and history beyond 60 days.
- "New messages" button, unread counter or any new visual element.
- `PromptBuilder` history, server.

## Risks and controls

- **Logic reading a partial window:** section 4 moves every non-drawing use off the paged list; VM tests cover each.
- **`reverseLayout` and IME insets:** the composer and thread keep the current inset handling; check on the emulator with the keyboard open.
- **Flow re-emitting the whole page on each insert:** at most `pageLimit` rows; `render` stays proportional to the window, not to 60 days.

## Acceptance criteria

- The Chat opens at the bottom with no scroll animation.
- 20 messages at first; older ones in pages of 20 with the indicator, without position jumps, up to 60 days.
- Recording, Substituir, question rounds and Forçar estimativa behave as before.
- The camera button and photo chip close the keyboard before the sheet.

## Closure

After implementation, record real results and apply the lifecycle in `docs/sdd/README.md`, with the git delivery (§ 6).

Only an explicit owner statement cancelling this plan allows `Cancelado` and the `plans/cancelled/` folder.

## Results (01/10/2026)

Approved by the owner on 01/10/2026 with the sentence of the authorization gate. No Stitch gate; no gold changed.

Implemented:

- Room v7: index `(createdAtEpochMs, id)` on `chat_message`, `MIGRATION_6_7` only creates it; schema `7.json` exported. DAO `observeLatest(fromDate, limit)` (newest first, `LIMIT`) and `getById`; repository `observeLatestMessages(limit)`, `messagesOf(date)`, `message(id)`. `observeMessages()` stays for the existing tests and readers.
- `ChatViewModel`: `pageLimit` starts at 20; `loadOlder()` adds 20 only when the last window came back full and no page is in flight. One Room observation per window: every emission also reads today's whole conversation, cut at the window's newest id. Two separate Room flows (window and today) emitted out of step during the first test run, so the actions could arrive a frame before their bubble; reading both in one emission removed it. Today's rows feed actions, question rounds, Forçar estimativa, the empty-day greeting and plan projection. `post` reads today with `messagesOf`. Gravar and Substituir read the estimate by id and its day with `messagesOf(estimate.date)` (`descriptionOf`, `chainStart`, `userBefore`, photo source). `ChatUiState` gains `loaded`, `hasOlder`, `loadingOlder` and `newestFirst` (an `asReversed` view built once per state).
- `ChatScreen`: the thread is composed after the first page (`loaded`). `LazyColumn(reverseLayout = true)` over `newestFirst`, with `verticalArrangement = Arrangement.Top`, so a short thread still starts at the top like the golds. Same gaps, with 0 dp on the oldest drawn item and the same content padding. Auto-scroll to index 0 only when the newest key changes and either the first visible index is ≤ 1 or the new item is the user's own send or its loading bubble. `loadOlder` fires when the last visible index is within 5 of the end; while `loadingOlder`, the existing `WaitIndicator` (`LoadingIndicator`) is centered at the visual top (`chat-loading-older`). The camera button and the `Tirar foto do prato` chip call `keyboard.hide()` and `clearFocus()` before their action. `MainActivity` wires `onLoadOlder`.
- QA tools: `capture-chat.sh` gets `SCENES=a32`, also part of the full run. It seeds 66 rows over 6 days plus 3 rows 61 days back and checks the open with a 3 s `screenrecord`. From tap + 0.9 s (after the navigation fade) every frame must equal the last one in the thread area. Calibrated on a `master` build: diff 11.9 (the old scroll from the top), versus 0.0 on A32. Then: scroll up to the 60-day edge; a delayed reply while scrolled up; a send at the bottom; camera with the keyboard open. `fake-chat-server.mjs` gets `{"delay": ms}`. The A30 `Pode estimar assim.` check now closes the keyboard first (see below).

Validation:

1. `:app:testDevDebugUnitTest`: **384** tests, 0 failures. New: `MigrationV6V7Test` (v6 rows intact, index present); `RoomV2Test` window of 45 rows (20 → 40 → 45, newest first, an insert lands at the top, the 60-day edge); `ChatViewModelTest` paging (20, then 40, then 45 and `hasOlder` false, one page in flight, a send lands at the newest end), an estimate out of the window keeps its actions and records its chain start (the dinner, 820 kcal), and question rounds with 25 earlier rows today; `ChatThreadTest` (Compose) opens at the newest message, asks for an older page at the top, indicator while loading, and camera button and photo chip call `hide` before their action. ✅
2. `:app:verifyRoborazziDevDebug` green with no baseline change: the reversed list moved no pixel. JVM `StitchGoldTest` is part of the same run. ✅
3. `:app:assembleDevRelease` ✅
4. Emulator (devDebug + fake server, 780×1688 @ 320 dpi), full `capture-chat.sh`, dark and light. Every A32 check ✓: open settled with no scroll (29 frames, diff 0.0); newest message on screen; scroll up reaches `msg00` and never draws the rows older than 60 days; a reply while scrolled up lands off screen with the same row still visible; reopen at the bottom with the reply; a send at the bottom follows to the new reply; keyboard open → camera → keyboard closed and photo sheet open → close the sheet → keyboard stays closed. The A30, A29 and A25 flows also pass. The loading indicator was not caught by `uiautomator`, because local Room answers before the dump. It is covered by `ChatThreadTest`. ⚠️ Not caused by A32: two old A8 checks of the full run fail on any build since A28 (memory.bin `NM` header after Gravar, and `POST memory` non-empty). A memory-less estimate no longer writes a memory line. They are left for a separate fix.
5. Gold comparison (`node tools/diff-gold.mjs`): chatE 1.56% / 1.20% ✓, chatQ dark 1.21% ✓ (Forçar region 0.19%), chatQ light 5.99% (known A30 conflict, region 0.12% ✓), chat0 4.99% / 4.68% (known gold conflict, report only). Against the previous captures in `docs/qa/android/current/`, chat0 differs only in the date pill and the greeting time.

Diff list (`chat0`, `chatE`, `chatQ`, both themes): no visual change. Layout, tokens, type sizes, radius, CTA, actions bar, Forçar bar and semantic macro colors match the previous captures. The short thread still starts below the header (`Arrangement.Top` with `reverseLayout`).

Behavior note recorded for the owner: when a tall reply arrives with the user at the bottom, the thread now follows to the newest item, as in the plan. With the keyboard open, the estimate card can fill the view and the user's own message sits just above it. Before A32, a reply that replaced the loading bubble did not scroll, so the user's message stayed visible and the card ran below it.

Emulator note: on this AVD, Gboard sometimes takes `adb` taps as stylus input and shows its floating toolbar instead of the keyboard, which covers the camera button. The camera check passed in the clean runs; the Compose test covers the order hide → photo deterministically.
