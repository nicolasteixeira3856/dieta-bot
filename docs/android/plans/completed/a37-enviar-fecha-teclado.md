# Plan — A37 Send closes the keyboard

- Status: Concluído (02/10/2026)
- Approval: 02/10/2026 (owner, in chat: "eu queria que você gerasse um microplano rápido para o app, já aplicasse ele, testasse e já fizesse o deploy após o merge + atualização da master. Quando o usuário clica no botão de enviar para o chat o teclado deveria fechar sozinho e não ficar aberto.")
- Date: 02/10/2026
- Owning context: `android`
- Affected code: `apps/android/app/src/main/java/com/nutri/android/feature/chat/ChatScreen.kt`, `apps/android/app/src/test/java/com/nutri/android/feature/chat/ChatThreadTest.kt`
- Prerequisites: none. No Stitch gate: no layout or gold change (the golds show no keyboard).

## Goal

A tap on the Chat send arrow hides the keyboard and clears the composer focus, then sends. Today the keyboard stays open and covers the answer and the receipt.

## Implementation scope

1. `ChatScreen`: wrap `onSend` the way A32 wraps the camera and the photo chip: `keyboard?.hide()`, `focusManager.clearFocus()`, then `onSend()`. Pass the wrapped lambda to `Footer`.
2. Nothing else changes. Enter still inserts a line break (A18). A disabled send (`canSend` false) does nothing, keyboard included. Editar and Quase igual still focus the composer and open the keyboard.

## Planned validation

1. `ChatThreadTest`: tapping `chat-send` with text calls `hide` before the send.
2. `./gradlew testDevDebugUnitTest verifyRoborazziDevDebug assembleDevRelease` green.
3. Owner: tested on the dev build distributed after the merge (A16).

## Out of scope

Hiding the keyboard on scroll or on a tap in the thread. Any layout change.

## Closure

After implementation, record real results and apply the lifecycle in `docs/sdd/README.md`. The plan waits in `pending_manual_validation/` until the owner confirms on the phone.

## Results (02/10/2026)

- `ChatScreen` wraps `onSend` with `keyboard?.hide()` + `focusManager.clearFocus()`, as the A32 camera and photo chip.
- `ChatThreadTest.sendHidesTheKeyboardBeforeSending`: `hide` then `send`. ChatThreadTest 7/7.
- `./gradlew testDevDebugUnitTest verifyRoborazziDevDebug assembleDevRelease`: green. No baseline changed (no layout change).
- ✅ Owner, 02/10/2026: confirmed on dev 0.0.9 (Firebase App Tester). Owner: "Confirmei, pode fechar o plano." Plan → `Concluído`.
