# Plan — A32 Chat: opens at the bottom, reverse paging, keyboard off for the photo

- Status: Aguardando aprovação
- Date: 01/10/2026
- Owning context: `android`
- Affected code: `apps/android/` (`feature/chat/*`, `core/database/*` chat messages, Room v7, tests and captures)
- Prerequisites: none. No new or changed gold: the paging indicator reuses the existing `LoadingIndicator` (owner decision of 01/10/2026). Independent of [ST8](../../stitch/plans/completed/st8-teto-sem-perfil.md) / [A31](a31-o1-perfil-obrigatorio-teclado.md).

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

- [chat](../../produto/specifications/chat.md) rules 3, 11 and 18; golds `chat0`, `chatE`, `chatQ`, `chatR`, `chatS` (no visual change expected).
- [Room spec](../specifications/room-v2.md), [ADR-010](../../decisions/010-room.md).
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

- [chat](../../produto/specifications/chat.md): rule 3 (photo closes the keyboard), rule 11 (thread opens at the bottom; pages of 20 within 60 days with the indicator; no jump when scrolled up).
- [Room spec](../specifications/room-v2.md): v7 index.

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
